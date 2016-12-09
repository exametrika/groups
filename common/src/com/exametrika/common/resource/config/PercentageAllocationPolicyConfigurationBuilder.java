/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;





/**
 * The {@link PercentageAllocationPolicyConfigurationBuilder} is a builder of configuration of percentage allocation policy.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class PercentageAllocationPolicyConfigurationBuilder
{
    private final List<Pair<String, Double>> quotas = new ArrayList<Pair<String, Double>>();
    private AllocationPolicyConfiguration otherPolicy = new UniformAllocationPolicyConfiguration();
    
    public PercentageAllocationPolicyConfigurationBuilder addQuota(String segment, double percent)
    {
        Assert.notNull(segment);
        Assert.isTrue(percent >= 0 && percent <= 100);
        
        quotas.add(new Pair<String, Double>(segment, percent));
        return this;
    }

    public PercentageAllocationPolicyConfigurationBuilder setOtherPolicy(AllocationPolicyConfiguration value)
    {
        Assert.notNull(value);
        
        otherPolicy = value;
        return this;
    }
    
    public PercentageAllocationPolicyConfiguration toConfiguration()
    {
        return new PercentageAllocationPolicyConfiguration(new ArrayList<Pair<String,Double>>(quotas), otherPolicy);
    }
}
