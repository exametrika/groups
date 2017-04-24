/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.config.resource.ClassPathResourceLoader;
import com.exametrika.common.config.resource.FileResourceLoader;
import com.exametrika.common.config.resource.IResourceLoader;
import com.exametrika.common.config.resource.ResourceManager;
import com.exametrika.common.io.ISerializationRegistrar;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.compression.CompressionProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.FullNodeTrackingStrategy;
import com.exametrika.common.messaging.impl.protocols.failuredetection.HeartbeatProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeTrackingStrategy;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.protocols.optimize.BundlingProtocol;
import com.exametrika.common.messaging.impl.protocols.optimize.LocalSendOptimizationProtocol;
import com.exametrika.common.messaging.impl.protocols.streaming.StreamingProtocol;
import com.exametrika.common.messaging.impl.protocols.trace.InterceptorProtocol;
import com.exametrika.common.messaging.impl.protocols.trace.TracingProtocol;
import com.exametrika.common.messaging.impl.transports.ConnectionManager;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.common.net.nio.TcpNioDispatcher;
import com.exametrika.common.net.nio.socket.ITcpSocketChannelFactory;
import com.exametrika.common.net.nio.socket.TcpSocketChannelFactory;
import com.exametrika.common.net.nio.ssl.TcpSslSocketChannelFactory;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link AbstractChannelFactory} represents a factory of message-oriented channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractChannelFactory
{
    private static final IMessages messages = Messages.get(IMessages.class);
    protected final ILogger logger = Loggers.get(getClass());
    protected final ChannelFactoryParameters factoryParameters;
    
    public AbstractChannelFactory()
    {
        this(new ChannelFactoryParameters());
    }
    
    public AbstractChannelFactory(ChannelFactoryParameters factoryParameters)
    {
        Assert.notNull(factoryParameters);
        
        this.factoryParameters = factoryParameters;
    }
    
    protected IChannel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager, 
        TcpNioDispatcher dispatcher, ICompartment compartment, ChannelParameters parameters)
    {
        Assert.notNull(parameters);
        Assert.notNull(parameters.receiver);
        
        ISerializationRegistry serializationRegistry = Serializers.createRegistry();
        for (ISerializationRegistrar registrar : parameters.serializationRegistrars)
            serializationRegistry.register(registrar);
        
        if (parameters.secured && logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, Loggers.getMarker(channelName), messages.securedChannel(parameters.keyStorePath));

        List<IFailureObserver> failureObservers = liveNodeManager.getFailureObservers();
        failureObservers.add(channelObserver);
        MessageFactory messageFactory = new MessageFactory(serializationRegistry, liveNodeManager);
        
        List<AbstractProtocol> protocols = new ArrayList<AbstractProtocol>();
        
        TracingProtocol highTracingProtocol = new TracingProtocol(channelName, TracingProtocol.class.getName() + ".High", messageFactory);
        protocols.add(highTracingProtocol);
        
        StreamingProtocol streamingProtocol = new StreamingProtocol(channelName, messageFactory, 
            factoryParameters.streamingMaxFragmentSize, true, true);
        protocols.add(streamingProtocol);

        LocalSendOptimizationProtocol highLocal = new LocalSendOptimizationProtocol(channelName, 
            LocalSendOptimizationProtocol.class.getName() + ".High", messageFactory, liveNodeManager);
        protocols.add(highLocal);
        
        BundlingProtocol bundlingProtocol = new BundlingProtocol(channelName, messageFactory, serializationRegistry, 
            factoryParameters.maxBundlingMessageSize, factoryParameters.maxBundlingPeriod, factoryParameters.maxBundleSize, 
            factoryParameters.sendQueueIdlePeriod, factoryParameters.receiveMessageList);
        protocols.add(bundlingProtocol);
        
        CompressionProtocol compressionProtocol = new CompressionProtocol(channelName, factoryParameters.compressionLevel, messageFactory, 
            serializationRegistry);
        protocols.add(compressionProtocol);
        
        createProtocols(parameters, channelName, messageFactory, serializationRegistry, liveNodeManager, failureObservers, protocols);
        
        LocalSendOptimizationProtocol lowLocal = new LocalSendOptimizationProtocol(channelName, 
            LocalSendOptimizationProtocol.class.getName() + ".Low", messageFactory, liveNodeManager);
        protocols.add(lowLocal);
        
        HeartbeatProtocol heartbeatProtocol = new HeartbeatProtocol(channelName, createNodeTrackingStrategy(), messageFactory, 
            factoryParameters.heartbeatTrackPeriod, factoryParameters.heartbeatStartPeriod, factoryParameters.heartbeatPeriod, 
            factoryParameters.heartbeatFailureDetectionPeriod);
        protocols.add(heartbeatProtocol);
        
        TracingProtocol lowTracingProtocol = new TracingProtocol(channelName, TracingProtocol.class.getName() + ".Low", messageFactory);
        protocols.add(lowTracingProtocol);
        
        InterceptorProtocol interceptorProtocol = new InterceptorProtocol(channelName, messageFactory);
        protocols.add(interceptorProtocol);
        channelObserver.addChannelListener(interceptorProtocol);
        
        ProtocolStack protocolStack = new ProtocolStack(channelName, protocols, liveNodeManager, factoryParameters.cleanupPeriod,
            factoryParameters.nodeCleanupPeriod);
        protocolStack.register(serializationRegistry);
        protocolStack.getFirst().setReceiver(parameters.receiver);
        
        AbstractProtocol lastProtocol = protocolStack.getLast();
        TcpTransport.Parameters transportParameters = new TcpTransport.Parameters();
        transportParameters.channelName = channelName;
        transportParameters.receiver = lastProtocol;
        transportParameters.serializationRegistry = serializationRegistry;
        transportParameters.messageFactory = messageFactory;
        transportParameters.selectionPeriod = factoryParameters.selectionPeriod;
        transportParameters.channelTimeout = factoryParameters.transportChannelTimeout;
        transportParameters.maxChannelIdlePeriod = factoryParameters.transportMaxChannelIdlePeriod;
        transportParameters.connectionFilter = parameters.connectionFilter;
        transportParameters.rateController = parameters.rateController;
        transportParameters.adminFilter = parameters.adminFilter;
        transportParameters.multiThreaded = parameters.multiThreaded;
        transportParameters.discardPolicy = parameters.discardPolicy;
        
        if (!parameters.secured)
            transportParameters.socketChannelFactory = new TcpSocketChannelFactory();
        else
            transportParameters.socketChannelFactory = createSecuredSocketChannelFactory(parameters.keyStorePath, 
                parameters.keyStorePassword);
        
        transportParameters.clientPart = parameters.clientPart;
        transportParameters.serverPart = parameters.serverPart;
        transportParameters.bindAddress = parameters.bindAddress;
        transportParameters.portRangeStart = parameters.portRangeStart;
        transportParameters.portRangeEnd = parameters.portRangeEnd;
        transportParameters.receiveThreadCount = factoryParameters.transportReceiveThreadCount;
        transportParameters.maxUnlockSendQueueCapacity = factoryParameters.transportMaxUnlockSendQueueCapacity;
        transportParameters.minLockSendQueueCapacity = factoryParameters.transportMinLockSendQueueCapacity;
        transportParameters.maxPacketSize = factoryParameters.transportMaxPacketSize;
        transportParameters.failureObserver = liveNodeManager;
        transportParameters.connectionObserver = liveNodeManager;
        transportParameters.localNodeInitializer = liveNodeManager;
        transportParameters.minReconnectPeriod = factoryParameters.transportMinReconnectPeriod;
        
        transportParameters.dispatcher = dispatcher;
        
        TcpTransport transport = new TcpTransport(transportParameters);
        transport.register(serializationRegistry);
        lastProtocol.setSender(transport);
        lastProtocol.setPullableSender(transport);
        heartbeatProtocol.setFailureObserver(transport);
        heartbeatProtocol.setAccessTimeProvider(transport);
        
        Map<String, IConnectionProvider> connectionProviders = new HashMap<String, IConnectionProvider>();
        connectionProviders.put("tcp", transport);
        ConnectionManager connectionManager = new ConnectionManager(transportParameters.transportId, connectionProviders);

        compartment.addTimerProcessor(transport);
        compartment.addTimerProcessor(protocolStack);
        compartment.addProcessor(lowLocal);
        compartment.addProcessor(highLocal);
        
        transport.setFlowController(compartment);
        highLocal.setCompartment(compartment);
        lowLocal.setCompartment(compartment);
        
        IChannel channel = createChannel(channelName, channelObserver, liveNodeManager, messageFactory, protocolStack, transport,
            connectionManager, compartment);
        
        if (parameters.multiThreaded)
            heartbeatProtocol.setChannel(channel);
        protocolStack.setTimeService(transport.getTimeService());
        protocolStack.setConnectionProvider(connectionManager);
        wireProtocols(channel, transport, protocolStack);
        
        return channel;
    }

    protected IChannel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
        MessageFactory messageFactory, ProtocolStack protocolStack, TcpTransport transport,
        ConnectionManager connectionManager, ICompartment compartment)
    {
        return new Channel(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, 
            connectionManager, compartment);
    }

    protected INodeTrackingStrategy createNodeTrackingStrategy()
    {
        return new FullNodeTrackingStrategy();
    }
    
    protected void createProtocols(ChannelParameters parameters, String channelName, IMessageFactory messageFactory, ISerializationRegistry serializationRegistry, 
        ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers, List<AbstractProtocol> protocols)
    {
    }

    protected void wireProtocols(IChannel channel, TcpTransport transport, ProtocolStack protocolStack)
    {
    }
    
    private ITcpSocketChannelFactory createSecuredSocketChannelFactory(String keyStorePath, String keyStorePassword)
    {
        Map<String, IResourceLoader> resourceLoaders = new HashMap<String, IResourceLoader>();
        resourceLoaders.put(FileResourceLoader.SCHEMA, new FileResourceLoader());
        resourceLoaders.put(ClassPathResourceLoader.SCHEMA, new ClassPathResourceLoader());
        ResourceManager resourceManager = new ResourceManager(resourceLoaders, "file");
        InputStream stream = null;
        try
        {
            stream = resourceManager.getResource(keyStorePath);
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(stream, keyStorePassword.toCharArray());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
            
            return new TcpSslSocketChannelFactory(sslContext, true, true);
        }
        catch (Exception e)
        {
            throw new ChannelException(e);
        }
        finally
        {
            IOs.close(stream);
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Secured channel uses keystore: {0}.")
        ILocalizedMessage securedChannel(String keyStorePath);
    }
}
