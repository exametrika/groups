/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.util.Map;

import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.utils.Assert;

/**
 * The {@link FloatingAllocationPolicy} is an allocation policy which allocates to all consumers except floating consumer
 * their actual amount of resource consumption plus some reserve specified as percent of actual amount. Floating consumer 
 * gets the rest of available resource amount.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FloatingAllocationPolicy implements IAllocationPolicy
{
    private final String floatingSegment;
    private final double reservePercentage;
    private final long minQuota;

    /**
     * Creates an object.
     *
     * @param floatingSegment segment of floating consumer
     * @param reservePercentage percent of actual amount of resource consumption reserved to non-floating consumer
     * @param minQuota minimum quota allocated to each non-floating consumer (if amount of available resource allows it).
     * Minimum quota must be larger that minimum allocation unit of resource consumer
     */
    public FloatingAllocationPolicy(String floatingSegment, double reservePercentage, long minQuota)
    {
        Assert.notNull(floatingSegment);
        
        Assert.isTrue(reservePercentage >= 0);
        Assert.isTrue(minQuota >= 0);
        
        this.floatingSegment = floatingSegment;
        this.reservePercentage = reservePercentage;
        this.minQuota = minQuota;
    }

    @Override
    public void allocate(long amount, Map<String, IResourceConsumer> consumers)
    {
        IResourceConsumer floatingConsumer = null;
        for (Map.Entry<String, IResourceConsumer> entry : consumers.entrySet())
        {
            IResourceConsumer consumer = entry.getValue();
            
            if (entry.getKey().equals(floatingSegment))
                floatingConsumer = consumer;
            else
            {
                long value = (long)(consumer.getAmount() * (1 + reservePercentage / 100));
                if (value < minQuota)
                    value = minQuota;
                
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
            }
        }
        
        if (floatingConsumer != null)
            floatingConsumer.setQuota(amount);
    }
}
