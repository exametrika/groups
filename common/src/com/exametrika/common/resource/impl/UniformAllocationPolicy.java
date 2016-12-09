/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.util.Map;

import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.IResourceConsumer;

/**
 * The {@link UniformAllocationPolicy} is an allocation policy which allocates equal amount of resource between all consumers.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class UniformAllocationPolicy implements IAllocationPolicy
{
    @Override
    public void allocate(long amount, Map<String, IResourceConsumer> consumers)
    {
        if (consumers.isEmpty())
            return;
        
        long value = amount / consumers.size();
        for (IResourceConsumer consumer : consumers.values())
            consumer.setQuota(value);
    }
}
