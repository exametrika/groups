/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.Map;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;





/**
 * The {@link ResourceAllocatorConfiguration} is an abstract configuration of resource allocator.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class ResourceAllocatorConfiguration extends Configuration
{
    protected final String name;
    protected final Map<String, AllocationPolicyConfiguration> policies;
    protected final AllocationPolicyConfiguration defaultPolicy;
    protected final long quotaIncreaseDelay;
    protected final long initializePeriod;

    public ResourceAllocatorConfiguration(String name, Map<String, AllocationPolicyConfiguration> policies, 
        AllocationPolicyConfiguration defaultPolicy, long quotaIncreaseDelay, long initializePeriod)
    {
        Assert.notNull(name);
        Assert.notNull(policies);
        Assert.notNull(defaultPolicy);
        
        this.name = name;
        this.policies = Immutables.wrap(policies);
        this.defaultPolicy = defaultPolicy;
        this.quotaIncreaseDelay = quotaIncreaseDelay;
        this.initializePeriod = initializePeriod;
    }
    
    public final String getName()
    {
        return name;
    }
    
    public final Map<String, AllocationPolicyConfiguration> getPolicies()
    {
        return policies;
    }
    
    public final AllocationPolicyConfiguration getDefaultPolicy()
    {
        return defaultPolicy;
    }
    
    public final long getQuotaIncreaseDelay()
    {
        return quotaIncreaseDelay;
    }
    
    public final long getInitializePeriod()
    {
        return initializePeriod;
    }
    
    public abstract IResourceAllocator createAllocator();

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof ResourceAllocatorConfiguration))
            return false;
        
        ResourceAllocatorConfiguration configuration = (ResourceAllocatorConfiguration)o;
        return name.equals(configuration.name) && policies.equals(configuration.policies) && 
            defaultPolicy.equals(configuration.defaultPolicy) && 
            quotaIncreaseDelay == configuration.quotaIncreaseDelay && initializePeriod == configuration.initializePeriod;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(name, policies, defaultPolicy, quotaIncreaseDelay, initializePeriod);
    }
    
    @Override
    public String toString()
    {
        return name;
    }
}
