/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.List;

import com.exametrika.common.resource.impl.DynamicFixedAllocationPolicy;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Pair;





/**
 * The {@link DynamicFixedAllocationPolicyConfiguration} is a configuration of dynamic fixed allocation policy.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DynamicFixedAllocationPolicyConfiguration extends DynamicAllocationPolicyConfiguration
{
    private final List<Pair<String, Long>> quotas;
    private final AllocationPolicyConfiguration otherPolicy;
    
    public DynamicFixedAllocationPolicyConfiguration(List<Pair<String, Long>> quotas, AllocationPolicyConfiguration otherPolicy,
        double underloadedThresholdPercentage, double overloadedThresholdPercentage, 
        double underloadedReservePercentage, double overloadedReservePercentage, long minQuota)
    {
        super(underloadedThresholdPercentage, overloadedThresholdPercentage, underloadedReservePercentage, overloadedReservePercentage, minQuota);
        
        Assert.notNull(otherPolicy);
        
        this.quotas = Immutables.wrap(quotas);
        this.otherPolicy = otherPolicy;
    }
    
    public List<Pair<String, Long>> getQuotas()
    {
        return quotas;
    }

    public AllocationPolicyConfiguration getOtherPolicy()
    {
        return otherPolicy;
    }

    @Override
    public DynamicFixedAllocationPolicy createPolicy()
    {
        return new DynamicFixedAllocationPolicy(quotas, otherPolicy.createPolicy(), underloadedThresholdPercentage, overloadedThresholdPercentage, 
            underloadedReservePercentage, overloadedReservePercentage, minQuota);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof DynamicFixedAllocationPolicyConfiguration))
            return false;
        
        DynamicFixedAllocationPolicyConfiguration configuration = (DynamicFixedAllocationPolicyConfiguration)o;
        return super.equals(o) && quotas.equals(configuration.quotas) && otherPolicy.equals(configuration.otherPolicy);
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hashCode(quotas, otherPolicy);
    }
}
