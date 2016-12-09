/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import com.exametrika.common.resource.impl.FloatingAllocationPolicy;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;





/**
 * The {@link FloatingAllocationPolicyConfiguration} is a configuration of floating allocation policy.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FloatingAllocationPolicyConfiguration extends AllocationPolicyConfiguration
{
    private final String floatingSegment;
    private final double reservePercentage;
    private final long minQuota;
    
    public FloatingAllocationPolicyConfiguration(String floatingSegment, double reservePercentage, long minQuota)
    {
        Assert.notNull(floatingSegment);
        
        Assert.isTrue(reservePercentage >= 0 && reservePercentage <= 100);
        Assert.isTrue(minQuota >= 0);
        
        this.floatingSegment = floatingSegment;
        this.reservePercentage = reservePercentage;
        this.minQuota = minQuota;
    }

    public String getFloatingSegment()
    {
        return floatingSegment;
    }

    public double getReservePercentage()
    {
        return reservePercentage;
    }

    public long getMinQuota()
    {
        return minQuota;
    }
    
    @Override
    public FloatingAllocationPolicy createPolicy()
    {
        return new FloatingAllocationPolicy(floatingSegment, reservePercentage, minQuota);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof FloatingAllocationPolicyConfiguration))
            return false;
        FloatingAllocationPolicyConfiguration configuration = (FloatingAllocationPolicyConfiguration)o;
        return floatingSegment.equals(configuration.floatingSegment) && reservePercentage == configuration.reservePercentage &&
            minQuota == configuration.minQuota;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(floatingSegment, reservePercentage, minQuota);
    }
}
