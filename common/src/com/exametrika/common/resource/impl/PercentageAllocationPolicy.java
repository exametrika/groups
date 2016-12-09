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
 * The {@link PercentageAllocationPolicy} is an allocation policy which allocates relative quotas set in percents to specified resource consumers.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class PercentageAllocationPolicy implements IAllocationPolicy
{
    private final List<Pair<String, Double>> quotas;
    private final IAllocationPolicy otherPolicy;

    /**
     * Creates an object.
     *
     * @param quotas relative quotas as list of pairs segment:percent of available amount. Allocation is performed in order of elements of list.
     * If consumer specified in quota is not found in list of actual consumers its quota is not preserved 
     * @param otherPolicy allocation policy for those consumers which are not enlisted in quotas
     */
    public PercentageAllocationPolicy(List<Pair<String, Double>> quotas, IAllocationPolicy otherPolicy)
    {
        Assert.notNull(quotas);
        Assert.notNull(otherPolicy);
        
        double sum = 0;
        for (Pair<String, Double> quota : quotas)
        {
            Assert.isTrue(quota.getValue() >= 0 && quota.getValue() <= 100);
            sum += quota.getValue();
        }
        
        Assert.isTrue(sum <= 100);
        
        this.quotas = quotas;
        this.otherPolicy = otherPolicy;
    }

    @Override
    public void allocate(long amount, Map<String, IResourceConsumer> consumers)
    {
        long baseAmount = amount;
        consumers = new LinkedHashMap<String, IResourceConsumer>(consumers);
        for (Pair<String, Double> quota : quotas)
        {
            IResourceConsumer consumer = consumers.get(quota.getKey());
            if (consumer == null)
                continue;
            
            long value = (long)(baseAmount * quota.getValue() / 100);
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
