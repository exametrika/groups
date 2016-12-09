/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.impl.ThresholdAllocationPolicy;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Pair;





/**
 * The {@link ThresholdAllocationPolicyConfiguration} is a configuration of threshold allocation policy.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ThresholdAllocationPolicyConfiguration extends AllocationPolicyConfiguration
{
    private final List<Pair<Long, AllocationPolicyConfiguration>> thresholds;
    
    public ThresholdAllocationPolicyConfiguration(List<Pair<Long, AllocationPolicyConfiguration>> thresholds)
    {
        Assert.notNull(thresholds);
        Assert.isTrue(!thresholds.isEmpty());
        
        this.thresholds = Immutables.wrap(thresholds);
    }

    public List<Pair<Long, AllocationPolicyConfiguration>> getThresholds()
    {
        return thresholds;
    }

    @Override
    public ThresholdAllocationPolicy createPolicy()
    {
        List<Pair<Long, IAllocationPolicy>> thresholds = new ArrayList<Pair<Long, IAllocationPolicy>>();
        for (Pair<Long, AllocationPolicyConfiguration> threshold : this.thresholds)
            thresholds.add(new Pair<Long, IAllocationPolicy>(threshold.getKey(), threshold.getValue().createPolicy()));
        
        return new ThresholdAllocationPolicy(thresholds);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof ThresholdAllocationPolicyConfiguration))
            return false;
        
        ThresholdAllocationPolicyConfiguration configuration = (ThresholdAllocationPolicyConfiguration)o;
        return thresholds.equals(configuration.thresholds);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(thresholds);
    }
}
