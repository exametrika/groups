/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.utils.Assert;



/**
 * The {@link RawPageSerialization} is an implementation of {@link IDataSerialization} based on pages.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawPageSerialization extends RawAbstractBlockSerialization
{
    private final IRawTransaction transaction;
    private final int fileIndex;
    private long pageIndex;

    public RawPageSerialization(IRawTransaction transaction, int fileIndex, IRawPage page, int pageOffset)
    {
        super(0, pageOffset);
        
        Assert.notNull(transaction);
        Assert.isTrue(!transaction.isReadOnly());
        
        this.transaction = transaction;
        this.fileIndex = fileIndex;
        setPosition(page, pageOffset);
    }
    
    public RawPageSerialization(IRawTransaction transaction, int fileIndex, long pageIndex, int pageOffset)
    {
        super(0, pageOffset);
        
        Assert.notNull(transaction);
        Assert.isTrue(!transaction.isReadOnly());
        
        this.transaction = transaction;
        this.fileIndex = fileIndex;
        setPosition(pageIndex, pageOffset);
    }

    public IRawTransaction getTransaction()
    {
        return transaction;
    }
    
    public int getPageSize()
    {
        return blockSize;
    }
    
    public int getFileIndex()
    {
        return fileIndex;
    }
    
    public long getPageIndex()
    {
        return pageIndex;
    }
    
    public int getPageOffset()
    {
        return blockOffset;
    }
    
    public long getFileOffset()
    {
        return pageIndex * blockSize + blockOffset;
    }

    public void setPosition(long pageIndex, int pageOffset)
    {
        if (getRegion() != null && this.pageIndex == pageIndex)
            this.blockOffset = pageOffset;
        else
            setPosition(transaction.getPage(fileIndex, pageIndex), pageOffset);
    }
    
    public void setPosition(IRawPage page, int pageOffset)
    {
        Assert.notNull(page);
        
        blockSize = page.getSize();
        
        IRawReadRegion region = page.getWriteRegion();
        setRegion(region);
        Assert.checkState(blockSize <= region.getLength());

        this.pageIndex = page.getIndex();
        this.blockOffset = pageOffset;
    }

    @Override
    protected void nextReadRegion()
    {
        pageIndex++;
        blockOffset = 0;
        
        IRawReadRegion region = transaction.getPage(fileIndex, pageIndex).getWriteRegion();
        setRegion(region);
        Assert.checkState(blockSize <= region.getLength());
    }
    
    @Override
    protected void nextWriteRegion()
    {
        nextReadRegion();
    }
    
    @Override
    protected void debug(int offset, int length)
    {
        RawDbDebug.debug(fileIndex, pageIndex, offset, length);
    }
}
