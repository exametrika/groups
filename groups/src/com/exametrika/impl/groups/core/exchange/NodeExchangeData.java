/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.exchange;

import com.exametrika.common.utils.Assert;

/**
 * The {@link NodeExchangeData} is exchange data from some node and some data exchange provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NodeExchangeData
{
    private final long id;
    private final IExchangeData data;

    public NodeExchangeData(long id, IExchangeData data)
    {
        Assert.notNull(data);
        
        this.id = id;
        this.data = data;
    }

    public long getId()
    {
        return id;
    }
    
    public IExchangeData getData()
    {
        return data;
    }
    
    @Override
    public String toString()
    {
        return id + ":[" + data + "]";
    }
}
