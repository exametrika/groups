/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;

/**
 * The {@link DynamicPercentageAllocationPolicy} is an dynamic allocation policy which allocates relative quotas set in percents to specified resource consumers.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DynamicPercentageAllocationPolicy extends DynamicAllocationPolicy
{
    private final List<Pair<String, Double>> quotas;
    private final Map<String, Double> quotasMap;
    private final IAllocationPolicy otherPolicy;
    
    /**
     * Creates an object.
     *
     * @param quotas relative quotas as list of pairs segment:percent of available amount. Allocation is performed in order of elements of list.
     * If consumer specified in quota is not found in list of actual consumers its quota is not preserved 
     * @param otherPolicy allocation policy for those consumers which are not enlisted in quotas
     * @param underloadedThresholdPercentage percent of consumer quota which defines a threshold of actual resource consumption
     * when consumer is considered as underloaded
     * @param overloadedThresholdPercentage percent of consumer quota which defines a threshold of actual resource consumption
     * when consumer is considered as overloaded
     * @param underloadedReservePercentage percent of actual amount of resource consumption reserved to underloaded consumer
     * @param overloadedReservePercentage percent of actual amount of resource consumption reserved to overloaded consumer
     * @param minQuota minimum quota allocated to each consumer (if amount of available resource allows it). Minimum
     * quota must be larger that minimum allocation unit of resource consumer
     */
    public DynamicPercentageAllocationPolicy(List<Pair<String, Double>> quotas, IAllocationPolicy otherPolicy,
        double underloadedThresholdPercentage, double overloadedThresholdPercentage, 
        double underloadedReservePercentage, double overloadedReservePercentage, long minQuota)
    {
        super(underloadedThresholdPercentage, overloadedThresholdPercentage, underloadedReservePercentage, overloadedReservePercentage, minQuota);
        
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
        
        Map<String, Double> quotasMap = new HashMap<String, Double>();
        for (Pair<String, Double> pair : quotas)
            Assert.isNull(quotasMap.put(pair.getKey(), pair.getValue()));
        
        this.quotasMap = quotasMap;
    }

    @Override
    protected Map<String, IResourceConsumer> orderConsumers(Map<String, IResourceConsumer> consumers)
    {
        consumers = new LinkedHashMap<String, IResourceConsumer>(consumers);
        Map<String, IResourceConsumer> orderedConsumers = new LinkedHashMap<String, IResourceConsumer>();
        for (Pair<String, Double> pair : quotas)
        {
            IResourceConsumer consumer = consumers.get(pair.getKey());
            if (consumer == null)
                continue;
            
            consumers.remove(pair.getKey());
            orderedConsumers.put(pair.getKey(), consumer);
        }
        
        orderedConsumers.put("", new CompositeConsumer(consumers, otherPolicy));
        return orderedConsumers;
    }
    
    @Override
    protected long getBaseQuota(long uniformBaseQuota, long baseAmount, long amount, String segment)
    {
        if (segment.isEmpty())
            return amount;
        
        long quota = (long)(baseAmount * quotasMap.get(segment) / 100);
        if (quota < amount)
            return quota;
        else
            return amount;
    }
}
