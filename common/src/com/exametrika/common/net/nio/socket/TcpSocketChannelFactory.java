/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.nio.socket;

import java.io.IOException;


/**
 * The {@link TcpSocketChannelFactory} is an implementation of {@link ITcpSocketChannelFactory} for plain TCP socket channels.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class TcpSocketChannelFactory implements ITcpSocketChannelFactory
{
    @Override
    public ITcpServerSocketChannel createServerSocketChannel() throws IOException
    {
        return new TcpServerSocketChannel();
    }

    @Override
    public ITcpSocketChannel createSocketChannel() throws IOException
    {
        return new TcpSocketChannel();
    }
}
