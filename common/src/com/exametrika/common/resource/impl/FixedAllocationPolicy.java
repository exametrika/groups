/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;

/**
 * The {@link FixedAllocationPolicy} is an allocation policy which allocates fixed quotas to specified resource consumers.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FixedAllocationPolicy implements IAllocationPolicy
{
    private final List<Pair<String, Long>> quotas;
    private final IAllocationPolicy otherPolicy;

    /**
     * Creates an object.
     *
     * @param quotas fixed quotas as list of pairs segment:quota. Allocation is performed in order of elements of list.
     * If consumer specified in quota is not found in list of actual consumers its quota is not preserved 
     * @param otherPolicy allocation policy for those consumers which are not enlisted in quotas
     */
    public FixedAllocationPolicy(List<Pair<String, Long>> quotas, IAllocationPolicy otherPolicy)
    {
        Assert.notNull(quotas);
        Assert.notNull(otherPolicy);
        
        this.quotas = quotas;
        this.otherPolicy = otherPolicy;
    }

    @Override
    public void allocate(long amount, Map<String, IResourceConsumer> consumers)
    {
        consumers = new LinkedHashMap<String, IResourceConsumer>(consumers);
        for (Pair<String, Long> quota : quotas)
        {
            IResourceConsumer consumer = consumers.get(quota.getKey());
            if (consumer == null)
                continue;
            
            long value = quota.getValue();
            if (amount > value)
            {
                amount -= value;
                consumer.setQuota(value);
            }
            else
            {
                consumer.setQuota(amount);
                amount = 0;
            }
            
            consumers.remove(quota.getKey());
        }

        if (!consumers.isEmpty())
            otherPolicy.allocate(amount, consumers);
    }
}
