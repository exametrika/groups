/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.impl.ChildResourceAllocator;
import com.exametrika.common.time.impl.SystemTimeService;





/**
 * The {@link ChildResourceAllocatorConfiguration} is a configuration of child resource allocator.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ChildResourceAllocatorConfiguration extends ResourceAllocatorConfiguration
{
    public ChildResourceAllocatorConfiguration(String name, Map<String, AllocationPolicyConfiguration> policies, 
        AllocationPolicyConfiguration defaultPolicy, long quotaIncreaseDelay, long initializePeriod)
    {
        super(name, policies, defaultPolicy, quotaIncreaseDelay, initializePeriod);
    }
    
    @Override
    public ChildResourceAllocator createAllocator()
    {
        Map<String, IAllocationPolicy> policies = new LinkedHashMap<String, IAllocationPolicy>(this.policies.size());
        for (Map.Entry<String, AllocationPolicyConfiguration> entry : this.policies.entrySet())
            policies.put(entry.getKey(), entry.getValue().createPolicy());
        
        return new ChildResourceAllocator(name, policies, defaultPolicy.createPolicy(), quotaIncreaseDelay, 
            initializePeriod, new SystemTimeService());
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof ChildResourceAllocatorConfiguration))
            return false;
        
        return super.equals(o);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }
}
