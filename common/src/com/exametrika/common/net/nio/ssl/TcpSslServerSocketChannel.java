/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.net.nio.ssl;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.exametrika.common.net.nio.socket.TcpServerSocketChannel;
import com.exametrika.common.net.nio.socket.TcpSocketChannel.State;
import com.exametrika.common.utils.Assert;


/**
 * The {@link TcpSslServerSocketChannel} represents a SSL server socket channel implementation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpSslServerSocketChannel extends TcpServerSocketChannel
{
    private final SSLContext sslContext;
    private final boolean wantClientAuth;
    private final boolean needClientAuth;

    public TcpSslServerSocketChannel(SSLContext sslContext,
        boolean wantClientAuth, boolean needClientAuth) throws IOException
    {
        this(ServerSocketChannel.open(), sslContext, wantClientAuth, needClientAuth);
    }

    @Override
    public TcpSslSocketChannel accept() throws IOException
    {
        SocketChannel channel = serverChannel.accept();
        SSLEngine engine = sslContext.createSSLEngine();

        engine.setWantClientAuth(wantClientAuth);
        engine.setNeedClientAuth(needClientAuth);

        return new TcpSslSocketChannel(channel, engine, false, State.CONNECTING);
    }

    private TcpSslServerSocketChannel(ServerSocketChannel serverChannel, SSLContext sslContext, boolean wantClientAuth,
        boolean needClientAuth)
    {
        super(serverChannel);

        Assert.notNull(sslContext);

        this.sslContext = sslContext;
        this.wantClientAuth = wantClientAuth;
        this.needClientAuth = needClientAuth;
    }
}
