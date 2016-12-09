/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.nio.socket;

import java.io.IOException;



/**
 * The {@link ITcpSocketChannelFactory} is a factory of TCP socket channels.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpSocketChannelFactory
{
    /**
     * Creates server socket channel.
     *
     * @return server socket channel
     * @exception IOException if IO exception occurs
     */
    ITcpServerSocketChannel createServerSocketChannel() throws IOException;

    /**
     * Creates socket channel.
     *
     * @return server socket channel
     * @exception IOException if IO exception occurs
     */
    ITcpSocketChannel createSocketChannel() throws IOException;
}
