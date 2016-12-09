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
 * The {@link DynamicFixedAllocationPolicy} is an dynamic allocation policy which allocates fixed quotas to specified resource consumers.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DynamicFixedAllocationPolicy extends DynamicAllocationPolicy
{
    private final List<Pair<String, Long>> quotas;
    private final Map<String, Long> quotasMap;
    private final IAllocationPolicy otherPolicy;

    /**
     * Creates an object.
     *
     * @param quotas fixed quotas as list of pairs segment:quota. Allocation is performed in order of elements of list.
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
    public DynamicFixedAllocationPolicy(List<Pair<String, Long>> quotas, IAllocationPolicy otherPolicy,
        double underloadedThresholdPercentage, double overloadedThresholdPercentage, 
        double underloadedReservePercentage, double overloadedReservePercentage, long minQuota)
    {
        super(underloadedThresholdPercentage, overloadedThresholdPercentage, underloadedReservePercentage, overloadedReservePercentage, minQuota);
        
        this.quotas = quotas;
        this.otherPolicy = otherPolicy;
        
        Map<String, Long> quotasMap = new HashMap<String, Long>();
        for (Pair<String, Long> pair : quotas)
            Assert.isNull(quotasMap.put(pair.getKey(), pair.getValue()));
        
        this.quotasMap = quotasMap;
    }

    @Override
    protected Map<String, IResourceConsumer> orderConsumers(Map<String, IResourceConsumer> consumers)
    {
        consumers = new LinkedHashMap<String, IResourceConsumer>(consumers);
        Map<String, IResourceConsumer> orderedConsumers = new LinkedHashMap<String, IResourceConsumer>();
        for (Pair<String, Long> pair : quotas)
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
        
        long quota = quotasMap.get(segment);
        if (quota < amount)
            return quota;
        else
            return amount;
    }
}
