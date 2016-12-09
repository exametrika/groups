/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.rawdb.config.RawDatabaseConfiguration;
import com.exametrika.common.rawdb.config.RawPageTypeConfiguration;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;




/**
 * The {@link RawPageTypeManager} is a manager of page types.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawPageTypeManager
{
    private final List<RawPageType> pageTypes = new ArrayList<RawPageType>();
    
    public RawPageTypeManager(RawPageManager pageManager, RawPagePool pagePool, ITimeService timeService, 
        List<RawPageTypeConfiguration> pageTypes, boolean nativeMemory, long timerPeriod, IResourceAllocator resourceAllocator)
    {
        Assert.notNull(pageTypes);
        Assert.isTrue(!pageTypes.isEmpty());
        
        int i = 0;
        for (RawPageTypeConfiguration pageType : pageTypes)
            this.pageTypes.add(new RawPageType(i++, pageType, pageManager, pagePool, timeService, nativeMemory, resourceAllocator));
    }
    
    public void setResourceAllocator(IResourceAllocator resourceAllocator)
    {
        for (RawPageType pageType: pageTypes)
            pageType.setResourceAllocator(resourceAllocator);
    }

    public void setConfiguration(RawDatabaseConfiguration configuration, boolean clearCache)
    {
        for (int i = 0; i < pageTypes.size(); i++)
        {
            RawPageType pageType = pageTypes.get(i);
            pageType.setConfiguration(configuration.getPageTypes().get(i), clearCache);
        }
    }

    public RawPageType getPageType(int index)
    {
        return pageTypes.get(index);
    }
    
    public void clear()
    {
        for (RawPageType pageType: pageTypes)
            pageType.clear();
    }
    
    public void close()
    {
        for (RawPageType pageType: pageTypes)
            pageType.close();
    }
    
    public void onTimer(long currentTime)
    {
        for (RawPageType pageType: pageTypes)
            pageType.onTimer(currentTime);
    }
    
    public String printStatistics()
    {
        StringBuilder builder = new StringBuilder();
        
        boolean first = true;
        for (RawPageType pageType: pageTypes)
        {
            if (first)
                first = false;
            else
                builder.append('\n');
            
            builder.append(pageType.printStatistics());
        }
        
        return builder.toString();
    }
}
