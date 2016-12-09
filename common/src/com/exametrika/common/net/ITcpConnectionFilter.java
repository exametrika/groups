/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;

import java.net.InetSocketAddress;


/**
 * The {@link ITcpConnectionFilter} is a filter of accepted connections.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ITcpConnectionFilter
{
    /**
     * Allows or denies establishing of new connection for specified address.
     *
     * @param remoteAddress remote address connecting to this server
     * @param existingConnections list of existing connection addresses
     * @return true if establishing of connection is allowed
     */
    boolean allow(InetSocketAddress remoteAddress, Iterable<InetSocketAddress> existingConnections);
}
