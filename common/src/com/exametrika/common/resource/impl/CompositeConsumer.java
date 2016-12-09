/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.util.Map;

import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.utils.Assert;

/**
 * The {@link CompositeConsumer} is a composite consumer.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class CompositeConsumer implements IResourceConsumer
{
    protected final Map<String, IResourceConsumer> consumers;
    protected final IAllocationPolicy allocationPolicy;
    private long quota;

    public CompositeConsumer(Map<String, IResourceConsumer> consumers, IAllocationPolicy allocationPolicy)
    {
        Assert.notNull(consumers);
        Assert.notNull(allocationPolicy);
        
        this.consumers = consumers;
        this.allocationPolicy = allocationPolicy;
    }
    
    public int getCount()
    {
        return consumers.size();
    }
    
    @Override
    public final long getAmount()
    {
        long amount = 0;
        for (IResourceConsumer consumer : consumers.values())
            amount += consumer.getAmount();
        
        return amount;
    }

    @Override
    public final long getQuota()
    {
        return quota;
    }

    @Override
    public final void setQuota(long value)
    {
        quota = value;
        allocationPolicy.allocate(quota, consumers);
    }
}