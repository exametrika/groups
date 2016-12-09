/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.utils.Assert;





/**
 * The {@link ResourceAllocatorConfigurationBuilder} is a builder of abstract configuration of resource allocator.
 * 
 * @param <T> builder type
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public abstract class ResourceAllocatorConfigurationBuilder<T>
{
    protected String name = "resource allocator";
    protected Map<String, AllocationPolicyConfiguration> policies = new LinkedHashMap<String, AllocationPolicyConfiguration>();
    protected AllocationPolicyConfiguration defaultPolicy = new DynamicUniformAllocationPolicyConfigurationBuilder().toConfiguration();
    protected long quotaIncreaseDelay = 1000;
    protected long initializePeriod = 60000;

    public T setName(String name)
    {
        Assert.notNull(name);
        
        this.name = name;
        return (T)this;
    }
    
    public T addPolicy(String pattern, AllocationPolicyConfiguration policy)
    {
        Assert.notNull(pattern);
        Assert.notNull(policy);
        
        policies.put(pattern, policy);
        return (T)this;
    }
    
    public T setDefaultPolicy(AllocationPolicyConfiguration policy)
    {
        Assert.notNull(policy);
        
        defaultPolicy = policy;
        return (T)this;
    }
    
    public T setQuotaIncreaseDelay(long value)
    {
        quotaIncreaseDelay = value;
        return (T)this;
    }
    
    public T setInitializePeriod(long value)
    {
        initializePeriod = value;
        return (T)this;
    }
}
