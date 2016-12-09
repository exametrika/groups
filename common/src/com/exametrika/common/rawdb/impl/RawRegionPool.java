/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleDeque;
import com.exametrika.common.utils.SimpleDeque.IIterator;




/**
 * The {@link RawRegionPool} is a pool of regions of the same size.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawRegionPool
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int pageSize;
    private long maxRegionIdlePeriod;
    private final RawRegionAllocator regionAllocator;
    private final SimpleDeque<RawRegion> regions = new SimpleDeque<RawRegion>();
    private final SimpleDeque<RawRegion> flushingRegions = new SimpleDeque<RawRegion>();
    private long lastStatisticsTime;
    private int minCount;
    
    public RawRegionPool(int pageSize, long maxRegionIdlePeriod, RawRegionAllocator regionAllocator)
    {
        Assert.notNull(regionAllocator);
        
        this.pageSize = pageSize;
        this.maxRegionIdlePeriod = maxRegionIdlePeriod;
        this.regionAllocator = regionAllocator;
    }
    
    public long getFlushingSize()
    {
        return flushingRegions.size() * pageSize;
    }
    
    public int getRegionCount()
    {
        return regions.size() + flushingRegions.size();
    }
    
    public long getRegionsSize()
    {
        return (regions.size() + flushingRegions.size()) * pageSize;
    }
    
    public void setRegionIdlePeriod(long maxRegionIdlePeriod)
    {
        this.maxRegionIdlePeriod = maxRegionIdlePeriod;
    }

    public void add(RawRegion region)
    {
        Assert.notNull(region);
        Assert.isTrue(region.getLength() == pageSize);

        if (region.isFlushing())
            flushingRegions.offer(region);
        else 
        {
            Assert.checkState(!region.isFree());
            region.setFree();
            
            regions.offer(region);
        }
    }

    public RawRegion remove(RawPageCache pageCache)
    {
        RawRegion region = flushingRegions.peekIgnoreNulls();
        if (region != null && !region.isFlushing())
        {
            flushingRegions.poll();
            region.setFree();
            region.setUsed(pageCache);
            return region;
        }

        if (regions.isEmpty())
            return null;
        
        region = regions.poll();
        
        Assert.checkState(region.isFree());
        region.setUsed(pageCache);
        
        if (minCount > regions.size())
            minCount = regions.size();
        
        return region;
    }
    
    public void close()
    {
        for (RawRegion region : regions)
            regionAllocator.free(region);
        
        regions.clear();
        
        for (RawRegion region : flushingRegions)
        {
            if (region != null)
            {
                region.setFree();
                regionAllocator.free(region);
            }
        }
        
        flushingRegions.clear();
        minCount = 0;
    }
    
    public void updateFlushingRegions()
    {
        for (IIterator<RawRegion> it = flushingRegions.iterator(); it.hasNext(); )
        {
            RawRegion region = it.next();
            if (region != null && !region.isFlushing())
            {
                add(region);
                it.set(null);
            }
        }
        
        flushingRegions.peekIgnoreNulls();
    }
    
    public void freeRegions(long maxPageCacheSize, long pageCacheSize)
    {
        long freeRegionCount = (pageCacheSize + (regions.size() + flushingRegions.size()) * pageSize - maxPageCacheSize) / pageSize;
        if (freeRegionCount > 0)
        {
            while (true)
            {
                if (regions.isEmpty() || freeRegionCount == 0)
                    break;
                
                RawRegion region = regions.poll();
                regionAllocator.free(region);
                freeRegionCount--;
            }
        }
    }

    public void onTimer(long currentTime)
    {
        if (lastStatisticsTime == 0 || currentTime > lastStatisticsTime + maxRegionIdlePeriod)
        {
            if (lastStatisticsTime > 0 && minCount > 0)
            {
                while (true)
                {
                    if (regions.isEmpty() || minCount == 0)
                        break;
                    
                    RawRegion region = regions.poll();
                    regionAllocator.free(region);
                    minCount--;
                }
            }
            
            minCount = regions.size();
            lastStatisticsTime = currentTime;
        }
        
        updateFlushingRegions();
    }
    
    public String printStatistics()
    {
        return messages.statistics(regions.size(), regions.size() * pageSize, flushingRegions.size(), 
            flushingRegions.size() * pageSize).toString();
    }

    private interface IMessages
    {
        @DefaultMessage("free regions count: {0}, free regions size: {1}, flushing free regions count: {2}, flushing free regions size: {3}")
        ILocalizedMessage statistics(long freeRegionsCount, long freeRegionsSize, long flushingRegionsCount, long flushingRegionsSize);
    }
}
