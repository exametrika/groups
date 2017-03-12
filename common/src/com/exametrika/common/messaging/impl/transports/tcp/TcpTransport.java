/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.io.ISerializationRegistrar;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IConnectionObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ILocalNodeAware;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeAccessTimeProvider;
import com.exametrika.common.messaging.impl.transports.ITransport;
import com.exametrika.common.messaging.impl.transports.InetSocketAddressSerializer;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;
import com.exametrika.common.messaging.impl.transports.UnicastAddressSerializer;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpChannelAcceptor;
import com.exametrika.common.net.ITcpChannelListener;
import com.exametrika.common.net.ITcpConnectionFilter;
import com.exametrika.common.net.ITcpPacketChannel;
import com.exametrika.common.net.ITcpRateControllerFactory;
import com.exametrika.common.net.ITcpServer;
import com.exametrika.common.net.nio.TcpNioDispatcher;
import com.exametrika.common.net.nio.socket.ITcpSocketChannelFactory;
import com.exametrika.common.net.utils.ITcpPacketDiscardPolicy;
import com.exametrika.common.net.utils.TcpNameFilter;
import com.exametrika.common.net.utils.TcpNoPacketDiscardPolicy;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tasks.impl.NoFlowController;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;

/**
 * The {@link TcpTransport} represents a tcp transport.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpTransport implements ITransport, ITcpChannelAcceptor, ITcpChannelListener, IFailureObserver, 
    ISerializationRegistrar, IConnectionProvider, ICompartmentTimerProcessor, INodeAccessTimeProvider
{
    public static final byte FLAG_PARALLEL = 0x1;
    public static final byte FLAG_LOW_PRIORITY = 0x2;
    public static final byte FLAG_HIGH_PRIORITY = 0x4;
    public static final byte FLAG_DUPLICATE = 0x8;
    public static final byte FLAG_DISCONNECT_REQUEST = 0x10;
    public static final byte FLAG_DISCONNECT_RESPONSE = 0x20;
    public static final byte HEADER_OVERHEAD = 1;//flags(byte)
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ILogger logger = Loggers.get(TcpTransport.class);
    private final IMarker marker;
    private final Parameters parameters;
    private final int transportId;
    private final TcpNioDispatcher dispatcher;
    private final ITcpIncomingMessageHandler incomingMessageHandler;
    private final ITcpSocketChannelFactory socketChannelFactory;
    private final ITcpThreadingModel threadingModel;
    private final long timerPeriod;
    private final ITcpPacketDiscardPolicy<IMessage> discardPolicy;
    private IFlowController<IAddress> flowController;
    private ITcpServer server;
    private volatile boolean started;
    private volatile boolean stopped;
    private boolean requestToStop;
    private UnicastAddress localNode;
    private long lastUpdateTime;
    private final Map<InetSocketAddress, TcpConnection> connections = new LinkedHashMap<InetSocketAddress, TcpConnection>();
    private final List<TcpConnection> closedConnections = new LinkedList<TcpConnection>();

    public static class Parameters
    {
        public int transportId;
        public String channelName;
        public TcpNioDispatcher dispatcher;
        public IReceiver receiver;
        public ISerializationRegistry serializationRegistry;
        public IMessageFactory messageFactory;
        public long timerPeriod = 1000;
        public long selectionPeriod = 100;
        public long channelTimeout;
        public long maxChannelIdlePeriod;
        public ITcpSocketChannelFactory socketChannelFactory;
        public ITcpConnectionFilter connectionFilter;
        public ITcpRateControllerFactory rateController;
        public TcpNameFilter adminFilter;
        public boolean clientPart;
        public boolean serverPart;
        public InetAddress bindAddress;
        public Integer portRangeStart;
        public Integer portRangeEnd;
        public Integer receiveThreadCount;
        public int maxUnlockSendQueueCapacity;
        public int minLockSendQueueCapacity;
        public int maxPacketSize;
        public IFailureObserver failureObserver;
        public IConnectionObserver connectionObserver;
        public ILocalNodeAware localNodeInitializer; 
        public long minReconnectPeriod;
        public boolean multiThreaded = true;
        public IFlowController<IAddress> flowController;
        public ITcpPacketDiscardPolicy<IMessage> discardPolicy;
    }
    
    public TcpTransport(Parameters parameters)
    {
        Assert.notNull(parameters);
        Assert.notNull(parameters.channelName);
        Assert.notNull(parameters.dispatcher);
        Assert.notNull(parameters.receiver);
        Assert.notNull(parameters.serializationRegistry);
        Assert.notNull(parameters.messageFactory);
        Assert.notNull(parameters.socketChannelFactory);
        Assert.notNull(parameters.failureObserver);
        Assert.notNull(parameters.connectionObserver);
        Assert.notNull(parameters.localNodeInitializer);
        Assert.isTrue(parameters.clientPart || parameters.serverPart);
        
        this.parameters = parameters;
        this.transportId = parameters.transportId;
        this.socketChannelFactory = parameters.socketChannelFactory;
        marker = Loggers.getMarker(parameters.channelName);
        timerPeriod = parameters.timerPeriod;
        dispatcher = parameters.dispatcher;

        if (parameters.flowController == null)
            flowController = new NoFlowController<IAddress>();
        else
            flowController = parameters.flowController;
        
        if (parameters.discardPolicy == null)
            discardPolicy = new TcpNoPacketDiscardPolicy<IMessage>();
        else
            discardPolicy = parameters.discardPolicy;
        
        discardPolicy.setTimeService(dispatcher);

        if (parameters.multiThreaded)
        {
            int receiveThreadCount = parameters.receiveThreadCount != null ? parameters.receiveThreadCount : 
                Runtime.getRuntime().availableProcessors() * 2;
            
            threadingModel = new TcpMultiThreadModel(receiveThreadCount);
        }
        else
            threadingModel = new TcpSingleThreadModel();
        
        incomingMessageHandler = threadingModel.createIncomingMessageHandler(parameters.channelName, 
            parameters.receiver, parameters.serializationRegistry);
    }

    public int getTransportId()
    {
        return transportId;
    }
    
    public ITimeService getTimeService()
    {
        return dispatcher;
    }
    
    public IMarker getMarker()
    {
        return marker;
    }
    
    public void setFlowController(IFlowController<IAddress> flowController)
    {
        Assert.notNull(flowController);
        Assert.checkState(!started);
        
        this.flowController = flowController;
    }
    
    public synchronized boolean addServerConnection(TcpConnection serverConnection)
    {
        if (parameters.clientPart)
        {
            TcpConnection clientConnection = connections.get(serverConnection.getRemoteInetAddress());
            
            if (clientConnection != null)
            {
                Assert.checkState(!clientConnection.isClosed());
                
                if (clientConnection.isConnected())
                {
                    serverConnection.setDuplicateSend(null);
                    return false;
                }
                
                if (TcpUtils.compare(serverConnection.getLocalInetAddress(), clientConnection.getRemoteInetAddress()) < 0)
                {
                    serverConnection.setDuplicateSend(null);
                    return false;
                }
                
                clientConnection.setDuplicateSend(serverConnection);
            }
        }
        
        connections.put(serverConnection.getRemoteInetAddress(), serverConnection);
        
        return true;
    }

    @Override
    public IAddress getLocalNode()
    {
        Assert.checkState(started && !stopped);
        
        return localNode;
    }

    @Override
    public void send(IMessage message)
    {
        if (!started || stopped)
            return;
        
        Assert.isInstanceOf(UnicastAddress.class, message.getDestination());
        
        UnicastAddress address = (UnicastAddress)message.getDestination();
        
        TcpConnection connection;
        synchronized (this)
        {
            connection = connections.get(address.getAddress(transportId));
            if (connection == null && parameters.clientPart)
                connection = createClientConnection(address.<InetSocketAddress>getAddress(transportId), address);
        }
        
        if (connection != null && !connection.isClosed())
            connection.enqueue(message);
        else if (logger.isLogEnabled(LogLevel.WARNING))
            logger.log(LogLevel.WARNING, marker, messages.messageLost(Strings.wrap(message.toString(), 4, 120)));
    }

    @Override
    public ISink register(IAddress destination, IFeed feed)
    {
        Assert.checkState(started && !stopped);
        Assert.isInstanceOf(UnicastAddress.class, destination);
        Assert.notNull(feed);
        
        UnicastAddress address = (UnicastAddress)destination;
        
        TcpConnection connection;
        synchronized (this)
        {
            connection = connections.get(address.getAddress(transportId));
            if (connection == null && parameters.clientPart)
                connection = createClientConnection(address.<InetSocketAddress>getAddress(transportId), address);
        }
        
        if (connection != null && !connection.isClosed())
            return connection.register(address, feed);
        else
            return null;
    }

    @Override
    public void unregister(ISink sink)
    {
        Assert.checkState(started && !stopped);
        Assert.isInstanceOf(TcpSink.class, sink);
        
        TcpSink tcpSink = (TcpSink)sink;
        tcpSink.getConnection().unregister(tcpSink);
    }

    @Override
    public ITcpChannel.Parameters accept(InetSocketAddress remoteAddress)
    {
        ITcpChannel.Parameters channelParameters = createChannelParameters(false);
        TcpConnection connection = (TcpConnection)channelParameters.data;
        connection.setLocalAddress(localNode);
        
        return channelParameters;
    }

    @Override
    public void onConnected(ITcpChannel channel)
    {
        TcpConnection connection = channel.getData();
        connection.onConnected();
        
        parameters.connectionObserver.onNodesConnected(Collections.<IAddress>singleton(connection.getRemoteAddress()));
    }

    @Override
    public void onDisconnected(ITcpChannel channel)
    {
        TcpConnection connection = channel.getData();

        if (!connection.isDuplicate())
            connection.onClosed();
        else if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, channel.getMarker(), messages.connectionDuplicated());
        
        synchronized (this)
        {
            closedConnections.add(connection);
        }
        
        if (connection.isConnected())
            parameters.failureObserver.onNodesLeft(Collections.<IAddress>singleton(connection.getRemoteAddress()));
    }

    @Override
    public void onFailed(ITcpChannel channel)
    {
        TcpConnection connection = channel.getData();
        
        if (!connection.isDuplicate())
            connection.onClosed();
        else if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, channel.getMarker(), messages.connectionDuplicated());
        
        synchronized (this)
        {
            closedConnections.add(connection);
        }
      
        if (connection.isAdded() || connection.isConnected())
            parameters.failureObserver.onNodesFailed(Collections.<IAddress>singleton(connection.getRemoteAddress()));
    }

    @Override
    public synchronized void onNodesFailed(Set<IAddress> nodes)
    {
        for (IAddress node : nodes)
        {
            if (!(node instanceof UnicastAddress))
                continue;
            
            TcpConnection connection = connections.get(((UnicastAddress)node).getAddress(transportId));
            if (connection != null)
                connection.close();
        }
    }

    @Override
    public synchronized void onNodesLeft(Set<IAddress> nodes)
    {
        for (IAddress node : nodes)
        {
            if (!(node instanceof UnicastAddress))
                continue;
            
            TcpConnection connection = connections.get(((UnicastAddress)node).getAddress(transportId));
            if (connection != null)
                connection.disconnect();
        }
    }

    @Override
    public void connect(String nodeAddress)
    {
        InetSocketAddress address = getInetAddress(nodeAddress);
        if (stopped || (localNode != null && localNode.getAddress(transportId).equals(address)))
            return;
        
        synchronized (this)
        {
            TcpConnection connection = connections.get(address);
            if (connection == null && parameters.clientPart)
                createClientConnection(address, null);
        }
    }
    
    @Override
    public void connect(IAddress nodeAddress)
    {
        if (!started || stopped)
            return;
        Assert.isInstanceOf(UnicastAddress.class, nodeAddress);
        
        UnicastAddress address = (UnicastAddress)nodeAddress;
        if (address.equals(localNode))
            return;
        
        synchronized (this)
        {
            TcpConnection connection = connections.get(address.getAddress(transportId));
            if (connection == null && parameters.clientPart)
                createClientConnection(address.<InetSocketAddress>getAddress(transportId), address);
        }
    }
    
    @Override
    public void disconnect(String nodeAddress)
    {
        InetSocketAddress address = getInetAddress(nodeAddress);
        if (stopped || (localNode != null && localNode.getAddress(transportId).equals(address)))
            return;
        
        synchronized (this)
        {
            TcpConnection connection = connections.get(address);
            if (connection != null)
                connection.disconnect();
        }
    }

    @Override
    public void disconnect(IAddress nodeAddress)
    {
        if (!started || stopped)
            return;
        Assert.isInstanceOf(UnicastAddress.class, nodeAddress);
        
        UnicastAddress address = (UnicastAddress)nodeAddress;
        if (address.equals(localNode))
            return;
        
        synchronized (this)
        {
            TcpConnection connection = connections.get(address.getAddress(transportId));
            if (connection != null)
                connection.disconnect();
        }
    }
    
    @Override
    public String canonicalize(String nodeAddress)
    {
        InetSocketAddress address = getInetAddress(nodeAddress);
        return TcpUtils.getCanonicalConnectionAddress(address);
    }
    
    @Override
    public long getLastReadTime(IAddress node)
    {
        if (!started || stopped)
            return 0;
        
        Assert.isInstanceOf(UnicastAddress.class, node);
        UnicastAddress address = (UnicastAddress)node;
        
        synchronized (this)
        {
            TcpConnection connection = connections.get(address.getAddress(transportId));
            if (connection != null)
                return connection.getChannel().getLastReadTime();
        }
        
        return 0;
    }

    @Override
    public long getLastWriteTime(IAddress node)
    {
        if (!started || stopped)
            return 0;
        
        Assert.isInstanceOf(UnicastAddress.class, node);
        UnicastAddress address = (UnicastAddress)node;
        
        synchronized (this)
        {
            TcpConnection connection = connections.get(address.getAddress(transportId));
            if (connection != null)
                return connection.getChannel().getLastWriteTime();
        }
        
        return 0;
    }

    public void dumpConnection(IAddress node)
    {
        if (!started || stopped)
            return;
        
        Assert.isInstanceOf(UnicastAddress.class, node);
        UnicastAddress address = (UnicastAddress)node;
        
        synchronized (this)
        {
            TcpConnection connection = connections.get(address.getAddress(transportId));
            if (connection != null)
                connection.dump();
        }
    }

    @Override
    public synchronized void onTimer(long currentTime)
    {
        if (lastUpdateTime != 0 && currentTime - lastUpdateTime < timerPeriod)
            return;
        
        lastUpdateTime = currentTime;
        
        for (Iterator<TcpConnection> it = closedConnections.iterator(); it.hasNext(); )
        {
            TcpConnection connection = it.next();
            if (currentTime - connection.getCloseTime() > parameters.minReconnectPeriod)
            {
                if (connection.isDuplicate())
                    connection.onClosed();
                
                if (connection.getRemoteInetAddress() != null && connections.get(connection.getRemoteInetAddress()) == connection)
                    connections.remove(connection.getRemoteInetAddress());
                
                it.remove();
            }
        }
    }

    @Override
    public synchronized void start()
    {
        Assert.checkState(!started);
        
        started = true;
        
        InetSocketAddress inetAddress;
        if (parameters.serverPart)
        {
            ITcpServer.Parameters serverParameters = new ITcpServer.Parameters();
            serverParameters.name = parameters.channelName;
            serverParameters.bindAddress = parameters.bindAddress;
            serverParameters.portRangeStart = parameters.portRangeStart;
            serverParameters.portRangeEnd = parameters.portRangeEnd;
            serverParameters.channelAcceptor = this;
            serverParameters.connectionFilter = parameters.connectionFilter;
            serverParameters.adminFilter = parameters.adminFilter;
            serverParameters.socketChannelFactory = socketChannelFactory;
            server = dispatcher.createServer(serverParameters);
            inetAddress = server.getLocalAddress();
        }
        else
        {
            try
            {
                inetAddress = new InetSocketAddress(InetAddress.getLocalHost(), 0);
            }
            catch (UnknownHostException e)
            {
                throw new ChannelException(e);
            }
        }
        
        localNode = parameters.localNodeInitializer.setLocalNode(transportId, inetAddress, TcpUtils.getCanonicalConnectionAddress(inetAddress));

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.transportStarted());
        
        incomingMessageHandler.start();
    }

    @Override
    public void stop()
    {
        synchronized (this)
        {
            if (!started || stopped || requestToStop)
                return;
            
            requestToStop = true;
        }
        
        incomingMessageHandler.stop();
        
        synchronized (this)
        {
            stopped = true;
            server = null;
            connections.clear();
            localNode = null;
        }
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.transportStopped());
    }


    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new UnicastAddressSerializer());
        registry.register(new InetSocketAddressSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(UnicastAddressSerializer.ID);
        registry.unregister(InetSocketAddressSerializer.ID);
    }

    @Override
    public void lockFlow(IAddress flow)
    {
        Assert.checkState(started && !stopped);
        
        if (flow != null)
        {
            Assert.isInstanceOf(UnicastAddress.class, flow);
            
            UnicastAddress address = (UnicastAddress)flow;
            
            TcpConnection connection;
            synchronized (this)
            {
                connection = connections.get(address.getAddress(transportId));
                if (connection != null)
                    connection.lockFlow();
            }
        }
        else
        {
            synchronized (this)
            {
                for (TcpConnection connection : connections.values())
                    connection.lockFlow();
            }
        }
    }

    @Override
    public void unlockFlow(IAddress flow)
    {
        Assert.checkState(started && !stopped);
        
        if (flow != null)
        {
            Assert.isInstanceOf(UnicastAddress.class, flow);
            
            UnicastAddress address = (UnicastAddress)flow;
            
            TcpConnection connection;
            synchronized (this)
            {
                connection = connections.get(address.getAddress(transportId));
                if (connection != null)
                    connection.unlockFlow();
            }
        }
        else
        {
            synchronized (this)
            {
                for (TcpConnection connection : connections.values())
                    connection.unlockFlow();
            }
        }
    }

    private ITcpPacketChannel.Parameters createChannelParameters(boolean client)
    {
        TcpConnection connection = new TcpConnection(this, incomingMessageHandler, parameters.serializationRegistry, 
            parameters.messageFactory, threadingModel, parameters.maxUnlockSendQueueCapacity, parameters.minLockSendQueueCapacity, client,
            flowController, discardPolicy);
        
        ITcpPacketChannel.Parameters channelParameters = new ITcpPacketChannel.Parameters();
        TcpChannelHandshaker channelHandshaker = new TcpChannelHandshaker(connection);
        channelParameters.channelHandshaker = channelHandshaker;
        channelParameters.channelListeners.add(this);
        channelParameters.channelReader = connection.getChannelReader();
        channelParameters.channelWriter = connection.getChannelWriter();
        channelParameters.maxPacketSize = parameters.maxPacketSize;
        channelParameters.packetSerializer = null;
        channelParameters.name = null;
        channelParameters.data = connection;
        channelParameters.rateController = parameters.rateController;
        channelParameters.socketChannelFactory = socketChannelFactory;

        connection.setChannelHandshaker(channelHandshaker);
        
        return channelParameters;
    }
    
    private TcpConnection createClientConnection(InetSocketAddress address, UnicastAddress nodeAddress)
    {
        ITcpPacketChannel.Parameters channelParameters = createChannelParameters(true);
        TcpConnection connection = (TcpConnection)channelParameters.data;
        
        if (nodeAddress != null)
        {
            connection.setRemoteAddress(nodeAddress);
            connection.setAdded();
        }
        else
            connection.setRemoteInetAddress(address);
        
        connections.put(address, connection);
        
        try
        {
            dispatcher.suspend();
            
            ITcpChannel channel = dispatcher.createClient(address, parameters.bindAddress, channelParameters);
            if (parameters.serverPart)
                connection.setLocalAddress(localNode);
            else
            {
                UnicastAddress localAddress = new UnicastAddress(localNode.getId(), parameters.channelName);
                localAddress.setAddress(transportId, channel.getLocalAddress(), 
                    TcpUtils.getCanonicalConnectionAddress(channel.getLocalAddress()));
                connection.setLocalAddress(localAddress);
            }
        }
        catch (ChannelException e)
        {
            if (logger.isLogEnabled(LogLevel.ERROR));
                logger.log(LogLevel.ERROR, marker, e);
        }
        finally
        {
            dispatcher.resume();
        }
        
        return connection;
    }
    
    private InetSocketAddress getInetAddress(String connection)
    {
        Assert.notNull(connection);
        
        int pos = connection.indexOf(":");
        Assert.isTrue(pos != -1);
        
        String hostName = connection.substring(0, pos);
        int port = Integer.parseInt(connection.substring(pos + 1));
        
        InetSocketAddress address;
        try
        {
            address = new InetSocketAddress(InetAddress.getByName(hostName), port);
        }
        catch (UnknownHostException e)
        {
            throw new ChannelException(e);
        }
        return address;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Tcp transport has been started.")
        ILocalizedMessage transportStarted();
        @DefaultMessage("Connection has been closed as duplicated.")
        ILocalizedMessage connectionDuplicated();
        @DefaultMessage("Tcp transport has been stopped.")
        ILocalizedMessage transportStopped();
        @DefaultMessage("Message has been lost:\n{0}.")
        ILocalizedMessage messageLost(String message);
    }
}
