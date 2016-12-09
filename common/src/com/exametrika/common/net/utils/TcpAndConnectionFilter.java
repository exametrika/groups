/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.utils;

import java.net.InetSocketAddress;
import java.util.List;

import com.exametrika.common.net.ITcpConnectionFilter;
import com.exametrika.common.utils.Assert;




/**
 * The {@link TcpAndConnectionFilter} is a composite connection filter that combines filters by AND.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpAndConnectionFilter implements ITcpConnectionFilter
{
    private final List<ITcpConnectionFilter> filters;
    
    public TcpAndConnectionFilter(List<ITcpConnectionFilter> filters)
    {
        Assert.notNull(filters);
        
        this.filters = filters;
    }
    
    @Override
    public boolean allow(InetSocketAddress remoteAddress, Iterable<InetSocketAddress> existingConnections)
    {
        for (ITcpConnectionFilter filter : filters)
        {
            if (!filter.allow(remoteAddress, existingConnections))
                return false;
        }
        
        return true;
    }
}
