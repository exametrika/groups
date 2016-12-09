/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.exametrika.common.rawdb.config.RawDatabaseConfiguration.Flag;
import com.exametrika.common.resource.config.ResourceAllocatorConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Enums;





/**
 * The {@link RawDatabaseConfigurationBuilder} is a builder of database configuration.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawDatabaseConfigurationBuilder
{
    private String name = "db";
    private List<String> paths = new ArrayList<String>();
    private long flushPeriod = 3000;
    private long maxFlushSize = Long.MAX_VALUE;
    private long timerPeriod = 1000;
    private long maxFileSize = Long.MAX_VALUE;
    private Set<Flag> flags = Enums.of(Flag.NATIVE_MEMORY);
    private long batchRunPeriod = 100;
    private long batchIdlePeriod = 900;
    private List<RawPageTypeConfigurationBuilder> pageTypes = new ArrayList<RawPageTypeConfigurationBuilder>();
    private ResourceAllocatorConfiguration resourceAllocator = new RootResourceAllocatorConfigurationBuilder().toConfiguration();
    
    public RawDatabaseConfigurationBuilder()
    {
    }
    
    public RawDatabaseConfigurationBuilder(RawDatabaseConfiguration configuration)
    {
        Assert.notNull(configuration);
        
        name = configuration.getName();
        paths.addAll(configuration.getPaths());
        flushPeriod = configuration.getFlushPeriod();
        maxFlushSize = configuration.getMaxFlushSize();
        timerPeriod = configuration.getTimerPeriod();
        maxFileSize = configuration.getMaxFileSize();
        flags = Enums.copyOf(configuration.getFlags());
        
        for (RawPageTypeConfiguration pageType : configuration.getPageTypes())
            pageTypes.add(new RawPageTypeConfigurationBuilder(this, pageType));
    }
    
    public RawDatabaseConfigurationBuilder setName(String value)
    {
        Assert.notNull(value);
        this.name = value;
        return this;
    }
    
    public RawDatabaseConfigurationBuilder addPath(String value)
    {
        Assert.notNull(value);
        paths.add(value);
        return this;
    }
    
    public RawDatabaseConfigurationBuilder clearPaths()
    {
        paths.clear();
        return this;
    }
    
    public RawDatabaseConfigurationBuilder setFlushPeriod(long value)
    {
        flushPeriod = value;
        return this;
    }
    
    public RawDatabaseConfigurationBuilder setMaxFlushSize(long value)
    {
        maxFlushSize = value;
        return this;
    }

    public RawDatabaseConfigurationBuilder setTimerPeriod(long value)
    {
        timerPeriod = value;
        return this;
    }

    public RawDatabaseConfigurationBuilder setMaxFileSize(long value)
    {
        maxFileSize = value;
        return this;
    }

    public RawDatabaseConfigurationBuilder addFlag(Flag value)
    {
        flags.add(value);
        return this;
    }
    
    public RawDatabaseConfigurationBuilder removeFlag(Flag value)
    {
        flags.remove(value);
        return this;
    }

    public RawDatabaseConfigurationBuilder setBatchRunPeriod(long value)
    {
        batchRunPeriod = value;
        return this;
    }
    
    public RawDatabaseConfigurationBuilder setBatchIdlePeriod(long value)
    {
        batchIdlePeriod = value;
        return this;
    }
    
    public RawPageTypeConfigurationBuilder addPageType(String name, int pageSize)
    {
        RawPageTypeConfigurationBuilder builder = new RawPageTypeConfigurationBuilder(this, name, pageSize);
        pageTypes.add(builder);
        return builder;
    }
    
    public RawDatabaseConfigurationBuilder setResourceAllocator(ResourceAllocatorConfiguration resourceAllocator)
    {
        Assert.notNull(resourceAllocator);
        this.resourceAllocator = resourceAllocator;
        return this;
    }
    
    public RawDatabaseConfiguration toConfiguration()
    {
        List<RawPageTypeConfiguration> pageTypes = new ArrayList<RawPageTypeConfiguration>(this.pageTypes.size());
        
        if (!this.pageTypes.isEmpty())
        {
            for (RawPageTypeConfigurationBuilder builder : this.pageTypes)
                pageTypes.add(builder.toConfiguration());
        }
        else
            pageTypes.add(new RawPageTypeConfigurationBuilder(this, "default", 0x4000).toConfiguration());
        
        return new RawDatabaseConfiguration(name, new ArrayList<String>(paths), flushPeriod, maxFlushSize, timerPeriod, maxFileSize, flags, 
            batchRunPeriod, batchIdlePeriod, pageTypes, resourceAllocator);
    }
}
