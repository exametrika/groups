/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
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
import com.exametrika.common.compartment.ICompartmentFactory;
import com.exametrika.common.compartment.impl.CompartmentFactory;
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
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
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
import com.exametrika.common.net.ITcpConnectionFilter;
import com.exametrika.common.net.ITcpRateControllerFactory;
import com.exametrika.common.net.nio.TcpNioDispatcher;
import com.exametrika.common.net.nio.socket.ITcpSocketChannelFactory;
import com.exametrika.common.net.nio.socket.TcpSocketChannelFactory;
import com.exametrika.common.net.nio.ssl.TcpSslSocketChannelFactory;
import com.exametrika.common.net.utils.ITcpPacketDiscardPolicy;
import com.exametrika.common.net.utils.TcpNameFilter;
import com.exametrika.common.tasks.impl.NoFlowController;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link ChannelFactory} represents a factory of message-oriented channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class ChannelFactory
{
    private static final IMessages messages = Messages.get(IMessages.class);
    protected final ILogger logger = Loggers.get(ChannelFactory.class);
    protected final FactoryParameters factoryParameters;
    
    public static class FactoryParameters
    {
        public long selectionPeriod = 100;
        public long cleanupPeriod = 1000;
        public long nodeCleanupPeriod = 30000;
        public int compressionLevel = 5;
        public int streamingMaxFragmentSize = 10000; 
        public long heartbeatTrackPeriod = 500;
        public long heartbeatStartPeriod = 10000;
        public long heartbeatPeriod = 1000;
        public long heartbeatFailureDetectionPeriod = 60000;
        public long transportChannelTimeout = 10000;
        public long transportMaxChannelIdlePeriod = 600000;
        public Integer transportReceiveThreadCount = null;
        public int transportMaxUnlockSendQueueCapacity = 700000;
        public int transportMinLockSendQueueCapacity = 1000000;
        public int transportMaxPacketSize = 1000000;
        public long transportMinReconnectPeriod = 60000;
        public int compartmentMaxUnlockQueueCapacity = 7000000;
        public int compartmentMinLockQueueCapacity = 10000000;
        public int maxBundlingMessageSize = 10000;
        public int maxBundlingPeriod = 100;
        public int maxBundleSize = 1000000;
        public boolean receiveMessageList = false;
        public int sendQueueIdlePeriod = 600000;
        
        public FactoryParameters()
        {
            this(false);
        }
        
        public FactoryParameters(boolean debug)
        {
            int timeMultiplier = !debug ? 1 : 1000; 
            heartbeatFailureDetectionPeriod *= timeMultiplier;
            heartbeatStartPeriod *= timeMultiplier;
            transportChannelTimeout *= timeMultiplier;
            transportMaxChannelIdlePeriod *= timeMultiplier;
        }
    }
    
    public static class Parameters
    {
        public String channelName;
        public boolean clientPart;
        public boolean serverPart;
        public IReceiver receiver;
        public InetAddress bindAddress;
        public Integer portRangeStart;
        public Integer portRangeEnd;
        public boolean secured;
        public String keyStorePath;
        public String keyStorePassword;
        public ITcpConnectionFilter connectionFilter;
        public ITcpRateControllerFactory rateController;
        public TcpNameFilter adminFilter;
        public List<ISerializationRegistrar> serializationRegistrars = new ArrayList<ISerializationRegistrar>();
        public boolean multiThreaded = false;
        public ITcpPacketDiscardPolicy<IMessage> discardPolicy;
    }
    
    public ChannelFactory()
    {
        this(new FactoryParameters());
    }
    
    public ChannelFactory(FactoryParameters factoryParameters)
    {
        Assert.notNull(factoryParameters);
        
        this.factoryParameters = factoryParameters;
    }
    
    public IChannel createChannel(Parameters parameters)
    {
        Assert.notNull(parameters);
        Assert.notNull(parameters.receiver);
        
        ISerializationRegistry serializationRegistry = Serializers.createRegistry();
        for (ISerializationRegistrar registrar : parameters.serializationRegistrars)
            serializationRegistry.register(registrar);
        
        String channelName;
        if (parameters.channelName != null)
            channelName = parameters.channelName;
        else
            channelName = ManagementFactory.getRuntimeMXBean().getName();
        
        if (parameters.secured && logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, Loggers.getMarker(channelName), messages.securedChannel(parameters.keyStorePath));

        ChannelObserver channelObserver = new ChannelObserver(channelName);
        List<IFailureObserver> failureObservers = new ArrayList<IFailureObserver>();
        failureObservers.add(channelObserver);
        LiveNodeManager liveNodeManager = new LiveNodeManager(channelName, failureObservers, channelObserver);
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
        
        transportParameters.dispatcher = new TcpNioDispatcher(transportParameters.channelTimeout, 
            transportParameters.maxChannelIdlePeriod, transportParameters.channelName);
        
        TcpTransport transport = new TcpTransport(transportParameters);
        transport.register(serializationRegistry);
        lastProtocol.setSender(transport);
        lastProtocol.setPullableSender(transport);
        heartbeatProtocol.setFailureObserver(transport);
        heartbeatProtocol.setAccessTimeProvider(transport);
        
        Map<String, IConnectionProvider> connectionProviders = new HashMap<String, IConnectionProvider>();
        connectionProviders.put("tcp", transport);
        ConnectionManager connectionManager = new ConnectionManager(transportParameters.transportId, connectionProviders);

        ICompartmentFactory.Parameters compartmentParameters = new ICompartmentFactory.Parameters();
        compartmentParameters.name = transportParameters.channelName;
        compartmentParameters.dispatchPeriod = transportParameters.selectionPeriod;
        compartmentParameters.dispatcher = transportParameters.dispatcher;
        compartmentParameters.timerProcessors.add(transport);
        compartmentParameters.timerProcessors.add(protocolStack);
        compartmentParameters.processors.add(lowLocal);
        compartmentParameters.processors.add(highLocal);
        compartmentParameters.flowController = new NoFlowController();
        compartmentParameters.minLockQueueCapacity = factoryParameters.compartmentMinLockQueueCapacity;
        compartmentParameters.minLockQueueCapacity = factoryParameters.compartmentMinLockQueueCapacity;
        
        ICompartment compartment = new CompartmentFactory().createCompartment(compartmentParameters);

        transport.setFlowController(compartment);
        highLocal.setCompartment(compartment);
        lowLocal.setCompartment(compartment);
        
        Channel channel = createChannel(channelName, channelObserver, liveNodeManager, messageFactory, protocolStack, transport,
            connectionManager, compartment);
        
        if (parameters.multiThreaded)
            heartbeatProtocol.setChannel(channel);
        protocolStack.setTimeService(transport.getTimeService());
        protocolStack.setConnectionProvider(connectionManager);
        wireProtocols(channel, transport, protocolStack);
        
        return channel;
    }

    protected Channel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
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
    
    protected void createProtocols(Parameters parameters, String channelName, IMessageFactory messageFactory, ISerializationRegistry serializationRegistry, 
        ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers, List<AbstractProtocol> protocols)
    {
    }

    protected void wireProtocols(Channel channel, TcpTransport transport, ProtocolStack protocolStack)
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
