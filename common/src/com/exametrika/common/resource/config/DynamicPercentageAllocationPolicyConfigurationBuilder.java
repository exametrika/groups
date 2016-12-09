/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;






/**
 * The {@link DynamicPercentageAllocationPolicyConfigurationBuilder} is a builder of configuration of dynamic percentage allocation policy.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class DynamicPercentageAllocationPolicyConfigurationBuilder extends 
    DynamicAllocationPolicyConfigurationBuilder<DynamicPercentageAllocationPolicyConfigurationBuilder>
{
    private final List<Pair<String, Double>> quotas = new ArrayList<Pair<String, Double>>();
    private AllocationPolicyConfiguration otherPolicy = new DynamicUniformAllocationPolicyConfigurationBuilder().toConfiguration();
    
    public DynamicPercentageAllocationPolicyConfigurationBuilder addQuota(String segment, double percentage)
    {
        Assert.notNull(segment);
        Assert.isTrue(percentage >= 0 && percentage <= 100);
        
        quotas.add(new Pair<String, Double>(segment, percentage));
        return this;
    }

    public DynamicPercentageAllocationPolicyConfigurationBuilder setOtherPolicy(AllocationPolicyConfiguration value)
    {
        Assert.notNull(value);
        
        otherPolicy = value;
        return this;
    }

    public DynamicPercentageAllocationPolicyConfiguration toConfiguration()
    {
        return new DynamicPercentageAllocationPolicyConfiguration(new ArrayList<Pair<String,Double>>(quotas), otherPolicy, underloadedThresholdPercentage, 
            overloadedThresholdPercentage, underloadedReservePercentage, overloadedReservePercentage, minQuota);
    }
}
