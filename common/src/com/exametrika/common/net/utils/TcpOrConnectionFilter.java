/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.utils;

import java.net.InetSocketAddress;
import java.util.List;

import com.exametrika.common.net.ITcpConnectionFilter;
import com.exametrika.common.utils.Assert;




/**
 * The {@link TcpOrConnectionFilter} is a composite connection filter that combines filters by OR.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpOrConnectionFilter implements ITcpConnectionFilter
{
    private final List<ITcpConnectionFilter> filters;
    
    public TcpOrConnectionFilter(List<ITcpConnectionFilter> filters)
    {
        Assert.notNull(filters);
        
        this.filters = filters;
    }
    
    @Override
    public boolean allow(InetSocketAddress remoteAddress, Iterable<InetSocketAddress> existingConnections)
    {
        for (ITcpConnectionFilter filter : filters)
        {
            if (filter.allow(remoteAddress, existingConnections))
                return true;
        }
        
        return false;
    }
}
