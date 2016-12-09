/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.LinkedHashMap;

import com.exametrika.common.utils.Assert;






/**
 * The {@link RootResourceAllocatorConfigurationBuilder} is a builder of configuration of root resource allocator.
 *
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class RootResourceAllocatorConfigurationBuilder extends ResourceAllocatorConfigurationBuilder<RootResourceAllocatorConfigurationBuilder>
{
    private ResourceProviderConfiguration resourceProvider = new PercentageResourceProviderConfiguration(new MemoryResourceProviderConfiguration(true), 30);
    private long timerPeriod = 100;
    private long allocationPeriod = 3000;
    
    public RootResourceAllocatorConfigurationBuilder()
    {
    }

    public RootResourceAllocatorConfigurationBuilder(long amount)
    {
        resourceProvider = new FixedResourceProviderConfiguration(amount);
    }

    public RootResourceAllocatorConfigurationBuilder setResourceProvider(ResourceProviderConfiguration resourceProvider)
    {
        Assert.notNull(resourceProvider);
        
        this.resourceProvider = resourceProvider;
        return this;
    }
    
    public RootResourceAllocatorConfigurationBuilder setTimerPeriod(long value)
    {
        this.timerPeriod = value;
        return this;
    }
    
    public RootResourceAllocatorConfigurationBuilder setAllocationPeriod(long value)
    {
        this.allocationPeriod = value;
        return this;
    }
    
    public RootResourceAllocatorConfiguration toConfiguration()
    {
        return new RootResourceAllocatorConfiguration(name, resourceProvider, timerPeriod, allocationPeriod, 
            new LinkedHashMap<String, AllocationPolicyConfiguration>(policies), defaultPolicy, quotaIncreaseDelay, initializePeriod);
    }
}
