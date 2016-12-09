/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import com.exametrika.common.resource.impl.LimitingAllocationPolicy;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;





/**
 * The {@link LimitingAllocationPolicyConfiguration} is a configuration of limiting allocation policy.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class LimitingAllocationPolicyConfiguration extends AllocationPolicyConfiguration
{
    private final AllocationPolicyConfiguration basePolicy;
    private final double limitPercentage;
    
    public LimitingAllocationPolicyConfiguration(AllocationPolicyConfiguration basePolicy, double limitPercentage)
    {
        Assert.notNull(basePolicy);
        Assert.isTrue(limitPercentage >= 0 && limitPercentage <= 100);
        
        this.basePolicy = basePolicy;
        this.limitPercentage = limitPercentage;
    }
    
    public AllocationPolicyConfiguration getBasePolicy()
    {
        return basePolicy;
    }
    
    public double getLimitPercentage()
    {
        return limitPercentage;
    }

    @Override
    public LimitingAllocationPolicy createPolicy()
    {
        return new LimitingAllocationPolicy(basePolicy.createPolicy(), limitPercentage);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof LimitingAllocationPolicyConfiguration))
            return false;
        
        LimitingAllocationPolicyConfiguration configuration = (LimitingAllocationPolicyConfiguration)o;
        return basePolicy.equals(configuration.basePolicy) && limitPercentage == configuration.limitPercentage;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(basePolicy, limitPercentage);
    }
}
