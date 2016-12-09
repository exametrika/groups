/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.utils;

import java.net.InetSocketAddress;

import com.exametrika.common.net.ITcpConnectionFilter;




/**
 * The {@link TcpCountPerNodeConnectionFilter} is a connection filter that allows only specified number of connections from each node.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpCountPerNodeConnectionFilter implements ITcpConnectionFilter
{
    private final int maxConnectionCountPerNode;
    
    public TcpCountPerNodeConnectionFilter(int maxConnectionCountPerNode)
    {
        this.maxConnectionCountPerNode = maxConnectionCountPerNode;
    }
    
    @Override
    public boolean allow(InetSocketAddress remoteAddress, Iterable<InetSocketAddress> existingConnections)
    {
        int count = 0;
        for (InetSocketAddress connection : existingConnections)
        {
            if (connection.getAddress().equals(remoteAddress.getAddress()))
            {
                count++;
                if (count >= maxConnectionCountPerNode)
                    return false;
            }
        }
        
        return true;
    }
}
