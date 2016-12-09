/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Numbers;





/**
 * The {@link RawPageTypeConfigurationBuilder} is a builder of configuration of page type.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RawPageTypeConfigurationBuilder
{
    private final String name;
    private final int pageSize;
    private final RawPageCategoryTypeConfigurationBuilder defaultPageCategory;
    private final Map<String, RawPageCategoryTypeConfigurationBuilder> pageCategories = new LinkedHashMap<String, RawPageCategoryTypeConfigurationBuilder>();
    private final RawDatabaseConfigurationBuilder parent;
    
    public RawPageTypeConfigurationBuilder(RawDatabaseConfigurationBuilder parent, String name, int pageSize)
    {
        Assert.notNull(parent);
        Assert.notNull(name);
        Assert.isTrue(Numbers.isPowerOfTwo(pageSize) && pageSize >= RawPageTypeConfiguration.MIN_PAGE_SIZE);
        
        this.parent = parent;
        this.name = name;
        this.pageSize = pageSize;
        defaultPageCategory = new RawPageCategoryTypeConfigurationBuilder(this, "");
    }
    
    public RawPageTypeConfigurationBuilder(RawDatabaseConfigurationBuilder parent, RawPageTypeConfiguration configuration)
    {
        Assert.notNull(parent);
        Assert.notNull(configuration);
        
        this.parent = parent;
        this.name = configuration.getName();
        this.pageSize = configuration.getPageSize();
        defaultPageCategory = new RawPageCategoryTypeConfigurationBuilder(this, configuration.getDefaultPageCategory());
        for (Map.Entry<String, RawPageCategoryTypeConfiguration> entry : configuration.getPageCategories().entrySet())
            pageCategories.put(entry.getKey(), new RawPageCategoryTypeConfigurationBuilder(this, entry.getValue()));
    }

    public RawPageCategoryTypeConfigurationBuilder getDefaultPageCategory()
    {
        return defaultPageCategory;
    }

    public RawPageCategoryTypeConfigurationBuilder addPageCategory(String name)
    {
        Assert.notNull(name);
        Assert.isTrue(!name.isEmpty());
        RawPageCategoryTypeConfigurationBuilder builder = new RawPageCategoryTypeConfigurationBuilder(this, name);
        pageCategories.put(name, builder);
        return builder;
    }

    public RawDatabaseConfigurationBuilder end()
    {
        return parent;
    }
    
    public RawPageTypeConfiguration toConfiguration()
    {
        Map<String, RawPageCategoryTypeConfiguration> pageCategories = new LinkedHashMap<String, RawPageCategoryTypeConfiguration>(this.pageCategories.size());
        for (Map.Entry<String, RawPageCategoryTypeConfigurationBuilder> entry : this.pageCategories.entrySet())
            pageCategories.put(entry.getKey(), entry.getValue().toConfiguration());
        
        return new RawPageTypeConfiguration(name, pageSize, defaultPageCategory.toConfiguration(), pageCategories);
    }
}
