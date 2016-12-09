/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.exchange;

import java.util.Map;
import java.util.UUID;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link ProviderExchangeData} is exchange data from some data exchange provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ProviderExchangeData
{
    private final Map<UUID, NodeExchangeData> nodeExchanges;
    private final int size;

    public ProviderExchangeData(Map<UUID, NodeExchangeData> nodeExchanges)
    {
        Assert.notNull(nodeExchanges);
        
        this.nodeExchanges = Immutables.wrap(nodeExchanges);
        
        int size = 0;
        for (NodeExchangeData exchange : nodeExchanges.values())
            size += exchange.getData().getSize();
        
        this.size = size;
    }

    public Map<UUID, NodeExchangeData> getNodeExchanges()
    {
        return nodeExchanges;
    }
    
    public int getSize()
    {
        return size;
    }
    
    @Override
    public String toString()
    {
        return nodeExchanges.toString();
    }
}
