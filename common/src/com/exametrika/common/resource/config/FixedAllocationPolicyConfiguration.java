/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.List;

import com.exametrika.common.resource.impl.FixedAllocationPolicy;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Pair;





/**
 * The {@link FixedAllocationPolicyConfiguration} is a configuration of fixed allocation policy.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FixedAllocationPolicyConfiguration extends AllocationPolicyConfiguration
{
    private final List<Pair<String, Long>> quotas;
    private final AllocationPolicyConfiguration otherPolicy;
    
    public FixedAllocationPolicyConfiguration(List<Pair<String, Long>> quotas, AllocationPolicyConfiguration otherPolicy)
    {
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
    public FixedAllocationPolicy createPolicy()
    {
        return new FixedAllocationPolicy(quotas, otherPolicy.createPolicy());
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof FixedAllocationPolicyConfiguration))
            return false;
        
        FixedAllocationPolicyConfiguration configuration = (FixedAllocationPolicyConfiguration)o;
        return quotas.equals(configuration.quotas) && otherPolicy.equals(configuration.otherPolicy);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(quotas, otherPolicy);
    }
}
