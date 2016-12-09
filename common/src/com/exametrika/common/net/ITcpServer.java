/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.exametrika.common.net.nio.socket.ITcpSocketChannelFactory;
import com.exametrika.common.net.nio.socket.TcpSocketChannelFactory;
import com.exametrika.common.net.utils.TcpNameFilter;



/**
 * The {@link ITcpServer} is a TCP server.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpServer
{
    /**
     * Initialization parameters.
     */
    public class Parameters
    {
        /** Server name. Can be <c>null<c> if not set. */
        public String name;
        /** Network interface bind address. If null, {@link InetAddress#getLocalHost()} is used. */
        public InetAddress bindAddress;
        /** Start of port range to choose bind port from. If null, 0 is used which means any local port from range [0..65535]. */
        public Integer portRangeStart;
        /** End of port range to choose bind port from. If null, 65535 is used, if not null and portRangeStart
         *  is null or 0, port range start set to 1 (because 0 is any local port). */
        public Integer portRangeEnd;
        /** Channel acceptor. */
        public ITcpChannelAcceptor channelAcceptor;
        /** Connection filter. Can be <c>null<c> if not set. */
        public ITcpConnectionFilter connectionFilter;
        /** Filter of admin hosts. Can be <c>null<c> if not set. */
        public TcpNameFilter adminFilter;
        /** Socket channel factory. */
        public ITcpSocketChannelFactory socketChannelFactory = new TcpSocketChannelFactory();
    }

    /**
     * Returns server name.
     *
     * @return server name or null is server does not have a name
     */
    String getName();
    
    /**
     * Returns dispatcher.
     *
     * @return dispatcher
     */
    ITcpDispatcher getDispatcher();
    
    /**
     * Is server ready to accept client connections?
     *
     * @return true if server is ready to accept client connections, false if is not ready
     */
    boolean isOpened();
    
    /**
     * Returns local adress server bound to.
     *
     * @return server's local address
     */
    InetSocketAddress getLocalAddress();
    
    /**
     * Closes server.
     */
    void close();
}
