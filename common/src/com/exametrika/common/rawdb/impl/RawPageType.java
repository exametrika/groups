/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.config.RawPageCategoryTypeConfiguration;
import com.exametrika.common.rawdb.config.RawPageTypeConfiguration;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;




/**
 * The {@link RawPageType} is a page type.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawPageType
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int index;
    private final RawPageManager pageManager;
    private final RawPagePool pagePool;
    private final ITimeService timeService;
    private final boolean nativeMemory;
    private IResourceAllocator resourceAllocator;
    private RawPageTypeConfiguration configuration;
    private final RawRegionAllocator regionAllocator;
    private final RawRegionPool regionPool;
    private final Map<String, RawPageCache> pageCaches = new LinkedHashMap<String, RawPageCache>();
    private final int interceptId;
    
    public RawPageType(int index, RawPageTypeConfiguration configuration, RawPageManager pageManager, RawPagePool pagePool, 
        ITimeService timeService, boolean nativeMemory, IResourceAllocator resourceAllocator)
    {
        Assert.notNull(configuration);
        Assert.notNull(pageManager);
        Assert.notNull(pagePool);
        Assert.notNull(timeService);
        Assert.notNull(resourceAllocator);
        
        this.index = index;
        this.configuration = configuration;
        this.pageManager = pageManager;
        this.pagePool = pagePool;
        this.timeService = timeService;
        this.nativeMemory = nativeMemory;
        this.resourceAllocator = resourceAllocator;
        interceptId = RawDatabaseInterceptor.INSTANCE.onPageTypeCreated(pageManager.getDatabase().getInterceptId(), 
            configuration.getName(), configuration.getPageSize());
        this.regionAllocator = new RawRegionAllocator(nativeMemory, interceptId);
        this.regionPool = new RawRegionPool(configuration.getPageSize(), configuration.getDefaultPageCategory().getMaxPageIdlePeriod(),
            regionAllocator);
    }

    public int getIndex()
    {
        return index;
    }
    
    public RawPageTypeConfiguration getConfiguration()
    {
        return configuration;
    }

    public void setResourceAllocator(IResourceAllocator resourceAllocator)
    {
        this.resourceAllocator = resourceAllocator;
    }

    public void setConfiguration(RawPageTypeConfiguration configuration, boolean clearCache)
    {
        this.configuration = configuration;
        regionPool.setRegionIdlePeriod(configuration.getDefaultPageCategory().getMaxPageIdlePeriod());
        
        if (!clearCache)
        {
            for (RawPageCache pageCache : pageCaches.values())
            {
                if (pageCache.getConfiguration().getName().isEmpty())
                    pageCache.setConfiguration(configuration.getDefaultPageCategory());
                else
                    pageCache.setConfiguration(configuration.getPageCategories().get(pageCache.getConfiguration().getName()));
            }
        }
    }

    public RawPageCache getExistingPageCache(String category)
    {
        if (category == null)
            category = "";

        RawPageCache pageCache = pageCaches.get(category);
        return pageCache;
    }
    
    public RawPageCache getPageCache(String categoryType, String category)
    {
        if (category == null)
            category = "";

        RawPageCache pageCache = pageCaches.get(category);
        if (pageCache != null)
        {
            if (categoryType != null && !categoryType.isEmpty())
                Assert.isTrue(pageCache.getConfiguration().getName().equals(categoryType));
            else
                Assert.isTrue(pageCache.getConfiguration().getName().isEmpty());
            return pageCache;
        }
        
        RawPageCategoryTypeConfiguration categoryTypeConfiguration;
        if (categoryType == null || categoryType.isEmpty())
            categoryTypeConfiguration = configuration.getDefaultPageCategory();
        else
        {
            categoryTypeConfiguration = configuration.getPageCategories().get(categoryType);
            Assert.notNull(categoryTypeConfiguration);
        }
        
        pageCache = new RawPageCache(category, configuration.getPageSize(), categoryTypeConfiguration, 
            this, pageManager, pagePool, regionPool, regionAllocator, timeService, resourceAllocator, nativeMemory);
        pageCaches.put(category, pageCache);

        return pageCache;
    }
    
    public void removePageCache(String category)
    {
        pageCaches.remove(category);
    }
    
    public void clear()
    {
        for (RawPageCache pageCache : pageCaches.values())
            pageCache.clear();
    }
    
    public void close()
    {
        for (RawPageCache pageCache : pageCaches.values())
            pageCache.close();
        
        pageCaches.clear();
        regionPool.close();
        RawDatabaseInterceptor.INSTANCE.onPageTypeClosed(interceptId);
    }
    
    public void onTimer(long currentTime)
    {
        long maxPageCacheSize = 0;
        long pageCacheSize = 0;
        
        for (RawPageCache pageCache : pageCaches.values())
        {
            pageCache.onTimer(currentTime);
            maxPageCacheSize += pageCache.getMaxPageCacheSize();
            pageCacheSize += pageCache.getPageCacheSize();
        }
        
        regionPool.onTimer(currentTime);
        regionPool.freeRegions(maxPageCacheSize, pageCacheSize);
        
            RawDatabaseInterceptor.INSTANCE.onPageType(interceptId, regionPool.getRegionCount(), regionPool.getRegionsSize());
    }
    
    public String printStatistics()
    {
        StringBuilder builder = new StringBuilder();
        
        builder.append(regionAllocator.printStatistics());
        builder.append('\n');
        builder.append(regionPool.printStatistics());
        
        for (RawPageCache pageCache : pageCaches.values())
        {
            builder.append('\n');
            builder.append(pageCache.printStatistics());
        }
        
        return messages.statistics(index, configuration.getPageSize()) + "\n" + Strings.indent(builder.toString(), 4);
    }
    
    @Override
    public String toString()
    {
        return configuration.toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("page type ''{0}({1})'':")
        ILocalizedMessage statistics(int index, int pageSize);
    }
}
