/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import com.exametrika.common.resource.impl.FixedResourceProvider;
import com.exametrika.common.utils.Objects;





/**
 * The {@link FixedResourceProviderConfiguration} is a configuration of fixed resource provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FixedResourceProviderConfiguration extends ResourceProviderConfiguration
{
    private final long amount;

    public FixedResourceProviderConfiguration(long amount)
    {
        this.amount = amount;
    }
    
    public long getAmount()
    {
        return amount;
    }
    
    @Override
    public FixedResourceProvider createProvider()
    {
        return new FixedResourceProvider(amount);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof FixedResourceProviderConfiguration))
            return false;
        
        FixedResourceProviderConfiguration configuration = (FixedResourceProviderConfiguration)o;
        return amount == configuration.amount;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(amount);
    }
}
