/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;






/**
 * The {@link DynamicAllocationPolicyConfigurationBuilder} is a builder of abstract configuration of dynamic allocation policy.
 * 
 * @param <T> builder type
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public abstract class DynamicAllocationPolicyConfigurationBuilder<T>
{
    protected double underloadedThresholdPercentage = 40;
    protected double overloadedThresholdPercentage = 90;
    protected double underloadedReservePercentage = 100;
    protected double overloadedReservePercentage = 10;
    protected long minQuota = 10000000;
    
    public T setUnderloadedThresholdPercentage(double underloadedThresholdPercent)
    {
        this.underloadedThresholdPercentage = underloadedThresholdPercent;
        return (T)this;
    }
    
    public T setOverloadedThresholdPercentage(double overloadedThresholdPercent)
    {
        this.overloadedThresholdPercentage = overloadedThresholdPercent;
        return (T)this;
    }
    
    public T setUnderloadedReservePercentage(double reservePercent)
    {
        this.underloadedReservePercentage = reservePercent;
        return (T)this;
    }
    
    public T setOverloadedReservePercentage(double reservePercent)
    {
        this.overloadedReservePercentage = reservePercent;
        return (T)this;
    }
    
    public T setMinQuota(long minQuota)
    {
        this.minQuota = minQuota;
        return (T)this;
    }
}
