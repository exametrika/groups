/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawPageData;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawTransactionReadOnlyException;
import com.exametrika.common.rawdb.impl.RawTransactionLog.FlushInfo;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList.Element;



/**
 * The {@link RawPage} is an implementation of {@link IRawPage}.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawPage implements IRawPage
{
    private int size;
    private long index;
    private RawDataFile file;
    private RawPageCache pageCache;
    private final RawPageManager pageManager;
    private final RawTransactionManager transactionManager;
    private final Element<RawPage> element = new Element<RawPage>(this);
    private final Element<RawPage> writeElement = new Element<RawPage>(this);
    private final Element<RawPage> committedElement = new Element<RawPage>(this);
    private RawPageProxy proxy;
    private RawRegion readRegion;
    private IRawWriteRegion writeRegion;
    private RawRegion savedRegion;
    private RawRegion region;
    private long lastAccessTime;
    private boolean stale;
    private boolean flushed;
    private boolean cached;
    private IRawPageData data;
    private int refreshIndex;

    public RawPage(int size, long index, RawDataFile file, RawRegion region, boolean cached, RawPageProxy proxy)
    {
        init(size, index, file, region, cached, proxy);

        this.pageManager = file.getDatabase().getPageManager();
        this.transactionManager = file.getDatabase().getTransactionManager();
    }

    public void init(int size, long index, RawDataFile file, RawRegion region, boolean cached, RawPageProxy proxy)
    {
        Assert.notNull(file);
        Assert.notNull(region);
        Assert.isTrue(region.isReadOnly());
        Assert.isTrue(region.getLength() == size);
        
        this.size = size;
        this.index = index;
        this.file = file;
        this.readRegion = region;
        this.region = region;
        this.savedRegion = region;
        this.lastAccessTime = file.getDatabase().getCurrentTime();
        this.stale = false;
        this.flushed = false;
        this.cached = cached;
        this.pageCache = file.getPageCache();
        this.refreshIndex = pageCache.getRefreshIndex();
        
        if (proxy != null)
            this.proxy = proxy;
        else
            this.proxy = createProxy();
    }

    public int getRefreshIndex()
    {
        return refreshIndex;
    }
    
    public Element<RawPage> getElement()
    {
        return element;
    }
    
    public Element<RawPage> getWriteElement()
    {
        return writeElement;
    }
    
    public Element<RawPage> getCommittedElement()
    {
        return committedElement;
    }
    
    public boolean isModified()
    {
        return writeRegion != null;
    }

    public boolean isFlushing()
    {
        return savedRegion.isFlushing();
    }
    
    public long getLastAccessTime()
    {
        return lastAccessTime;
    }
    
    public void setLastAccessTime(long time)
    {
        lastAccessTime = time;
    }
    
    public RawPageProxy getProxy()
    {
        return proxy;
    }
    
    public boolean isCached()
    {
        return cached;
    }
    
    public void setStale()
    {
        if (stale)
            return;

        if (data != null)
        {
            data.onUnloaded();
            data = null;
        }
        
        if (writeRegion != null)
            pageCache.releaseRegion(((RawRegion)writeRegion).toReadRegion());
        
        pageCache.releaseRegion(region);
        
        if (savedRegion != region)
            pageCache.releaseRegion(savedRegion);
        
        stale = true;
        file = null;
        readRegion = null;
        writeRegion = null;
        savedRegion = null;
        region = null;
        proxy.setUnloaded();
        proxy = null;
    }
    
    @Override
    public int getSize()
    {
        return size;
    }

    @Override
    public long getIndex()
    {
        return index;
    }

    @Override
    public RawDataFile getFile()
    {
        return file;
    }

    @Override
    public boolean isReadOnly()
    {
        IRawTransaction transaction = transactionManager.getTransaction();
        return stale || file.isReadOnly() || transaction == null || transaction.isReadOnly();
    }
    
    @Override
    public boolean isStale()
    {
        if (stale || refreshIndex == pageCache.getRefreshIndex())
            return stale;

        return refreshStale();
    }

    public void refresh()
    {
        Assert.checkState(!isStale());
    }

    @Override
    public IRawPageData getData()
    {
        return data;
    }

    @Override
    public void setData(IRawPageData data)
    {
        this.data = data;
    }

    @Override
    public RawRegion getReadRegion()
    {
        return readRegion;
    }
    
    @Override
    public IRawWriteRegion getWriteRegion()
    {
        if (writeRegion != null)
            return writeRegion;

        return createWriteRegion();
    }

    @Override
    public String toString()
    {
        if (!stale)
            return file.toString() + "[" + index + "]";
        else
            return "[stale]";
    }

    public FlushInfo flush(boolean redo)
    {
        if (!flushed)
        {
            RawRegion oldSavedRegion = savedRegion;
            
            oldSavedRegion.setFlushing(true, true);
            pageCache.releaseRegion(oldSavedRegion);
            
            Assert.checkState(region.isReadOnly());
            region.setFlushing(false, true);
            savedRegion = region;
            
            FlushInfo flushInfo = new FlushInfo(file, index, oldSavedRegion, savedRegion, false);
            flushed = true;
            return flushInfo;
        }
        else if (!redo)
        {
            savedRegion.setFlushing(false, true);
            return new FlushInfo(file, index, savedRegion, savedRegion, true);
        }
        else
            return null;
    }
    
    public void commit()
    {
        Assert.checkState(writeRegion != null);
        
        if (data != null)
            data.onBeforeCommitted();
        
        if (region != savedRegion)
            pageCache.releaseRegion(region);
        region = ((RawRegion)writeRegion).toReadRegion();

        writeRegion = null;
        readRegion = region;
        flushed = false;
        
        if (data != null)
            data.onCommitted();
    }
    
    public void rollback()
    {
        pageCache.releaseRegion(((RawRegion)writeRegion).toReadRegion());
        writeRegion = null;
        readRegion = region;
        
        if (data != null)
            data.onRolledBack();
    }
    
    public void migrate(RawPageCache pageCache)
    {
        if (writeRegion != null)
        {
            ((RawRegion)writeRegion).setFree();
            ((RawRegion)writeRegion).setUsed(pageCache);
        }
        
        region.setFree();
        region.setUsed(pageCache);
        
        if (savedRegion != region)
        {
            savedRegion.setFree();
            savedRegion.setUsed(pageCache);
        }
        
        this.pageCache = pageCache;
    }
    
    private RawPageProxy createProxy()
    {
        RawPageProxyCache proxyCache = file.getDatabase().getProxyCache();
        
        if (cached)
        {
            RawPageProxy proxy = proxyCache.get(file.getIndex(), index);
            if (proxy != null)
            {
                proxy.init(this);
                return proxy;
            }
            
            proxy = new RawPageProxy(this);
            proxyCache.put(file.getIndex(), index, proxy);
            return proxy;
        }
        else
            return new RawPageProxy(this);
    }

    private IRawWriteRegion createWriteRegion()
    {
        refresh();
        
        if (isReadOnly())
            throw new RawTransactionReadOnlyException();
        
        writeRegion = region.toWriteRegion(pageCache);
        
        readRegion = (RawRegion)writeRegion;

        pageManager.addWritePage(this);
        
        return writeRegion;
    }
    
    private boolean refreshStale()
    {
        if (!stale && cached)
        {
            refreshIndex = pageCache.renewPage(this, true);
            return false;
        }
        else
            return stale;
    }
}
