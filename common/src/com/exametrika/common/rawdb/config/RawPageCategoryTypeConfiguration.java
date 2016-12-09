/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;





/**
 * The {@link RawPageCategoryTypeConfiguration} is a configuration of page category type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawPageCategoryTypeConfiguration extends Configuration
{
    private final String name;
    private final long initialPageCacheSize;
    private final double minPageCachePercentage;
    private final long maxPageIdlePeriod;
    
    public RawPageCategoryTypeConfiguration(String name, long initialPageCacheSize, double minPageCachePercentage, long maxPageIdlePeriod)
    {
        Assert.notNull(name);
        Assert.isTrue(minPageCachePercentage >= 90 && minPageCachePercentage <= 100);
        
        this.name = name;
        this.initialPageCacheSize = initialPageCacheSize;
        this.minPageCachePercentage = minPageCachePercentage;
        this.maxPageIdlePeriod = maxPageIdlePeriod;
    }

    public String getName()
    {
        return name;
    }
    
    public long getInitialPageCacheSize()
    {
        return initialPageCacheSize;
    }
    
    public double getMinPageCachePercentage()
    {
        return minPageCachePercentage;
    }

    public long getMaxPageIdlePeriod()
    {
        return maxPageIdlePeriod;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof RawPageCategoryTypeConfiguration))
            return false;
        
        RawPageCategoryTypeConfiguration configuration = (RawPageCategoryTypeConfiguration)o;
        return name.equals(configuration.name) && initialPageCacheSize == configuration.initialPageCacheSize &&
            minPageCachePercentage == configuration.minPageCachePercentage &&
            maxPageIdlePeriod == configuration.maxPageIdlePeriod;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hashCode(name, initialPageCacheSize, minPageCachePercentage, maxPageIdlePeriod);
    }
    
    @Override
    public String toString()
    {
        return name.isEmpty() ? "<default>" : name;
    }
}
