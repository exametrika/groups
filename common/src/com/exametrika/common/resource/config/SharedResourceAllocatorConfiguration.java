/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.impl.SharedResourceAllocator;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;





/**
 * The {@link SharedResourceAllocatorConfiguration} is a configuration of shared resource allocator.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SharedResourceAllocatorConfiguration extends RootResourceAllocatorConfiguration
{
    private final String dataExchangeFileName;
    private final long dataExchangePeriod;
    private final long staleAllocatorPeriod;
    private final long initialQuota;

    public SharedResourceAllocatorConfiguration(String dataExchangeFileName, long allocationPeriod, 
        long dataExchangePeriod, long staleAllocatorPeriod, long initialQuota,
        String name, ResourceProviderConfiguration resourceProvider, long timerPeriod,
        Map<String, AllocationPolicyConfiguration> policies, AllocationPolicyConfiguration defaultPolicy, 
        long quotaIncreaseDelay, long initializePeriod)
    {
        super(name, resourceProvider, timerPeriod, allocationPeriod, policies, defaultPolicy, quotaIncreaseDelay, initializePeriod);
        
        Assert.notNull(dataExchangeFileName);
        
        this.dataExchangeFileName = dataExchangeFileName;
        this.dataExchangePeriod = dataExchangePeriod;
        this.staleAllocatorPeriod = staleAllocatorPeriod;
        this.initialQuota = initialQuota;
    }
    
    public String getDataExchangeFileName()
    {
        return dataExchangeFileName;
    }

    public long getDataExchangePeriod()
    {
        return dataExchangePeriod;
    }

    public long getStaleAllocatorPeriod()
    {
        return staleAllocatorPeriod;
    }

    public long getInitialQuota()
    {
        return initialQuota;
    }

    @Override
    public SharedResourceAllocator createAllocator()
    {
        Map<String, IAllocationPolicy> policies = new LinkedHashMap<String, IAllocationPolicy>(this.policies.size());
        for (Map.Entry<String, AllocationPolicyConfiguration> entry : this.policies.entrySet())
            policies.put(entry.getKey(), entry.getValue().createPolicy());
        
        return new SharedResourceAllocator(dataExchangeFileName, name, resourceProvider.createProvider(), policies, 
            defaultPolicy.createPolicy(), timerPeriod, allocationPeriod, dataExchangePeriod, staleAllocatorPeriod, 
            initialQuota, quotaIncreaseDelay, initializePeriod, new SystemTimeService());
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof SharedResourceAllocatorConfiguration))
            return false;
        
        SharedResourceAllocatorConfiguration configuration = (SharedResourceAllocatorConfiguration)o;
        return super.equals(o) && dataExchangeFileName.equals(configuration.dataExchangeFileName) && 
            dataExchangePeriod == configuration.dataExchangePeriod &&
            staleAllocatorPeriod == configuration.staleAllocatorPeriod && initialQuota == configuration.initialQuota;
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hashCode(dataExchangeFileName, dataExchangePeriod,
            staleAllocatorPeriod, initialQuota);
    }
}
