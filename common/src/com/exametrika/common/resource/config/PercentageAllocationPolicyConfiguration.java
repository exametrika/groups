/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.List;

import com.exametrika.common.resource.impl.PercentageAllocationPolicy;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Pair;





/**
 * The {@link PercentageAllocationPolicyConfiguration} is a configuration of percentage allocation policy.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class PercentageAllocationPolicyConfiguration extends AllocationPolicyConfiguration
{
    private final List<Pair<String, Double>> quotas;
    private final AllocationPolicyConfiguration otherPolicy;
    
    public PercentageAllocationPolicyConfiguration(List<Pair<String, Double>> quotas, AllocationPolicyConfiguration otherPolicy)
    {
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
    public PercentageAllocationPolicy createPolicy()
    {
        return new PercentageAllocationPolicy(quotas, otherPolicy.createPolicy());
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof PercentageAllocationPolicyConfiguration))
            return false;
        
        PercentageAllocationPolicyConfiguration configuration = (PercentageAllocationPolicyConfiguration)o;
        return quotas.equals(configuration.quotas) && otherPolicy.equals(configuration.otherPolicy);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(quotas, otherPolicy);
    }
}
