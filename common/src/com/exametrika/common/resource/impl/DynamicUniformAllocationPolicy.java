/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.util.Map;

import com.exametrika.common.resource.IResourceConsumer;

/**
 * The {@link DynamicUniformAllocationPolicy} is an dynamic allocation policy which allocates equal amount of resource between all consumers.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DynamicUniformAllocationPolicy extends DynamicAllocationPolicy
{
    public DynamicUniformAllocationPolicy(double underloadedThresholdPercentage, double overloadedThresholdPercentage, 
        double underloadedReservePercentage, double overloadedReservePercentage, long minQuota)
    {
        super(underloadedThresholdPercentage, overloadedThresholdPercentage, underloadedReservePercentage, overloadedReservePercentage, minQuota);
    }

    @Override
    protected Map<String, IResourceConsumer> orderConsumers(Map<String, IResourceConsumer> consumers)
    {
        return consumers;
    }

    @Override
    protected long getBaseQuota(long uniformBaseQuota, long baseAmount, long amountRest, String segment)
    {
        return uniformBaseQuota;
    }
}
