/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.utils.Assert;

/**
 * The {@link DynamicAllocationPolicy} is an allocation policy which has ability to reallocate
 * unused resources of underloaded consumers to overloaded consumers in fair manner, rapidly restoring fair share
 * of resource in underloaded consumer when its consumption grows.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class DynamicAllocationPolicy implements IAllocationPolicy
{
    private final double underloadedThresholdPercentage;
    private final double overloadedThresholdPercentage;
    private final double underloadedReservePercentage;
    private final double overloadedReservePercentage;
    private final long minQuota;

    /**
     * Creates an object.
     *
     * @param underloadedThresholdPercentage percent of consumer quota which defines a threshold of actual resource consumption
     * when consumer is considered as underloaded
     * @param overloadedThresholdPercentage percent of consumer quota which defines a threshold of actual resource consumption
     * when consumer is considered as overloaded
     * @param underloadedReservePercentage percent of actual amount of resource consumption reserved to underloaded consumer
     * @param overloadedReservePercentage percent of actual amount of resource consumption reserved to overloaded consumer
     * @param minQuota minimum quota allocated to each consumer (if amount of available resource allows it). Minimum
     * quota must be larger that minimum allocation unit of resource consumer
     */
    public DynamicAllocationPolicy(double underloadedThresholdPercentage, double overloadedThresholdPercentage, 
        double underloadedReservePercentage, double overloadedReservePercentage, long minQuota)
    {
        Assert.isTrue(underloadedThresholdPercentage >= 0 && underloadedThresholdPercentage <= 100);
        Assert.isTrue(overloadedThresholdPercentage >= 0 && overloadedThresholdPercentage <= 100);
        Assert.isTrue(underloadedReservePercentage >= 0);
        Assert.isTrue(overloadedReservePercentage >= 0);
        
        this.underloadedThresholdPercentage = underloadedThresholdPercentage;
        this.overloadedThresholdPercentage = overloadedThresholdPercentage;
        this.underloadedReservePercentage = underloadedReservePercentage;
        this.overloadedReservePercentage = overloadedReservePercentage;
        this.minQuota = minQuota;
    }
    
    @Override
    public void allocate(long value, Map<String, IResourceConsumer> consumers)
    {
        if (consumers.isEmpty())
            return;
        
        consumers = orderConsumers(consumers);
        allocateResources(value, consumers);
    }
    
    protected abstract Map<String, IResourceConsumer> orderConsumers(Map<String, IResourceConsumer> consumers);
    protected abstract long getBaseQuota(long uniformBaseQuota, long baseAmount, long amount, String segment);

    private void allocateResources(long amount, Map<String, IResourceConsumer> consumers)
    {
        long baseAmount = amount;
        long uniformBaseQuota = amount / consumers.size();
        long uniformDelta = 0;
        boolean overloaded = false;
        List<Long> quotas = new ArrayList<Long>();
        
        while (true)
        {
            Map<String, IResourceConsumer> overloadedConsumers = new LinkedHashMap<String, IResourceConsumer>();
            List<Long> overloadedQuotas = new ArrayList<Long>();
            long overloadedAmount = 0;
            
            int i = 0;
            for (Map.Entry<String, IResourceConsumer> entry : consumers.entrySet())
            {
                long baseQuota;
                if (!overloaded)
                    baseQuota = getBaseQuota(uniformBaseQuota, baseAmount, amount, entry.getKey());
                else
                    baseQuota = quotas.get(i++) + uniformDelta;
                
                if (amount >= minQuota && baseQuota < minQuota)
                    baseQuota = minQuota;
                
                IResourceConsumer consumer = entry.getValue();
                
                long consumerAmount = consumer.getAmount();
                if (consumerAmount >= baseQuota * overloadedThresholdPercentage / 100)
                {
                    overloadedConsumers.put(entry.getKey(), consumer);
                    amount -= baseQuota;
                    overloadedAmount += baseQuota;
                    overloadedQuotas.add(baseQuota);
                }
                else if (!overloaded)
                {
                    if (consumerAmount < baseQuota * underloadedThresholdPercentage / 100)
                    {
                        long quota = (long)(consumerAmount * (1 + underloadedReservePercentage / 100));
                        if (quota < minQuota)
                            quota = minQuota;
                        if (quota > baseQuota)
                            quota = baseQuota;
                        
                        consumer.setQuota(quota);
                        amount -= quota;
                    }
                    else
                    {
                        consumer.setQuota(baseQuota);
                        amount -= baseQuota;
                    }
                }
                else
                {
                    long quota = (long)(consumerAmount * (1 + overloadedReservePercentage / 100));
                    if (quota < minQuota)
                        quota = minQuota;
                    if (quota > baseQuota)
                        quota = baseQuota;
                    
                    consumer.setQuota(quota);
                    amount -= quota;
                }
            }
            
            if (overloadedConsumers.size() == consumers.size())
            {
                i = 0;
                for (IResourceConsumer consumer : consumers.values())
                    consumer.setQuota(overloadedQuotas.get(i++));
                
                return;
            }
            else if (!overloadedConsumers.isEmpty())
            {
                uniformDelta = amount / overloadedConsumers.size();
                amount += overloadedAmount;
                overloaded = true;
                consumers = overloadedConsumers;
                quotas = overloadedQuotas;
            }
            else
                return;
        }
    }
}
