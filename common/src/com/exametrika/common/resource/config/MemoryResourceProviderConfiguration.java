/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import com.exametrika.common.resource.impl.MemoryResourceProvider;
import com.exametrika.common.utils.Objects;





/**
 * The {@link MemoryResourceProviderConfiguration} is a configuration of memory resource provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MemoryResourceProviderConfiguration extends ResourceProviderConfiguration
{
    protected final boolean nativeMemory;

    public MemoryResourceProviderConfiguration(boolean nativeMemory)
    {
        this.nativeMemory = nativeMemory;
    }
    
    public boolean isNativeMemory()
    {
        return nativeMemory;
    }
    
    @Override
    public MemoryResourceProvider createProvider()
    {
        return new MemoryResourceProvider(nativeMemory);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        else if (!(o instanceof MemoryResourceProviderConfiguration))
            return false;
        
        MemoryResourceProviderConfiguration configuration = (MemoryResourceProviderConfiguration)o;
        return nativeMemory == configuration.nativeMemory;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(nativeMemory);
    }
}
