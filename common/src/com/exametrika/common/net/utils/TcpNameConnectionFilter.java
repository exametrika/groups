/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.utils;

import java.net.InetSocketAddress;

import com.exametrika.common.net.ITcpConnectionFilter;
import com.exametrika.common.utils.Assert;



/**
 * The {@link TcpNameConnectionFilter} represents a connection filter by IP address or host name.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpNameConnectionFilter implements ITcpConnectionFilter
{
    private final TcpNameFilter nameFilter;

    public TcpNameConnectionFilter(TcpNameFilter nameFilter)
    {
        Assert.notNull(nameFilter);
        
        this.nameFilter = nameFilter;
    }

    @Override
    public boolean allow(InetSocketAddress remoteAddress, Iterable<InetSocketAddress> existingConnections)
    {
        return nameFilter.match(remoteAddress);
    }
}
