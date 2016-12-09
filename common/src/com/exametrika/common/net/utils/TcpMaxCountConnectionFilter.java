/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.utils;

import java.net.InetSocketAddress;

import com.exametrika.common.net.ITcpConnectionFilter;




/**
 * The {@link TcpMaxCountConnectionFilter} is a connection filter that allows only specified number of connections.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpMaxCountConnectionFilter implements ITcpConnectionFilter
{
    private final int maxConnectionCount;
    
    public TcpMaxCountConnectionFilter(int maxConnectionCount)
    {
        this.maxConnectionCount = maxConnectionCount;
    }
    
    @Override
    @SuppressWarnings("unused")
    public boolean allow(InetSocketAddress remoteAddress, Iterable<InetSocketAddress> existingConnections)
    {
        int count = 0;
        for (InetSocketAddress connection : existingConnections)
        {
            count++;
            if (count >= maxConnectionCount)
                return false;
        }
        
        return true;
    }
}
