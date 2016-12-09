/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.net.nio;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Set;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpChannelAcceptor;
import com.exametrika.common.net.ITcpChannelAware;
import com.exametrika.common.net.ITcpConnectionFilter;
import com.exametrika.common.net.ITcpDispatcher;
import com.exametrika.common.net.ITcpPacketChannel;
import com.exametrika.common.net.ITcpServer;
import com.exametrika.common.net.TcpAbstractChannel;
import com.exametrika.common.net.TcpException;
import com.exametrika.common.net.nio.socket.ITcpServerSocketChannel;
import com.exametrika.common.net.nio.socket.ITcpSocketChannel;
import com.exametrika.common.net.utils.TcpNameFilter;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.IOs;



/**
 * The {@link TcpNioServer} is a NIO implementaion of {@link ITcpServer}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpNioServer implements ITcpServer
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TcpNioServer.class);
    private final ITcpChannelAcceptor channelAcceptor;
    private final ITcpConnectionFilter connectionFilter;
    private final TcpNameFilter adminFilter;
    private final TcpNioDispatcher dispatcher;
    private final ITcpServerSocketChannel serverSocketChannel;
    private final InetSocketAddress localAddress;
    private final SelectionKey selectionKey;
    private final String name;
    private final IMarker marker;

    public TcpNioServer(ITcpServer.Parameters parameters, TcpNioDispatcher dispatcher)
    {
        Assert.notNull(parameters);
        Assert.notNull(parameters.channelAcceptor);
        Assert.notNull(parameters.socketChannelFactory);
        Assert.notNull(dispatcher);
        
        int rangeStart = parameters.portRangeStart != null ? parameters.portRangeStart : 0;
        int rangeEnd = parameters.portRangeEnd != null ? parameters.portRangeEnd : 65535;
        if (rangeStart == 0 && rangeEnd != 65535 && rangeEnd != 0)
            rangeStart = 1;
        
        if (rangeStart < 0 || rangeStart > 65535 || rangeEnd < 0 || rangeEnd > 65535 || rangeStart > rangeEnd)
            throw new IllegalArgumentException();

        InetAddress bindAddress = null;
        
        try
        {
            bindAddress = parameters.bindAddress != null ? parameters.bindAddress : InetAddress.getLocalHost();
        }
        catch (UnknownHostException e)
        {
            throw new TcpException(e); 
        }

        this.channelAcceptor = parameters.channelAcceptor;
        this.connectionFilter = parameters.connectionFilter;
        this.adminFilter = parameters.adminFilter;
        this.dispatcher = dispatcher;
        
        ITcpServerSocketChannel serverSocketChannel = null;
        
        int bindPort = 0;
        for (int i = rangeStart; i <= rangeEnd; i++)
        {
            try
            {
                serverSocketChannel = parameters.socketChannelFactory.createServerSocketChannel();
                ServerSocket socket = serverSocketChannel.socket();
                socket.bind(new InetSocketAddress(bindAddress, i));
                bindPort = socket.getLocalPort();
                serverSocketChannel.configureBlocking(false);
                break;
            }
            catch (SocketException e)
            {
                // Try another port
                IOs.close(serverSocketChannel);
                continue;
            }
            catch (SecurityException e)
            {
                // Try another port
                IOs.close(serverSocketChannel);
                continue;
            }
            catch (Exception e)
            {
                IOs.close(serverSocketChannel);
                throw new TcpException(e);
            }
        }

        if (serverSocketChannel == null)
            throw new TcpException(messages.createSocketError(bindAddress, rangeStart, rangeEnd));

        this.serverSocketChannel = serverSocketChannel;
        localAddress = new InetSocketAddress(bindAddress, bindPort);
        this.name = parameters.name;
        this.marker = Loggers.getMarker("server:" + getName(), dispatcher.getMarker());
        
        try
        {
            selectionKey = serverSocketChannel.register(dispatcher.getSelector(), SelectionKey.OP_ACCEPT, this);
        }
        catch (Exception e)
        {
            IOs.close(serverSocketChannel);
            throw new TcpException(e);
        }
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.serverStarted(localAddress));
    }
    
    @Override
    public String getName()
    {
        if (name != null)
            return name;
        
        return localAddress.toString();
    }
    
    @Override
    public ITcpDispatcher getDispatcher()
    {
        return dispatcher;
    }
    
    @Override 
    public boolean isOpened()
    {
        return serverSocketChannel.isOpen();
    }
    
    @Override 
    public InetSocketAddress getLocalAddress()
    {
        return localAddress;
    }

    @Override
    public synchronized void close()
    {
        if (!serverSocketChannel.isOpen())
            return;
        
        selectionKey.cancel();
        IOs.close(serverSocketChannel);
        dispatcher.decrementServerCount();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.serverStopped());
    }
    
    public void handleAccept()
    {
        ITcpSocketChannel socketChannel = null;
        
        try
        {
            socketChannel = serverSocketChannel.accept();
            if (socketChannel == null)
                return;
            
            InetSocketAddress remoteAddress = (InetSocketAddress)socketChannel.socket().getRemoteSocketAddress();
            
            boolean admin = false;
            if (adminFilter != null && adminFilter.match(remoteAddress))
                admin = true;
            
            boolean allowConnection = true;
            if (!admin && connectionFilter != null)
                allowConnection = connectionFilter.allow(remoteAddress, new ConnectionIterable(selectionKey.selector().keys()));
                
            ITcpChannel.Parameters parameters = null;
            if (allowConnection)
                parameters = channelAcceptor.accept(remoteAddress);
            
            if (parameters == null)
            {
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.channelRejected((InetSocketAddress)socketChannel.socket().getRemoteSocketAddress()));
                
                IOs.close(socketChannel);
                return;
            }
            
            TcpAbstractChannel channel = new TcpNioPacketChannel((ITcpPacketChannel.Parameters)parameters, socketChannel, dispatcher);
            
            if (parameters.data instanceof ITcpChannelAware)
                ((ITcpChannelAware)parameters.data).setChannel(channel);
            
            if (admin)
                channel.setAdmin();
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, channel.getMarker(), messages.channelAccepted());
        }
        catch (Exception e)
        {
            IOs.close(socketChannel);
            
            throw new TcpException(e);
        }
    }
    
    @Override
    public String toString()
    {
        return getName();
    }
    
    @Override
    protected void finalize()
    {
        close();
    }

    private static class ConnectionIterable implements Iterable<InetSocketAddress>
    {
        private final Set<SelectionKey> keys;

        public ConnectionIterable(Set<SelectionKey> keys)
        {
            Assert.notNull(keys);
            
            this.keys = keys;
        }
        
        @Override
        public Iterator<InetSocketAddress> iterator()
        {
            return new ConnectionIterator(keys);
        }
    }
    
    private static class ConnectionIterator implements Iterator<InetSocketAddress>
    {
        private final Iterator<SelectionKey> it;
        private ITcpChannel channel;
        
        public ConnectionIterator(Set<SelectionKey> keys)
        {
            Assert.notNull(keys);
            
            it = keys.iterator();
            channel = findNext();
        }
        
        @Override
        public boolean hasNext()
        {
            return channel != null;
        }

        @Override
        public InetSocketAddress next()
        {
            Assert.checkState(channel != null);
            
            InetSocketAddress result = channel.getRemoteAddress();
            channel = findNext();
            return result;
        }

        @Override
        public void remove()
        {
            Assert.supports(false);
        }
        
        private ITcpChannel findNext()
        {
            while (it.hasNext())
            {
                SelectionKey key = it.next();
                if (key.attachment() instanceof ITcpChannel)
                    return (ITcpChannel)key.attachment();
            }
            
            return null;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Could not create socket on bind address ''{0}'' and port in range [{1}..{2}].")
        ILocalizedMessage createSocketError(InetAddress bindAddress, int portRangeStart, int portRangeEnd);
        @DefaultMessage("TCP channel has been accepted.")
        ILocalizedMessage channelAccepted();
        @DefaultMessage("TCP channel ''{0}'' has been rejected.")
        ILocalizedMessage channelRejected(InetSocketAddress remoteSocketAddress);
        @DefaultMessage("TCP server has been started at ''{0}''.")
        ILocalizedMessage serverStarted(InetSocketAddress address);
        @DefaultMessage("TCP server has been stopped.")
        ILocalizedMessage serverStopped();
    }    
}
