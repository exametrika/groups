/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.nio.ssl;

import java.io.IOException;

import javax.net.ssl.SSLContext;

import com.exametrika.common.net.nio.socket.ITcpServerSocketChannel;
import com.exametrika.common.net.nio.socket.ITcpSocketChannel;
import com.exametrika.common.net.nio.socket.ITcpSocketChannelFactory;
import com.exametrika.common.utils.Assert;


/**
 * The {@link TcpSslSocketChannelFactory} is an implementation of {@link ITcpSocketChannelFactory} for SSL over TCP socket channels.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class TcpSslSocketChannelFactory implements ITcpSocketChannelFactory
{
    private final SSLContext sslContext;
    private final boolean wantClientAuth;
    private final boolean needClientAuth;

    /**
     * Creates a new object.
     *
     * @param sslContext SSL context to be used
     * @param wantClientAuth if client authentication supported?
     * @param needClientAuth if client authentication required?
     */
    public TcpSslSocketChannelFactory(SSLContext sslContext, boolean wantClientAuth, boolean needClientAuth)
    {
        Assert.notNull(sslContext);
        
        this.sslContext = sslContext;
        this.wantClientAuth = wantClientAuth;
        this.needClientAuth = needClientAuth;
    }
    
    @Override
    public ITcpServerSocketChannel createServerSocketChannel() throws IOException
    {
        return new TcpSslServerSocketChannel(sslContext, wantClientAuth, needClientAuth);
    }

    @Override
    public ITcpSocketChannel createSocketChannel() throws IOException
    {
        return new TcpSslSocketChannel(sslContext);
    }
}
