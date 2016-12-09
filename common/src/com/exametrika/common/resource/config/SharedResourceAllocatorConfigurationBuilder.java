/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.LinkedHashMap;

import com.exametrika.common.utils.Assert;






/**
 * The {@link SharedResourceAllocatorConfigurationBuilder} is a builder of configuration of shared resource allocator.
 *
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class SharedResourceAllocatorConfigurationBuilder extends ResourceAllocatorConfigurationBuilder<SharedResourceAllocatorConfigurationBuilder>
{
    private ResourceProviderConfiguration resourceProvider = new FixedResourceProviderConfiguration(10000000);
    private long timerPeriod = 100;
    private long allocationPeriod = 3000;
    private String dataExchangeFileName;
    private long dataExchangePeriod = 500;
    private long staleAllocatorPeriod = 10000;
    private long initialQuota = 10000000;
    
    public SharedResourceAllocatorConfigurationBuilder setResourceProvider(ResourceProviderConfiguration resourceProvider)
    {
        Assert.notNull(resourceProvider);
        
        this.resourceProvider = resourceProvider;
        return this;
    }
    
    public SharedResourceAllocatorConfigurationBuilder setTimerPeriod(long value)
    {
        this.timerPeriod = value;
        return this;
    }
    
    public SharedResourceAllocatorConfigurationBuilder setAllocationPeriod(long value)
    {
        this.allocationPeriod = value;
        return this;
    }

    public SharedResourceAllocatorConfigurationBuilder setDataExchangeFileName(String dataExchangeFileName)
    {
        Assert.notNull(dataExchangeFileName);
        
        this.dataExchangeFileName = dataExchangeFileName;
        return this;
    }

    public SharedResourceAllocatorConfigurationBuilder setDataExchangePeriod(long dataExchangePeriod)
    {
        this.dataExchangePeriod = dataExchangePeriod;
        return this;
    }

    public SharedResourceAllocatorConfigurationBuilder setStaleAllocatorPeriod(long staleAllocatorPeriod)
    {
        this.staleAllocatorPeriod = staleAllocatorPeriod;
        return this;
    }

    public SharedResourceAllocatorConfigurationBuilder setInitialQuota(long initialQuota)
    {
        this.initialQuota = initialQuota;
        return this;
    }

    public SharedResourceAllocatorConfiguration toConfiguration()
    {
        return new SharedResourceAllocatorConfiguration(dataExchangeFileName, allocationPeriod, dataExchangePeriod, staleAllocatorPeriod, 
            initialQuota, name, resourceProvider, timerPeriod, new LinkedHashMap<String, AllocationPolicyConfiguration>(policies), defaultPolicy, quotaIncreaseDelay, initializePeriod);
    }
}
