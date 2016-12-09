/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.impl.RootResourceAllocator;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;





/**
 * The {@link RootResourceAllocatorConfiguration} is a configuration of top-level resource allocator.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class RootResourceAllocatorConfiguration extends ResourceAllocatorConfiguration
{
    protected final ResourceProviderConfiguration resourceProvider;
    protected final long timerPeriod;
    protected final long allocationPeriod;

    public RootResourceAllocatorConfiguration(String name, ResourceProviderConfiguration resourceProvider, long timerPeriod,
        long allocationPeriod, Map<String, AllocationPolicyConfiguration> policies, AllocationPolicyConfiguration defaultPolicy, 
        long quotaIncreaseDelay, long initializePeriod)
    {
        super(name, policies, defaultPolicy, quotaIncreaseDelay, initializePeriod);
        
        Assert.notNull(resourceProvider);
        
        this.resourceProvider = resourceProvider;
        this.timerPeriod = timerPeriod;
        this.allocationPeriod = allocationPeriod;
    }
    
    public ResourceProviderConfiguration getResourceProvider()
    {
        return resourceProvider;
    }
    
    public long getTimerPeriod()
    {
        return timerPeriod;
    }
    
    public long getAllocationPeriod()
    {
        return allocationPeriod;
    }
    
    @Override
    public RootResourceAllocator createAllocator()
    {
        Map<String, IAllocationPolicy> policies = new LinkedHashMap<String, IAllocationPolicy>(this.policies.size());
        for (Map.Entry<String, AllocationPolicyConfiguration> entry : this.policies.entrySet())
            policies.put(entry.getKey(), entry.getValue().createPolicy());
        
        return new RootResourceAllocator(name, resourceProvider.createProvider(), policies, defaultPolicy.createPolicy(),
            timerPeriod, allocationPeriod, quotaIncreaseDelay, initializePeriod, new SystemTimeService());
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof RootResourceAllocatorConfiguration))
            return false;
        
        RootResourceAllocatorConfiguration configuration = (RootResourceAllocatorConfiguration)o;
        return super.equals(o) && resourceProvider.equals(configuration.resourceProvider) &&
            timerPeriod == configuration.timerPeriod && allocationPeriod == configuration.allocationPeriod;
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hashCode(resourceProvider, timerPeriod, allocationPeriod);
    }
}
