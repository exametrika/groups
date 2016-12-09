/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.nio.ByteBuffer;

import sun.misc.Unsafe;

import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.config.RawPageTypeConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Classes;



/**
 * The {@link RawHeapReadRegion} is an implementation of {@link IRawReadRegion} that uses heap memory.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class RawHeapReadRegion extends RawRegion
{
    protected static final Unsafe unsafe = Classes.getUnsafe();
    protected static final long byteArrayOffset = unsafe.arrayBaseOffset(byte[].class);
    protected static final long charArrayOffset = unsafe.arrayBaseOffset(char[].class);
    protected static final long shortArrayOffset = unsafe.arrayBaseOffset(short[].class);
    protected static final long intArrayOffset = unsafe.arrayBaseOffset(int[].class);
    protected static final long longArrayOffset = unsafe.arrayBaseOffset(long[].class);
    protected static final long floatArrayOffset = unsafe.arrayBaseOffset(float[].class);
    protected static final long doubleArrayOffset = unsafe.arrayBaseOffset(double[].class);
    protected byte[] buffer;
    protected final RawHeapReadRegion parent;

    public RawHeapReadRegion(byte[] buffer, int offset, int length)
    {
        this(0, 0, buffer, offset, length, null);
    }
    
    public RawHeapReadRegion(int fileIndex, long pageIndex, byte[] buffer, int offset, int length)
    {
        this(fileIndex, pageIndex, buffer, offset, length, null);
    }
    
    @Override
    public ByteBuffer getBuffer()
    {
        return ByteBuffer.wrap(buffer, offset, length);
    }

    @Override
    public void init()
    {
        Assert.checkState(parent == null && offset == 0);
        
        int baseSize = RawPageTypeConfiguration.MIN_PAGE_SIZE;
        for (int i = 0; i < length; i += baseSize)
            unsafe.copyMemory(null, initBuffer.address(), buffer, byteArrayOffset + i, baseSize);
    }
    
    @Override
    public IRawWriteRegion toWriteRegion(RawPageCache pageCache)
    {
        Assert.checkState(parent == null);
        
        RawHeapReadRegion region = (RawHeapReadRegion)pageCache.acquireRegion(fileIndex, pageIndex, false, false, null);
        if (!(region instanceof RawHeapWriteRegion))
        {
            region.pageCache = null;
            region = new RawHeapWriteRegion(fileIndex, pageIndex, region.buffer, 0, length);
            region.pageCache = pageCache;
        }
        
        System.arraycopy(this.buffer, 0, region.buffer, 0, length);
        return (IRawWriteRegion)region;
    }
    
    @Override
    public RawRegion toReadRegion()
    {
        Assert.checkState(parent == null);
        RawHeapReadRegion region = new RawHeapReadRegion(fileIndex, pageIndex, buffer, 0, length);
        region.pageCache = pageCache;
        pageCache = null;
        return region;
    }
    
    @Override
    public void clear()
    {
        buffer = null;
        length = 0;
    }
    
    @Override
    public boolean isReadOnly()
    {
        return true;
    }
    
    @Override
    public boolean isNative()
    {
        return false;
    }

    @Override
    public <T extends IRawReadRegion> T getParent()
    {
        return (T)parent;
    }
    
    @Override
    public <T extends IRawReadRegion> T getRegion(int offset, int length)
    {
        if (offset < 0 || length < 0 || offset + length > this.length)
            throw new IndexOutOfBoundsException();
        
        return (T)new RawHeapReadRegion(fileIndex, pageIndex, buffer, this.offset + offset, length, parent != null ? parent : this);
    }

    @Override
    public final byte readByte(int index)
    {
        if (index < 0 || index + 1 > length)
            throw new IndexOutOfBoundsException();
        
        return unsafe.getByte(buffer, byteArrayOffset + offset + index);
    }

    @Override
    public final ByteArray readByteArray(int index, int length)
    {
        if (index < 0 || length < 0 || index + length > this.length)
            throw new IndexOutOfBoundsException();
        
        return new ByteArray(buffer, offset + index, length);
    }

    @Override
    public void readByteArray(int index, byte[] value, int offset, int length)
    {
        if (index < 0 || length < 0 || offset < 0 || index + length > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        
        unsafe.copyMemory(buffer, byteArrayOffset + this.offset + index, value, byteArrayOffset + offset, length);
    }

    @Override
    public final char readChar(int index)
    {
        if (index < 0 || index + 2 > length)
            throw new IndexOutOfBoundsException();
        
        return unsafe.getChar(buffer, byteArrayOffset + offset + index);
    }

    @Override
    public void readCharArray(int index, char[] value, int offset, int length)
    {
        int l = length << 1;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        
        unsafe.copyMemory(buffer, byteArrayOffset + this.offset + index, value, charArrayOffset + (offset << 1), l);
    }

    @Override
    public final String readString(int index, int length)
    {
        char[] values = new char[length];
        readCharArray(index, values, 0, length);
        return new String(values);
    }

    @Override
    public final short readShort(int index)
    {
        if (index < 0 || index + 2 > length)
            throw new IndexOutOfBoundsException();
        
        return unsafe.getShort(buffer, byteArrayOffset + offset + index);
    }

    @Override
    public void readShortArray(int index, short[] value, int offset, int length)
    {
        int l = length << 1;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        
        unsafe.copyMemory(buffer, byteArrayOffset + this.offset + index, value, shortArrayOffset + (offset << 1), l);
    }

    @Override
    public final int readInt(int index)
    {
        if (index < 0 || index + 4 > length)
            throw new IndexOutOfBoundsException();
        
        return unsafe.getInt(buffer, byteArrayOffset + offset + index);
    }

    @Override
    public void readIntArray(int index, int[] value, int offset, int length)
    {
        int l = length << 2;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        
        unsafe.copyMemory(buffer, byteArrayOffset + this.offset + index, value, intArrayOffset + (offset << 2), l);
    }

    @Override
    public final long readLong(int index)
    {
        if (index < 0 || index + 8 > length)
            throw new IndexOutOfBoundsException();
        
        return unsafe.getLong(buffer, byteArrayOffset + offset + index);
    }

    @Override
    public void readLongArray(int index, long[] value, int offset, int length)
    {
        int l = length << 3;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        
        unsafe.copyMemory(buffer, byteArrayOffset + this.offset + index, value, longArrayOffset + (offset << 3), l);
    }

    @Override
    public final float readFloat(int index)
    {
        if (index < 0 || index + 4 > length)
            throw new IndexOutOfBoundsException();
        
        return unsafe.getFloat(buffer, byteArrayOffset + offset + index);
    }

    @Override
    public void readFloatArray(int index, float[] value, int offset, int length)
    {
        int l = length << 2;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        
        unsafe.copyMemory(buffer, byteArrayOffset + this.offset + index, value, floatArrayOffset + (offset << 2), l);
    }

    @Override
    public final double readDouble(int index)
    {
        if (index < 0 || index + 8 > length)
            throw new IndexOutOfBoundsException();
        
        return unsafe.getDouble(buffer, byteArrayOffset + offset + index);
    }

    @Override
    public void readDoubleArray(int index, double[] value, int offset, int length)
    {
        int l = length << 3;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        
        unsafe.copyMemory(buffer, byteArrayOffset + this.offset + index, value, doubleArrayOffset + (offset << 3), l);
    }

    @Override
    public String toString()
    {
        return "[file: " + fileIndex + ", page: " + pageIndex + ", offset: " + offset + ", length: " + length + ", read, heap]";
    }
    
    protected RawHeapReadRegion(int fileIndex, long pageIndex, byte[] buffer, int offset, int length, RawHeapReadRegion parent)
    {
        super(fileIndex, pageIndex, offset, length);
        
        Assert.notNull(buffer);
        Assert.isTrue(offset >= 0 && length >= 0);
        Assert.isTrue(offset + length <= buffer.length);
        
        this.buffer = buffer;
        this.parent = parent;
    }
}
