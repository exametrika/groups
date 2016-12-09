/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.nio.ByteBuffer;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;




/**
 * The {@link RawRegionAllocator} is a allocator of regions.
 * 
 * @threadsafety This class and its methods are thread not safe.
 * @author Medvedev-A
 */
public final class RawRegionAllocator
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final boolean nativeMemory;
    private long allocatedSize;
    private long allocationsCount;
    private long allocationsSize;
    private long freeCount;
    private long freeSize;
    private final int interceptId;
    
    public RawRegionAllocator(boolean nativeMemory, int interceptId)
    {
        this.nativeMemory = nativeMemory;
        this.interceptId = interceptId;
    }
    
    public <T extends RawRegion> T allocate(int fileIndex, long pageIndex, boolean readOnly, int length)
    {
        allocationsCount++;
        allocationsSize += length;
        allocatedSize += length;
        
        RawDatabaseInterceptor.INSTANCE.onRegionAllocated(interceptId,
            length);
        
        if (nativeMemory)
        {
            ByteBuffer buffer = ByteBuffer.allocateDirect(length);
            
            if (readOnly)
                return (T)new RawNativeReadRegion(fileIndex, pageIndex, buffer, 0, length);
            else
                return (T)new RawNativeWriteRegion(fileIndex, pageIndex, buffer, 0, length);
        }
        else
        {
            byte[] buffer = new byte[length];
            
            if (readOnly)
                return (T)new RawHeapReadRegion(fileIndex, pageIndex, buffer, 0, length);
            else
                return (T)new RawHeapWriteRegion(fileIndex, pageIndex, buffer, 0, length);
        }
    }
    
    public void free(RawRegion region)
    {
        if (region == null)
            return;
        
        int length = region.getLength();
        
        freeCount++;
        freeSize += length;
        allocatedSize -= length;
        
        region.clear();
        
        RawDatabaseInterceptor.INSTANCE.onRegionFreed(interceptId,
            length);
    }
    
    public String printStatistics()
    {
        return messages.statistics(allocatedSize, allocationsCount, allocationsSize, freeCount, freeSize).toString();
    }

    private interface IMessages
    {
        @DefaultMessage("allocated size: {0}, allocations count: {1}, allocations size: {2}, free count: {3}, free size: {4}")
        ILocalizedMessage statistics(long allocatedSize, long allocationsCount, long allocationsSize, long freeCount, long freeSize);
    }
}
