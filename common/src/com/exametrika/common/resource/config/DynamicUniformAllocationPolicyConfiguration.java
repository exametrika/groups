/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import com.exametrika.common.resource.impl.DynamicUniformAllocationPolicy;





/**
 * The {@link DynamicUniformAllocationPolicyConfiguration} is a configuration of dynamic uniform allocation policy.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DynamicUniformAllocationPolicyConfiguration extends DynamicAllocationPolicyConfiguration
{
    public DynamicUniformAllocationPolicyConfiguration(double underloadedThresholdPercent, double overloadedThresholdPercent, 
        double underloadedReservePercent, double overloadedReservePercent, long minQuota)
    {
        super(underloadedThresholdPercent, overloadedThresholdPercent, underloadedReservePercent, overloadedReservePercent, minQuota);
    }

    @Override
    public DynamicUniformAllocationPolicy createPolicy()
    {
        return new DynamicUniformAllocationPolicy(underloadedThresholdPercentage, overloadedThresholdPercentage, 
            underloadedReservePercentage, overloadedReservePercentage, minQuota);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof DynamicUniformAllocationPolicyConfiguration))
            return false;
        
        return super.equals(o);
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode();
    }
}
