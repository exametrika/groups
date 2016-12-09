/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;





/**
 * The {@link DynamicAllocationPolicyConfiguration} is an abstract configuration of dynamic allocation policy.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class DynamicAllocationPolicyConfiguration extends AllocationPolicyConfiguration
{
    protected final double underloadedThresholdPercentage;
    protected final double overloadedThresholdPercentage;
    protected final double underloadedReservePercentage;
    protected final double overloadedReservePercentage;
    protected final long minQuota;

    public DynamicAllocationPolicyConfiguration(double underloadedThresholdPercentage, double overloadedThresholdPercentage, 
        double underloadedReservePercentage, double overloadedReservePercentage, long minQuota)
    {
        Assert.isTrue(underloadedThresholdPercentage >= 0 && underloadedThresholdPercentage <= 100);
        Assert.isTrue(overloadedThresholdPercentage >= 0 && overloadedThresholdPercentage <= 100);
        Assert.isTrue(underloadedReservePercentage >= 0);
        Assert.isTrue(overloadedReservePercentage >= 0);
        Assert.isTrue(minQuota >= 0);
        
        this.underloadedThresholdPercentage = underloadedThresholdPercentage;
        this.overloadedThresholdPercentage = overloadedThresholdPercentage;
        this.underloadedReservePercentage = underloadedReservePercentage;
        this.overloadedReservePercentage = overloadedReservePercentage;
        this.minQuota = minQuota;
    }
    
    public double getUnderloadedThresholdPercentage()
    {
        return underloadedThresholdPercentage;
    }

    public double getOverloadedThresholdPercentage()
    {
        return overloadedThresholdPercentage;
    }

    public double getUnderloadedReservePercentage()
    {
        return underloadedReservePercentage;
    }

    public double getOverloadedReservePercentage()
    {
        return overloadedReservePercentage;
    }
    
    public long getMinQuota()
    {
        return minQuota;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof DynamicAllocationPolicyConfiguration))
            return false;
        
        DynamicAllocationPolicyConfiguration configuration = (DynamicAllocationPolicyConfiguration)o;
        return underloadedThresholdPercentage == configuration.underloadedThresholdPercentage && 
            overloadedThresholdPercentage == configuration.overloadedThresholdPercentage && 
            underloadedReservePercentage == configuration.underloadedReservePercentage && 
            overloadedReservePercentage == configuration.overloadedReservePercentage && minQuota == configuration.minQuota;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(underloadedThresholdPercentage, overloadedThresholdPercentage, 
            underloadedReservePercentage, overloadedReservePercentage, minQuota);
    }
}

