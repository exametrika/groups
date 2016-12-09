/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.config.RawPageCategoryTypeConfiguration;
import com.exametrika.common.rawdb.impl.RawTransactionLog.FlushInfo;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Out;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.SimpleList.Element;



/**
 * The {@link RawPageCache} is a page cache.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawPageCache implements IResourceConsumer
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String name;
    private final int pageSize;
    private RawPageCategoryTypeConfiguration configuration;
    private final RawPageType pageType;
    private final RawPageManager pageManager;
    private final RawPagePool pagePool;
    private final RawRegionPool regionPool;
    private final RawRegionAllocator regionAllocator;
    private final ITimeService timeService;
    private final IResourceAllocator resourceAllocator;
    private final boolean nativeMemory;
    private final SimpleList<RawPage> pages = new SimpleList<RawPage>();
    private volatile long pageCacheSize;
    private volatile long maxPageCacheSize;
    private volatile long quota;
    private volatile long preparedQuota;
    private volatile long applyQuotaTime;
    private volatile long batchMaxPageCacheSize = Long.MAX_VALUE;
    private long unloadPageCount;
    private long unloadByTimerCount;
    private long unloadByOverflowCount;
    private int refCount;
    private int refreshIndex;
    private long refreshPageCount;
    private final int interceptId;
    
    public RawPageCache(String name, int pageSize, RawPageCategoryTypeConfiguration configuration, RawPageType pageType, RawPageManager pageManager, 
        RawPagePool pagePool, RawRegionPool regionPool, RawRegionAllocator regionAllocator, ITimeService timeService, 
        IResourceAllocator resourceAllocator, boolean nativeMemory)
    {
        Assert.notNull(name);
        Assert.notNull(configuration);
        Assert.notNull(pageType);
        Assert.notNull(pageManager);
        Assert.notNull(pagePool);
        Assert.notNull(regionPool);
        Assert.notNull(regionAllocator);
        Assert.notNull(timeService);
        Assert.notNull(resourceAllocator);
        
        this.name = name;
        this.pageSize = pageSize;
        this.configuration = configuration;
        this.pageType = pageType;
        this.pageManager = pageManager;
        this.pagePool = pagePool;
        this.regionPool = regionPool;
        this.regionAllocator = regionAllocator;
        this.timeService = timeService;
        this.resourceAllocator = resourceAllocator;
        this.nativeMemory = nativeMemory;
        this.maxPageCacheSize = configuration.getInitialPageCacheSize();
        this.quota = maxPageCacheSize;
        
        resourceAllocator.register(getResourceConsumerName(), this);
        interceptId = RawDatabaseInterceptor.INSTANCE.onPageCacheCreated(pageManager.getDatabase().getInterceptId(), name, pageSize);
    }
    
    public String getName()
    {
        return name;
    }
    
    public RawPageCategoryTypeConfiguration getConfiguration()
    {
        return configuration;
    }
    
    public int getPageSize()
    {
        return pageSize;
    }

    public RawPageType getPageType()
    {
        return pageType;
    }
    
    public long getPageCacheSize()
    {
        return pageCacheSize;
    }
    
    public long getMaxPageCacheSize()
    {
        return maxPageCacheSize;
    }

    public int getRefreshIndex()
    {
        return refreshIndex;
    }
    
    public void setConfiguration(RawPageCategoryTypeConfiguration configuration)
    {
        this.configuration = configuration;
    }

    public synchronized void setBatchMaxPageCacheSize(long value)
    {
        batchMaxPageCacheSize = value;
        maxPageCacheSize = Math.min(quota, batchMaxPageCacheSize);
        preparedQuota = 0;
        applyQuotaTime = 0;
        
        updateRefreshPageCount(0, false);
        unloadExcessive();
    }

    public void incrementSize()
    {
        pageCacheSize += pageSize;
    }
    
    public void decrementSize()
    {
        pageCacheSize -= pageSize;
        Assert.checkState(pageCacheSize >= 0);
    }
    
    public void addRef()
    {
        if (name.isEmpty())
            return;
        
        refCount++;
    }
    
    public void release()
    {
        if (name.isEmpty())
            return;
        
        refCount--;
        if (refCount <= 0)
        {
            pageType.removePageCache(name);
            Assert.checkState(pageCacheSize == 0 && pages.isEmpty());
            
            resourceAllocator.unregister(getResourceConsumerName());
            setQuota(configuration.getInitialPageCacheSize());
            RawDatabaseInterceptor.INSTANCE.onPageCacheClosed(interceptId);
        }
    }
    
    public void addLoadedPage(RawPage page)
    {
        renewPage(page, false);
    }

    public int renewPage(RawPage page, boolean renew)
    {
        Element<RawPage> element = page.getElement();
        element.remove();
        element.reset();
        pages.addLast(element);
        page.setLastAccessTime(timeService.getCurrentTime());
        RawDatabaseInterceptor.INSTANCE.onPageLoaded(interceptId);
        updateRefreshPageCount(1, renew);
        return preparedQuota == 0 ? refreshIndex : -1;
    }
    
    public void removePage(RawPage page)
    {
        Element<RawPage> element = page.getElement();
        element.remove();
        element.reset();
        
        element = page.getWriteElement();
        element.remove();
        element.reset();
        
        element = page.getCommittedElement();
        element.remove();
        element.reset();
        
        page.setStale();
        
        pagePool.add(page);
        
        if (page.getRefreshIndex() == refreshIndex)
            updateRefreshPageCount(-1, false);
    }

    public void migratePage(RawPage page)
    {
        renewPage(page, false);
        page.migrate(this);
    }
    
    public void clear()
    {
        for (RawPage page : pages.values())
            removePage(page);
    }

    public void close()
    {
        for (RawPage page : pages.values())
            removePage(page);
        
        pages.clear();
        
        resourceAllocator.unregister(getResourceConsumerName());
        setQuota(configuration.getInitialPageCacheSize());
        
        RawDatabaseInterceptor.INSTANCE.onPageCacheClosed(interceptId);
    }

    public void unloadExcessive()
    {
        applyQuota();
        
        while (pageCacheSize > maxPageCacheSize)
        {
            long oldPageCacheSize = pageCacheSize;
            
            unloadPage();
            
            if (pageCacheSize == oldPageCacheSize)
                break;
        }
    }
    
    public void onTimer(long currentTime)
    {
        if (!pages.isEmpty())
        {
            refreshIndex++;
            RawPage page = pages.getFirst().getValue();
            if (currentTime - page.getLastAccessTime() > configuration.getMaxPageIdlePeriod())
                unloadPages(false);
            
            updateRefreshPageCount(0, false);
            unloadExcessive();
        }
        
            RawDatabaseInterceptor.INSTANCE.onPageCache(interceptId, pageCacheSize, maxPageCacheSize, quota);
    }
    
    public RawRegion acquireRegion(int fileIndex, long pageIndex, boolean readOnly, boolean init, Out<Boolean> fromPool)
    {
        applyQuota();
        
        if (pageCacheSize > 0 && pageCacheSize + pageSize > maxPageCacheSize)
            unloadPage();
        
        RawRegion region = regionPool.remove(this);
        if (region == null)
        {
            region = regionAllocator.allocate(fileIndex, pageIndex, readOnly, pageSize);
            region.setUsed(this);
            if (fromPool != null)
                fromPool.value = false;
        }
        else
        {
            region.setFileIndex(fileIndex);
            region.setPageIndex(pageIndex);
            if (fromPool != null)
                fromPool.value = true;
            if (init)
                region.init();
        }
        
        return region;
    }
    
    public void releaseRegion(RawRegion region)
    {
        regionPool.add(region);
    }
    
    @Override
    public long getAmount()
    {
        return pageCacheSize;
    }

    @Override
    public long getQuota()
    {
        return quota;
    }

    @Override
    public synchronized void setQuota(long value)
    {
        quota = value;
        long newSize = Math.min(quota, batchMaxPageCacheSize);
        if (newSize >= maxPageCacheSize)
            maxPageCacheSize = newSize;
        else
        {
            if (preparedQuota == 0)
                applyQuotaTime = timeService.getCurrentTime() + pageManager.getDatabase().getConfiguration().getTimerPeriod() + 1000;
            
            preparedQuota = newSize;
        }
    }

    public String printStatistics()
    {
        return messages.statistics(!name.isEmpty() ? name : "default", configuration.toString(), maxPageCacheSize, pageCacheSize, unloadPageCount, 
            unloadByOverflowCount, unloadByTimerCount).toString();
    }
    
    @Override
    public String toString()
    {
        return getResourceConsumerName();
    }
    
    private void unloadPage()
    {
        unloadPageCount++;
        if (!pages.isEmpty())
        {
            RawPage page = pages.getFirst().getValue();
            Element<RawPage> committedElement = page.getCommittedElement();
            
            if (!page.isFlushing() && !page.isModified() && (!committedElement.isAttached() || committedElement.isRemoved())
                && page.getRefreshIndex() != refreshIndex)
            {
                page.getFile().unloadPage(page.getIndex());
                removePage(page);
                RawDatabaseInterceptor.INSTANCE.onPageUnloaded(interceptId, false);
                return;
            }
        }

        unloadPages(true);
        return;
    }
    
    private void unloadPages(boolean exceedsMaxSize)
    {
        applyQuota();
        
        boolean byTimer;
        if (exceedsMaxSize)
        {
            unloadByOverflowCount++;
            byTimer = false;
            pageManager.flush(true);
            regionPool.updateFlushingRegions();
        }
        else
        {
            unloadByTimerCount++;
            byTimer = true;
        }
        
        List<FlushInfo> flushedPages = null;
        
        long currentTime = timeService.getCurrentTime();
        
        long minPageCacheSize = (long)(maxPageCacheSize * configuration.getMinPageCachePercentage() / 100);
        for (Iterator<Element<RawPage>> it = pages.iterator(); it.hasNext(); )
        {
            RawPage page = it.next().getValue();
            
            if (exceedsMaxSize && page.getRefreshIndex() == refreshIndex)
                break;
            
            if (currentTime - page.getLastAccessTime() > configuration.getMaxPageIdlePeriod() || 
                (exceedsMaxSize && pageCacheSize - regionPool.getFlushingSize() > minPageCacheSize))
            {
                if (exceedsMaxSize)
                {
                    if (page.isModified())
                    {
                        page.commit();
                        if (flushedPages == null)
                            flushedPages = new ArrayList<FlushInfo>();
                        flushedPages.add(page.flush(false));
                    }
                }
                else
                {
                    Assert.checkState(!page.isModified());
                    Element<RawPage> committedElement = page.getCommittedElement();
                    if (committedElement.isAttached() && !committedElement.isRemoved())
                        pageManager.flush(true);
                }
                    
                page.getFile().unloadPage(page.getIndex());
                removePage(page);
                RawDatabaseInterceptor.INSTANCE.onPageUnloaded(interceptId, byTimer);
            }
            else
                break;
        }
            
        if (exceedsMaxSize && flushedPages != null)
        {
            pageManager.addFlushedPages(flushedPages);
            pageManager.flushPendingPages(false);
            regionPool.updateFlushingRegions();
        }
    }
    
    private void updateRefreshPageCount(int count, boolean renew)
    {
        applyQuota();
        
        refreshPageCount += count;
        Assert.checkState(refreshPageCount >= 0);
        
        if (refreshPageCount > maxPageCacheSize / pageSize / 10)
        {
            refreshIndex++;
            refreshPageCount = 0;

            if (renew)
                refreshPageCount++;
        }
    }
    
    private void applyQuota()
    {
        if (preparedQuota == 0)
            return;
        
        synchronized (this)
        {
            if (preparedQuota > 0 && timeService.getCurrentTime() > applyQuotaTime)
            {
                maxPageCacheSize = preparedQuota;
                preparedQuota = 0;
                applyQuotaTime = 0;
            }
        }
    }
    
    private String getResourceConsumerName()
    {
        return (nativeMemory ? "native." : "heap.") + "pages." + pageType.getConfiguration().getName() + "." + (!name.isEmpty() ? name : "<default>"); 
    }
    
    private interface IMessages
    {
        @DefaultMessage("page cache ''{0}:{1}'' - max cache size: {2}, cache size: {3}, unload page count: {4}, " +
            "unloadByOverflowCount: {5}, unloadByTimerCount: {6}")
        ILocalizedMessage statistics(String category, String categoryType, long maxPageCacheSize, long pageCacheSize, 
            long unloadPageCount, long unloadByOverflowCount, long unloadByTimerCount);
    }
}
