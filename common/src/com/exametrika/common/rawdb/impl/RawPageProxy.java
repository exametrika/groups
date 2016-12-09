/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import com.exametrika.common.rawdb.IRawDataFile;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawPageData;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;



/**
 * The {@link RawPageProxy} is an proxy implementation of {@link IRawPage}.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawPageProxy implements IRawPage
{
    private final int fileIndex;
    private final long pageIndex;
    private RawTransactionManager pageLoader;
    private RawPage page;

    public RawPageProxy(RawPage page)
    {
        Assert.notNull(page);
        
        this.fileIndex = page.getFile().getIndex();
        this.pageIndex = page.getIndex();
        if (page.isCached())
            pageLoader = page.getFile().getDatabase().getTransactionManager();
        else
            pageLoader = null;
        
        this.page = page;
    }
    
    public boolean isLoaded()
    {
        return page != null;
    }
    
    @Override
    public boolean isStale()
    {
        return page == null && pageLoader == null;
    }
    
    public RawPage getPage()
    {
        if (page != null && !page.isStale())
            return page;
        else
            return refreshPage();
    }
    
    public void init(RawPage page)
    {
        Assert.notNull(page);
        Assert.checkState(this.page == null);

        this.page = page;
    }
    
    public void setUnloaded()
    {
        page = null;
    }
    
    public void setStale()
    {
        page = null;
        pageLoader = null;
    }
    
    @Override
    public int getSize()
    {
        IRawPage page = getPage();
        return page.getSize();
    }

    @Override
    public long getIndex()
    {
        IRawPage page = getPage();
        return page.getIndex();
    }

    @Override
    public IRawDataFile getFile()
    {
        IRawPage page = getPage();
        return page.getFile();
    }

    @Override
    public boolean isReadOnly()
    {
        IRawPage page = getPage();
        return page.isReadOnly();
    }

    @Override
    public IRawPageData getData()
    {
        IRawPage page = getPage();
        return page.getData();
    }

    @Override
    public void setData(IRawPageData data)
    {
        IRawPage page = getPage();
        page.setData(data);
    }

    @Override
    public IRawReadRegion getReadRegion()
    {
        IRawPage page = getPage();
        return page.getReadRegion();
    }

    @Override
    public IRawWriteRegion getWriteRegion()
    {
        IRawPage page = getPage();
        return page.getWriteRegion();
    }
    
    @Override
    public String toString()
    {
        IRawPage page = getPage();
        return page.toString();
    }
    
    private RawPage refreshPage()
    {
        Assert.checkState(pageLoader != null);
        page = pageLoader.getTransaction().getPageFromFile(fileIndex, pageIndex, this);
        Assert.checkState(page.getProxy() == this);
        return page;
    }
}
