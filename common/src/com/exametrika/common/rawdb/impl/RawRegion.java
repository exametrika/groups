/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.nio.ByteBuffer;

import sun.nio.ch.DirectBuffer;

import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.config.RawPageTypeConfiguration;




/**
 * The {@link RawRegion} is a base abstract region.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public abstract class RawRegion implements IRawReadRegion
{
    protected static final DirectBuffer initBuffer = (DirectBuffer)ByteBuffer.allocateDirect(RawPageTypeConfiguration.MIN_PAGE_SIZE);
    protected int fileIndex;
    protected long pageIndex;
    protected final int offset;
    protected int length;
    private volatile boolean flushing;
    private volatile boolean savedFlushing;
    protected RawPageCache pageCache;
    
    public RawRegion(int fileIndex, long pageIndex, int offset, int length)
    {
        this.fileIndex = fileIndex;
        this.pageIndex = pageIndex;
        this.offset = offset;
        this.length = length;
    }
    
    public abstract ByteBuffer getBuffer();
    
    public abstract void init();
    
    public abstract IRawWriteRegion toWriteRegion(RawPageCache pageCache);
    
    public abstract RawRegion toReadRegion();
    
    public abstract void clear();
    
    public int getFileIndex()
    {
        return fileIndex;
    }
    
    public void setFileIndex(int fileIndex)
    {
        this.fileIndex = fileIndex;
    }
    
    public final long getPageIndex()
    {
        return pageIndex;
    }
    
    public final void setPageIndex(long pageIndex)
    {
        this.pageIndex = pageIndex;
    }
    
    public final boolean isFlushing()
    {
        return flushing || savedFlushing;
    }
    
    public final void setFlushing(boolean saved, boolean value)
    {
        if (saved)
            savedFlushing = value;
        else
            flushing = value;
    }

    public final boolean isFree()
    {
        return pageCache == null;
    }
    
    public final void setFree()
    {
        pageCache.decrementSize();
        pageCache = null;
    }
    
    public final void setUsed(RawPageCache pageCache)
    {
        this.pageCache = pageCache;
        pageCache.incrementSize();
    }
    
    @Override
    public int getOffset()
    {
        return offset;
    }
    
    @Override
    public final int getLength()
    {
        return length;
    }
}
