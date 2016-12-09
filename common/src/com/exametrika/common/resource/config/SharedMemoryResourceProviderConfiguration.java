/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import com.exametrika.common.resource.impl.SharedMemoryResourceProvider;





/**
 * The {@link SharedMemoryResourceProviderConfiguration} is a configuration of shared memory resource provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SharedMemoryResourceProviderConfiguration extends ResourceProviderConfiguration
{
    @Override
    public SharedMemoryResourceProvider createProvider()
    {
        return new SharedMemoryResourceProvider();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof SharedMemoryResourceProviderConfiguration))
            return false;
        
        return true;
    }

    @Override
    public int hashCode()
    {
        return getClass().hashCode();
    }
}
