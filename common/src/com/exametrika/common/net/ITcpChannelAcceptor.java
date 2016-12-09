/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;

import java.net.InetSocketAddress;




/**
 * The {@link ITcpChannelAcceptor} accepts or rejects new connections.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpChannelAcceptor
{
    /**
     * Called when new connection is about to be accepted.
     *
     * @param remoteAddress remote address connecting to this server
     * @return channel parameters if channel can be accepted or null if channel must be rejected
     */
    ITcpChannel.Parameters accept(InetSocketAddress remoteAddress);
}
