/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.net.nio.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

import com.exametrika.common.net.nio.socket.TcpSocketChannel.State;
import com.exametrika.common.utils.Assert;


/**
 * The {@link TcpServerSocketChannel} is an implementation of {@link ITcpServerSocketChannel}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class TcpServerSocketChannel implements ITcpServerSocketChannel
{
    protected final ServerSocketChannel serverChannel;

    public TcpServerSocketChannel() throws IOException
    {
        this(ServerSocketChannel.open());
    }
    
    @Override
    public int validOps()
    {
        return serverChannel.validOps();
    }
    
    @Override
    public ServerSocket socket()
    {
        return serverChannel.socket();
    }
    
    @Override
    public ITcpSocketChannel accept() throws IOException
    {
        return new TcpSocketChannel(serverChannel.accept(), State.CONNECTING);
    }
    
    @Override
    public boolean isRegistered()
    {
        return serverChannel.isRegistered();
    }
    
    @Override
    public SelectionKey keyFor(ITcpSelector sel)
    {
        Assert.isInstanceOf(TcpSelector.class, sel);
        return serverChannel.keyFor(((TcpSelector)sel).getSelector());
    }
    
    @Override
    public SelectionKey register(ITcpSelector sel, int ops, Object att) throws ClosedChannelException
    {
        Assert.isInstanceOf(TcpSelector.class, sel);
        return serverChannel.register(((TcpSelector)sel).getSelector(), ops, att);
    }

    @Override
    public SelectionKey register(ITcpSelector sel, int ops) throws ClosedChannelException
    {
        Assert.isInstanceOf(TcpSelector.class, sel);
        return serverChannel.register(((TcpSelector)sel).getSelector(), ops);
    }

    @Override
    public boolean isBlocking()
    {
        return serverChannel.isBlocking();
    }
    
    @Override
    public Object blockingLock()
    {
        return serverChannel.blockingLock();
    }
    
    @Override
    public void configureBlocking(boolean block) throws IOException
    {
        serverChannel.configureBlocking(block);
    }
    
    @Override
    public boolean isOpen()
    {
        return serverChannel.isOpen();
    }
    
    @Override
    public void close() throws IOException
    {
        serverChannel.close();
    }
    
    protected TcpServerSocketChannel(ServerSocketChannel serverChannel)
    {
        Assert.notNull(serverChannel);
        
        this.serverChannel = serverChannel;
    }
}
