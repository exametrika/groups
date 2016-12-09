/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;





/**
 * The {@link ThresholdAllocationPolicyConfigurationBuilder} is a builder of configuration of threshold allocation policy.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class ThresholdAllocationPolicyConfigurationBuilder
{
    private final List<Pair<Long, AllocationPolicyConfiguration>> thresholds = new ArrayList<Pair<Long,AllocationPolicyConfiguration>>();
    
    public ThresholdAllocationPolicyConfigurationBuilder addThreshold(long threshold, AllocationPolicyConfiguration allocationPolicy)
    {
        Assert.notNull(allocationPolicy);
        
        thresholds.add(new Pair<Long, AllocationPolicyConfiguration>(threshold, allocationPolicy));
        return this;
    }

    public ThresholdAllocationPolicyConfiguration toConfiguration()
    {
        return new ThresholdAllocationPolicyConfiguration(new ArrayList<Pair<Long,AllocationPolicyConfiguration>>(thresholds));
    }
}
