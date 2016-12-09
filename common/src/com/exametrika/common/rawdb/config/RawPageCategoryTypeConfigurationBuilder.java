/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.config;

import com.exametrika.common.utils.Assert;





/**
 * The {@link RawPageCategoryTypeConfigurationBuilder} is a builder of configuration of page category type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawPageCategoryTypeConfigurationBuilder
{
    private final RawPageTypeConfigurationBuilder parent;
    private final String name;
    private long initialPageCacheSize = 10000000;
    private double minPageCachePercentage = 90;
    private long maxPageIdlePeriod = 600000;
    
    public RawPageCategoryTypeConfigurationBuilder(RawPageTypeConfigurationBuilder parent, String name)
    {
        Assert.notNull(parent);
        Assert.notNull(name);
        
        this.parent = parent;
        this.name = name;
    }

    public RawPageCategoryTypeConfigurationBuilder(RawPageTypeConfigurationBuilder parent, RawPageCategoryTypeConfiguration configuration)
    {
        Assert.notNull(parent);
        Assert.notNull(configuration);
        
        this.parent = parent;
        name = configuration.getName();
        initialPageCacheSize = configuration.getInitialPageCacheSize();
        minPageCachePercentage = configuration.getMinPageCachePercentage();
        maxPageIdlePeriod = configuration.getMaxPageIdlePeriod();
    }
    
    public RawPageCategoryTypeConfigurationBuilder setInitialPageCacheSize(long value)
    {
        initialPageCacheSize = value;
        return this;
    }
    
    public RawPageCategoryTypeConfigurationBuilder setMinPageCachePercentage(double value)
    {
        minPageCachePercentage = value;
        return this;
    }

    public RawPageCategoryTypeConfigurationBuilder setMaxPageIdlePeriod(long value)
    {
        maxPageIdlePeriod = value;
        return this;
    }

    public RawPageTypeConfigurationBuilder end()
    {
        return parent;
    }
    
    public RawPageCategoryTypeConfiguration toConfiguration()
    {
        return new RawPageCategoryTypeConfiguration(name, initialPageCacheSize, minPageCachePercentage, maxPageIdlePeriod);
    }
}
