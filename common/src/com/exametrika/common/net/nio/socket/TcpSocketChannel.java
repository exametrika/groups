/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.net.nio.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;


/**
 * The {@link TcpSocketChannel} is an implementation of {@link ITcpSocketChannel}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class TcpSocketChannel implements ITcpSocketChannel
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ILogger logger = Loggers.get(TcpSocketChannel.class);
    protected final SocketChannel channel;
    protected volatile IMarker marker;
    protected volatile State state = State.NOT_CONNECTED;

    public enum State
    {
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED
    }

    public TcpSocketChannel() throws IOException
    {
        this(SocketChannel.open(), State.NOT_CONNECTED);
    }
    
    public TcpSocketChannel(InetSocketAddress remoteSocketAddress) throws IOException
    {
        this(SocketChannel.open(remoteSocketAddress), State.CONNECTING);
    }

    public SocketChannel getChannel()
    {
        return channel;
    }
    
    @Override
    public void setMarker(IMarker marker)
    {
        this.marker = marker;
    }
    
    public State getState()
    {
        return state;
    }
    
    @Override
    public int validOps()
    {
        return channel.validOps();
    }
    
    @Override
    public Socket socket()
    {
        return channel.socket();
    }
    
    @Override
    public boolean isConnected()
    {
        return state == State.CONNECTED;
    }
    
    @Override
    public boolean connect(SocketAddress remote) throws IOException
    {
        return channel.connect(remote);
    }
    
    @Override
    public boolean finishConnect() throws IOException
    {
        return channel.finishConnect();
    }
    
    @Override
    public int read(ByteBuffer dst) throws IOException
    {
        int n = channel.read(dst);
        if (n > 0 && logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.channelRead(n));
        
        return n;
    }
    
    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException
    {
        long n = channel.read(dsts, offset, length);
        if (n > 0 && logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.channelRead(n));
        
        return n;
    }
    
    @Override
    public long read(ByteBuffer[] dsts) throws IOException
    {
        return read(dsts, 0, dsts.length);
    }
    
    @Override
    public int write(ByteBuffer src) throws IOException
    {
        int n = channel.write(src);
        if (n > 0 && logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.channelWrite(n));
        
        return n;
    }
    
    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException
    {
        long n = channel.write(srcs, offset, length);
        if (n > 0 && logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.channelWrite(n));
        
        return n;
    }
    
    @Override
    public long write(ByteBuffer[] srcs) throws IOException
    {
        return write(srcs, 0, srcs.length);
    }
    
    @Override
    public boolean isRegistered()
    {
        return channel.isRegistered();
    }
    
    @Override
    public SelectionKey keyFor(ITcpSelector sel)
    {
        Assert.isInstanceOf(TcpSelector.class, sel);
        return channel.keyFor(((TcpSelector)sel).getSelector());
    }
    
    @Override
    public SelectionKey register(ITcpSelector sel, int ops, Object att) throws ClosedChannelException
    {
        Assert.isInstanceOf(TcpSelector.class, sel);
        return channel.register(((TcpSelector)sel).getSelector(), ops, att);
    }

    @Override
    public SelectionKey register(ITcpSelector sel, int ops) throws ClosedChannelException
    {
        Assert.isInstanceOf(TcpSelector.class, sel);
        return channel.register(((TcpSelector)sel).getSelector(), ops);
    }

    @Override
    public boolean isBlocking()
    {
        return channel.isBlocking();
    }
    
    @Override
    public Object blockingLock()
    {
        return channel.blockingLock();
    }
    
    @Override
    public void configureBlocking(boolean block) throws IOException
    {
        channel.configureBlocking(block);
    }
    
    @Override
    public boolean isOpen()
    {
        return channel.isOpen();
    }
    
    @Override
    public void close() throws IOException
    {
        state = State.DISCONNECTED;
        channel.close();
    }

    @Override
    public boolean handshake() throws IOException
    {
        state = State.CONNECTED;
        return true;
    }
    
    @Override
    public boolean disconnect() throws IOException
    {
        state = State.DISCONNECTING;
        return true;
    }
    
    @Override
    public boolean flush() throws IOException
    {
        return true;
    }
    
    @Override
    public boolean hasReadData()
    {
        return false;
    }
    
    @Override
    public boolean hasWriteData()
    {
        return false;
    }
    
    protected TcpSocketChannel(SocketChannel channel, State state)
    {
        Assert.notNull(channel);
        
        this.channel = channel;
        this.state = state;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Data has been read: {0}.")
        ILocalizedMessage channelRead(long count);
        @DefaultMessage("Data has been written: {0}.")
        ILocalizedMessage channelWrite(long count);
    }
}
