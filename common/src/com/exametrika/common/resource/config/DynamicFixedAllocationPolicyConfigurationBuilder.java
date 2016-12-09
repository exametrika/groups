/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;






/**
 * The {@link DynamicFixedAllocationPolicyConfigurationBuilder} is a builder of configuration of dynamic fixed allocation policy.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class DynamicFixedAllocationPolicyConfigurationBuilder extends 
    DynamicAllocationPolicyConfigurationBuilder<DynamicFixedAllocationPolicyConfigurationBuilder>
{
    private final List<Pair<String, Long>> quotas = new ArrayList<Pair<String, Long>>();
    private AllocationPolicyConfiguration otherPolicy = new DynamicUniformAllocationPolicyConfigurationBuilder().toConfiguration();
    
    public DynamicFixedAllocationPolicyConfigurationBuilder addQuota(String segment, long quota)
    {
        Assert.notNull(segment);
        
        quotas.add(new Pair<String, Long>(segment, quota));
        return this;
    }

    public DynamicFixedAllocationPolicyConfigurationBuilder setOtherPolicy(AllocationPolicyConfiguration value)
    {
        Assert.notNull(value);
        
        otherPolicy = value;
        return this;
    }

    public DynamicFixedAllocationPolicyConfiguration toConfiguration()
    {
        return new DynamicFixedAllocationPolicyConfiguration(new ArrayList<Pair<String,Long>>(quotas), otherPolicy, underloadedThresholdPercentage, 
            overloadedThresholdPercentage, underloadedReservePercentage, overloadedReservePercentage, minQuota);
    }
}
