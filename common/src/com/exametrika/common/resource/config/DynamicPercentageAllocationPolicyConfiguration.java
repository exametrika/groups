/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.List;

import com.exametrika.common.resource.impl.DynamicPercentageAllocationPolicy;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Pair;





/**
 * The {@link DynamicPercentageAllocationPolicyConfiguration} is a configuration of dynamic percentage allocation policy.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DynamicPercentageAllocationPolicyConfiguration extends DynamicAllocationPolicyConfiguration
{
    private final List<Pair<String, Double>> quotas;
    private final AllocationPolicyConfiguration otherPolicy;
    
    public DynamicPercentageAllocationPolicyConfiguration(List<Pair<String, Double>> quotas, AllocationPolicyConfiguration otherPolicy,
        double underloadedThresholdPercentage, double overloadedThresholdPercentage, 
        double underloadedReservePercentage, double overloadedReservePercentage, long minQuota)
    {
        super(underloadedThresholdPercentage, overloadedThresholdPercentage, underloadedReservePercentage, overloadedReservePercentage, minQuota);
        
        Assert.notNull(otherPolicy);
        
        this.quotas = Immutables.wrap(quotas);
        this.otherPolicy = otherPolicy;
    }
    
    public List<Pair<String, Double>> getQuotas()
    {
        return quotas;
    }

    public AllocationPolicyConfiguration getOtherPolicy()
    {
        return otherPolicy;
    }

    @Override
    public DynamicPercentageAllocationPolicy createPolicy()
    {
        return new DynamicPercentageAllocationPolicy(quotas, otherPolicy.createPolicy(), underloadedThresholdPercentage, overloadedThresholdPercentage, 
            underloadedReservePercentage, overloadedReservePercentage, minQuota);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof DynamicPercentageAllocationPolicyConfiguration))
            return false;
        
        DynamicPercentageAllocationPolicyConfiguration configuration = (DynamicPercentageAllocationPolicyConfiguration)o;
        return super.equals(o) && quotas.equals(configuration.quotas) && otherPolicy.equals(configuration.otherPolicy);
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hashCode(quotas, otherPolicy);
    }
}
