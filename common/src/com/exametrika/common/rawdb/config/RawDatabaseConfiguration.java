/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.resource.config.ResourceAllocatorConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;





/**
 * The {@link RawDatabaseConfiguration} is a database configuration.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawDatabaseConfiguration extends Configuration
{
    private final String name;
    private final List<String> paths;
    private final long flushPeriod;
    private final long maxFlushSize;
    private final long timerPeriod;
    private final long maxFileSize;
    private final Set<Flag> flags;
    private final long batchRunPeriod;
    private final long batchIdlePeriod;
    private final List<RawPageTypeConfiguration> pageTypes;
    private final ResourceAllocatorConfiguration resourceAllocator;
    
    public enum Flag
    {
        NATIVE_MEMORY,
        
        PRELOAD_DATA,
        
        DISABLE_SYNC,
        
        DISABLE_IO_CACHE_PAGE_EVICTION
    }
    
    public RawDatabaseConfiguration(String name, List<String> paths, long flushPeriod, long maxFlushSize, long timerPeriod, 
        long maxFileSize, Set<Flag> flags, long batchRunPeriod, long batchIdlePeriod, 
        List<RawPageTypeConfiguration> pageTypes, ResourceAllocatorConfiguration resourceAllocator)
    {
        Assert.notNull(name);
        Assert.notNull(paths);
        Assert.isTrue(!paths.isEmpty());
        Assert.notNull(flags);
        Assert.notNull(pageTypes);
        Assert.isTrue(!pageTypes.isEmpty());
        Assert.notNull(resourceAllocator);
        
        Map<String, RawPageTypeConfiguration> pageTypeMap = new LinkedHashMap<String, RawPageTypeConfiguration>();
        for (RawPageTypeConfiguration pageType : pageTypes)
            Assert.isTrue(pageTypeMap.put(pageType.getName(), pageType) == null);
        
        this.name = name;
        this.paths = Immutables.wrap(paths);
        this.flushPeriod = flushPeriod;
        this.maxFlushSize = maxFlushSize;
        this.timerPeriod = timerPeriod;
        this.maxFileSize = maxFileSize;
        this.flags = Immutables.wrap(flags);
        this.batchRunPeriod = batchRunPeriod;
        this.batchIdlePeriod = batchIdlePeriod;
        this.pageTypes = Immutables.wrap(pageTypes);
        this.resourceAllocator = resourceAllocator;
    }

    public String getName()
    {
        return name;
    }
    
    public List<String> getPaths()
    {
        return paths;
    }
    
    public long getFlushPeriod()
    {
        return flushPeriod;
    }
    
    public long getMaxFlushSize()
    {
        return maxFlushSize;
    }

    public long getTimerPeriod()
    {
        return timerPeriod;
    }

    public long getMaxFileSize()
    {
        return maxFileSize;
    }

    public Set<Flag> getFlags()
    {
        return flags;
    }

    public long getBatchRunPeriod()
    {
        return batchRunPeriod;
    }
    
    public long getBatchIdlePeriod()
    {
        return batchIdlePeriod;
    }
    
    public List<RawPageTypeConfiguration> getPageTypes()
    {
        return pageTypes;
    }
    
    public ResourceAllocatorConfiguration getResourceAllocator()
    {
        return resourceAllocator;
    }

    public boolean isCompatible(RawDatabaseConfiguration configuration)
    {
        Assert.notNull(configuration);
        
        if (pageTypes.size() == configuration.pageTypes.size())
        {
            for (int i = 0; i < pageTypes.size(); i++)
            {
                RawPageTypeConfiguration pageType = pageTypes.get(i);
                RawPageTypeConfiguration newPageType = configuration.pageTypes.get(i);
                
                if (!pageType.isCompatible(newPageType))
                    return false;
            }
        }
        else
            return false;
        
        return name.equals(configuration.name) && paths.equals(configuration.paths) && maxFileSize == configuration.maxFileSize &&
            flags.equals(configuration.flags);
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof RawDatabaseConfiguration))
            return false;
        
        RawDatabaseConfiguration configuration = (RawDatabaseConfiguration)o;
        return name.equals(configuration.name) && paths.equals(configuration.paths) && 
            flushPeriod == configuration.flushPeriod && maxFlushSize == configuration.maxFlushSize &&
            timerPeriod == configuration.timerPeriod && maxFileSize == configuration.maxFileSize &&
            flags.equals(configuration.flags) && batchRunPeriod == configuration.batchRunPeriod && 
            batchIdlePeriod == configuration.batchIdlePeriod && pageTypes.equals(configuration.pageTypes) &&
            resourceAllocator.equals(configuration.resourceAllocator);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hashCode(name, paths, flushPeriod, maxFlushSize, timerPeriod, maxFileSize, flags, 
            batchRunPeriod, batchIdlePeriod, pageTypes, resourceAllocator);
    }
    
    @Override
    public String toString()
    {
        return name + paths.toString();
    }
}
