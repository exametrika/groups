/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.net.nio;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;

import com.exametrika.common.json.Json;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpDispatcher;
import com.exametrika.common.net.ITcpRateController;
import com.exametrika.common.net.TcpAbstractChannel;
import com.exametrika.common.net.TcpChannelException;
import com.exametrika.common.net.TcpException;
import com.exametrika.common.net.nio.socket.ITcpSocketChannel;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.IOs;



/**
 * The {@link TcpNioAbstractChannel} is a NIO abstract implementaion of {@link ITcpChannel}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class TcpNioAbstractChannel extends TcpAbstractChannel
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TcpNioAbstractChannel.class);
    protected final TcpNioDispatcher dispatcher;
    protected final ITcpSocketChannel socketChannel;
    private final SelectionKey selectionKey;
    private volatile State state = State.NOT_CONNECTED;
    protected volatile long lastReadTime;
    protected volatile long lastWriteTime;
    protected final ITcpRateController rateController;

    public TcpNioAbstractChannel(InetSocketAddress remoteAddress, InetAddress bindAddress, ITcpChannel.Parameters parameters, 
        TcpNioDispatcher dispatcher)
    {
        super(parameters, dispatcher.getMarker());
        
        Assert.notNull(remoteAddress);
        Assert.notNull(parameters);
        Assert.notNull(parameters.channelReader);
        Assert.notNull(parameters.channelWriter);
        Assert.notNull(parameters.channelListeners);
        Assert.notNull(parameters.socketChannelFactory);
        Assert.notNull(dispatcher);

        try
        {
            bindAddress = bindAddress != null ? bindAddress : InetAddress.getLocalHost();
        }
        catch (UnknownHostException e)
        {
            throw new TcpChannelException(this, e); 
        }

        ITcpSocketChannel socketChannel = null;

        try
        {
            socketChannel = parameters.socketChannelFactory.createSocketChannel();
            socketChannel.configureBlocking(false);
            socketChannel.socket().bind(new InetSocketAddress(bindAddress, 0));

            socketChannel.socket().setTcpNoDelay(true);
            
            if (parameters.sendBufferSize != null)
                socketChannel.socket().setSendBufferSize(parameters.sendBufferSize);
            if (parameters.receiveBufferSize != null)
                socketChannel.socket().setReceiveBufferSize(parameters.receiveBufferSize);
            if (parameters.trafficClass != null)
                socketChannel.socket().setTrafficClass(parameters.trafficClass);

            this.dispatcher = dispatcher;
            this.socketChannel = socketChannel;
            
            if (parameters.rateController != null)
                rateController = parameters.rateController.createController(this);
            else
                rateController = null;
            
            this.lastReadTime = dispatcher.getCurrentTime();
            this.lastWriteTime = dispatcher.getCurrentTime();
            
            selectionKey = socketChannel.register(dispatcher.getSelector(), SelectionKey.OP_CONNECT, this);
            
            socketChannel.connect(remoteAddress);
        }
        catch (Exception e)
        {
            IOs.close(socketChannel);
            throw new TcpChannelException(this, e);
        }
        
        updateMarker();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, getMarker(), messages.channelCreated(socketChannel.socket().getLocalSocketAddress(),
                remoteAddress));
    }
    
    public TcpNioAbstractChannel(ITcpChannel.Parameters parameters, ITcpSocketChannel socketChannel, TcpNioDispatcher dispatcher)
    {
        super(parameters, dispatcher.getMarker());
        
        Assert.notNull(parameters);
        Assert.notNull(parameters.channelReader);
        Assert.notNull(parameters.channelWriter);
        Assert.notNull(parameters.channelListeners);
        Assert.notNull(socketChannel);
        Assert.notNull(dispatcher);
        
        try
        {
            socketChannel.configureBlocking(false);
            
            socketChannel.socket().setTcpNoDelay(true);
            
            if (parameters.sendBufferSize != null)
                socketChannel.socket().setSendBufferSize(parameters.sendBufferSize);
            if (parameters.receiveBufferSize != null)
                socketChannel.socket().setReceiveBufferSize(parameters.receiveBufferSize);
            if (parameters.trafficClass != null)
                socketChannel.socket().setTrafficClass(parameters.trafficClass);
            
            this.dispatcher = dispatcher;
            this.socketChannel = socketChannel;
            
            if (parameters.rateController != null)
                rateController = parameters.rateController.createController(this);
            else
                rateController = null;
            
            this.lastReadTime = dispatcher.getCurrentTime();
            this.lastWriteTime = dispatcher.getCurrentTime();
            
            selectionKey = socketChannel.register(dispatcher.getSelector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
        }
        catch (Exception e)
        {
            IOs.close(socketChannel);
            throw new TcpChannelException(this, e);
        }

        updateMarker();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, getMarker(), messages.channelCreated(socketChannel.socket().getLocalSocketAddress(),
                socketChannel.socket().getRemoteSocketAddress()));

        handshake();
    }

    @Override
    public ITcpDispatcher getDispatcher()
    {
        return dispatcher;
    }
    
    @Override
    public final String getName()
    {
        String name = super.getName();
        if (name != null)
            return name;
        
        SocketAddress address = socketChannel.socket().getRemoteSocketAddress();
        if (address != null)
            return address.toString();

        return messages.notConnected().toString();
    }
    
    @Override
    public final boolean isConnected()
    {
        return state == State.CONNECTED;
    }
    
    @Override
    public final boolean isDisconnected()
    {
        return state == State.DISCONNECTED;
    }
    
    @Override
    public final InetSocketAddress getRemoteAddress()
    {
        return (InetSocketAddress)socketChannel.socket().getRemoteSocketAddress();
    }
    
    @Override
    public final InetSocketAddress getLocalAddress()
    {
        return (InetSocketAddress)socketChannel.socket().getLocalSocketAddress();
    }
    
    @Override
    public final long getLastReadTime()
    {
        return lastReadTime;
    }
    
    @Override
    public final long getLastWriteTime()
    {
        return lastWriteTime;
    }
    
    @Override
    public final synchronized void updateReadStatus()
    {
        enableOperation(SelectionKey.OP_READ, canRead());
    }

    @Override
    public final synchronized void updateWriteStatus()
    {
        enableOperation(SelectionKey.OP_WRITE, canWrite());
    }

    @Override
    public final void disconnect()
    {
        if (state == State.DISCONNECTED)
            return;
        
        if (!dispatcher.isMainThread())
        {
            dispatcher.addDisconnectEvent(this);
            return;
        }
        
        if (state != State.CONNECTED && state != State.HANDSHAKING && state != State.DISCONNECTING && state != State.SOCKET_DISCONNECTING)
        {
            close();
            return;
        }
        
        try
        {
            boolean disconnected = false;

            if (state == State.CONNECTED || state == State.HANDSHAKING)
            {
                state = State.DISCONNECTING;
                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                selectionKey.selector().wakeup();
            }
            else if (state == State.DISCONNECTING)
            {
                if (doDisconnect() && flush())
                {
                    state = State.SOCKET_DISCONNECTING;
                    selectionKey.selector().wakeup();
                }
                else
                    updateWriteStatus();
            }
            else if (state == State.SOCKET_DISCONNECTING && socketChannel.disconnect())
                disconnected = true;
            
            if (disconnected)
            {
                closeChannel();
                
                fireDisconnected();
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, getMarker(), messages.channelDisconnected());
            }
        }
        catch (Exception e)
        {
            close();
            throw new TcpChannelException(this, e);
        }
    }
    
    @Override
    public final void close()
    {
        if (state == State.DISCONNECTED)
            return;
        
        if (!dispatcher.isMainThread())
        {
            dispatcher.addCloseEvent(this);
            return;
        }

        if (!closeChannel())
            return;
        
        fireFailed();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, getMarker(), messages.channelFailed());
    }

    private boolean closeChannel()
    {
        if (state == State.DISCONNECTED)
            return false;
        
        selectionKey.cancel();
        IOs.close(socketChannel);
        state = State.DISCONNECTED;
        
        doClose();
        
        return true;
    }
    
    public final void handleConnect()
    {
        try
        {
            socketChannel.finishConnect();
            selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            
            updateMarker();
        }
        catch (Exception e)
        {
            close();
            throw new TcpChannelException(this, e);
        }
        
        handshake();
    }
    
    public final boolean handleRead()
    {
        try
        {
            switch (state)
            {
            case SOCKET_HANDSHAKING:
                handshake();
                return true;
            case HANDSHAKING:
                doRead();
                handshake();
                return !hasReadData();
            case CONNECTED:
                doRead();
                return !hasReadData();
            case DISCONNECTING:
                doRead();
                disconnect();
                return !hasReadData();
            case SOCKET_DISCONNECTING:
                disconnect();
                return true;
            case NOT_CONNECTED:
            case DISCONNECTED:
            default:
                return (Boolean)Assert.error();
            }
        }
        catch (TcpException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            close();
            throw new TcpChannelException(this, e);
        }
    }

    public final void handleWrite()
    {
        try
        {
            switch (state)
            {
            case SOCKET_HANDSHAKING:
                handshake();
                break;
            case HANDSHAKING:
                handshake();
                doWrite();
                break;
            case CONNECTED:
                doWrite();
                break;
            case DISCONNECTING:
                disconnect();
                if (state == State.DISCONNECTING)
                    doWrite();
                break;
            case SOCKET_DISCONNECTING:
                disconnect();
                break;
            case NOT_CONNECTED:
            case DISCONNECTED:
            default:
                Assert.error();
            }
        }
        catch (TcpException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            close();
            throw new TcpChannelException(this, e);
        }
    }

    @Override
    public void onTimer(ITimeService timeService)
    {
        if (rateController != null)
            rateController.onTimer(timeService.getCurrentTime());
    }
    
    @Override
    public final String toString()
    {
        return dispatcher.getName() + "->" + getName();
    }

    public boolean hasReadData()
    {
        return socketChannel.hasReadData();
    }
    
    protected boolean canRead()
    {
        if (state == State.CONNECTED)
            return (rateController != null ? rateController.canRead() : true) && channelReader.canRead(this);
        else
            return true;
    }
    
    protected boolean canWrite()
    {
        if (state == State.CONNECTED)
            return (rateController != null ? rateController.canWrite() : true) && channelWriter.canWrite(this);
        else
            return false;
    }

    protected void doRead()
    {
        if (state == State.CONNECTED)
            channelReader.onRead(this);
    }

    protected void doWrite()
    {
        if (state == State.CONNECTED)
            channelWriter.onWrite(this);
    }

    protected void doClose()
    {
    }
    
    protected boolean doHandshake()
    {
        return true;
    }
    
    protected boolean doDisconnect()
    {
        return true;
    }
    
    protected boolean flush()
    {
        return true;
    }
    
    @Override
    protected void doUpdateMarker(IMarker marker)
    {
        socketChannel.setMarker(marker);
    }
    
    protected void dump(Json json)
    {
        json.put("name", getName())
          .put("state", state)
          .put("lastReadTime", lastReadTime)
          .put("lastWriteTime", lastWriteTime)
          .put("selectionKey.valid", selectionKey.isValid())
          .put("selectionKey.readable", selectionKey.isReadable())
          .put("selectionKey.writable", selectionKey.isWritable())
          .put("selectionKey.interestOpts", selectionKey.interestOps())
          .put("selectionKey.readyOpts", selectionKey.readyOps())
          .put("channelWriter.canWrite", channelWriter.canWrite(this));
    }    

    private void enableOperation(int operation, boolean enable)
    {
        int interestOps = selectionKey.interestOps();
        
        if (enable && (interestOps | operation) != interestOps)
        {
            if (logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, getMarker(), messages.channelEnabled(operation == SelectionKey.OP_READ ? "read" : "write"));
            
            selectionKey.interestOps(interestOps | operation);
            selectionKey.selector().wakeup();
        }
        else if (!enable && (interestOps & ~operation) != interestOps)
        {
            if (logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, getMarker(), messages.channelDisabled(operation == SelectionKey.OP_READ ? "read" : "write"));
            
            selectionKey.interestOps(interestOps & ~operation);
        }
    }

    private void handshake()
    {
        try
        {
            if (state == State.NOT_CONNECTED)
            {
                state = State.SOCKET_HANDSHAKING;
                selectionKey.selector().wakeup();
            }
            else if (state == State.SOCKET_HANDSHAKING && socketChannel.handshake())
            {
                state = State.HANDSHAKING;
                selectionKey.selector().wakeup();
            }
            else if (state == State.HANDSHAKING)
            {
                if (doHandshake())
                {
                    state = State.CONNECTED;
                    lastReadTime = dispatcher.getCurrentTime();
                    lastWriteTime = dispatcher.getCurrentTime();
                    
                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, getMarker(), messages.channelConnected());
                    
                    fireConnected();
                    
                    updateReadStatus();
                    updateWriteStatus();
                }
                else
                    updateWriteStatus();
            }
        }
        catch (Exception e)
        {
            close();
            throw new TcpChannelException(this, e);
        }
    }

    private enum State
    {
        NOT_CONNECTED,
        SOCKET_HANDSHAKING,
        HANDSHAKING,
        CONNECTED,
        DISCONNECTING,
        SOCKET_DISCONNECTING,
        DISCONNECTED
    }
    
    private interface IMessages
    {
        @DefaultMessage("TCP channel has been created. Local address: {0}, remote address: {1}.")
        ILocalizedMessage channelCreated(SocketAddress localAddress, SocketAddress remoteAddress);
        @DefaultMessage("TCP channel has been connected.")
        ILocalizedMessage channelConnected();
        @DefaultMessage("TCP channel has been disconnected.")
        ILocalizedMessage channelDisconnected();
        @DefaultMessage("TCP channel has been forcefully closed or failed.")
        ILocalizedMessage channelFailed();
        @DefaultMessage("(not connected)")
        ILocalizedMessage notConnected();
        @DefaultMessage("TCP channel {0} has been enabled.")
        ILocalizedMessage channelEnabled(String operation);
        @DefaultMessage("TCP channel {0} has been disabled.")
        ILocalizedMessage channelDisabled(String operation);
    }
}
