/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.config;

import java.util.Map;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Numbers;
import com.exametrika.common.utils.Objects;





/**
 * The {@link RawPageTypeConfiguration} is a configuration of page type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawPageTypeConfiguration extends Configuration
{
    public static final int MIN_PAGE_SIZE = 0x800;
    private final String name;
    private final int pageSize;
    private final RawPageCategoryTypeConfiguration defaultPageCategory;
    private final Map<String, RawPageCategoryTypeConfiguration> pageCategories;
    
    public RawPageTypeConfiguration(String name, int pageSize, RawPageCategoryTypeConfiguration defaultPageCategory, 
        Map<String, RawPageCategoryTypeConfiguration> pageCategories)
    {
        Assert.notNull(name);
        Assert.isTrue(!name.isEmpty());
        Assert.isTrue(Numbers.isPowerOfTwo(pageSize) && pageSize >= MIN_PAGE_SIZE);
        Assert.notNull(defaultPageCategory);
        Assert.isTrue(defaultPageCategory.getName().isEmpty());
        Assert.notNull(pageCategories);
        Assert.isTrue(!pageCategories.containsKey(defaultPageCategory.getName()));
        
        this.name = name;
        this.pageSize = pageSize;
        this.defaultPageCategory = defaultPageCategory;
        this.pageCategories = Immutables.wrap(pageCategories);
        
        for (RawPageCategoryTypeConfiguration pageCategory : pageCategories.values())
            Assert.isTrue(!pageCategory.getName().isEmpty());
    }

    public String getName()
    {
        return name;
    }
    
    public int getPageSize()
    {
        return pageSize;
    }

    public RawPageCategoryTypeConfiguration getDefaultPageCategory()
    {
        return defaultPageCategory;
    }

    public Map<String, RawPageCategoryTypeConfiguration> getPageCategories()
    {
        return pageCategories;
    }

    public boolean isCompatible(RawPageTypeConfiguration pageType)
    {
        return name.equals(pageType.name) && pageSize == pageType.pageSize && 
            defaultPageCategory.getName().equals(pageType.getDefaultPageCategory().getName()) && 
            pageType.pageCategories.keySet().containsAll(pageCategories.keySet());
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof RawPageTypeConfiguration))
            return false;
        
        RawPageTypeConfiguration configuration = (RawPageTypeConfiguration)o;
        return name.equals(configuration.name) && pageSize == configuration.pageSize && 
            defaultPageCategory.equals(configuration.defaultPageCategory) &&
            pageCategories.equals(configuration.pageCategories);
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hashCode(name, pageSize, defaultPageCategory, pageCategories);
    }
    
    @Override
    public String toString()
    {
        return name + ":" + pageSize + pageCategories.toString();
    }
}
