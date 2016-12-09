/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.nio.ByteBuffer;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.config.RawPageTypeConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Classes;



/**
 * The {@link RawNativeReadRegion} is an implementation of {@link IRawReadRegion} that uses native non-heap memory.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class RawNativeReadRegion extends RawRegion
{
    protected static final Unsafe unsafe = Classes.getUnsafe();
    protected static final long byteArrayOffset = unsafe.arrayBaseOffset(byte[].class);
    protected static final long charArrayOffset = unsafe.arrayBaseOffset(char[].class);
    protected static final long shortArrayOffset = unsafe.arrayBaseOffset(short[].class);
    protected static final long intArrayOffset = unsafe.arrayBaseOffset(int[].class);
    protected static final long longArrayOffset = unsafe.arrayBaseOffset(long[].class);
    protected static final long floatArrayOffset = unsafe.arrayBaseOffset(float[].class);
    protected static final long doubleArrayOffset = unsafe.arrayBaseOffset(double[].class);
    protected final RawNativeReadRegion parent;
    protected Ref ref;

    public RawNativeReadRegion(ByteBuffer buffer, int offset, int length)
    {
        this(0, 0, buffer, offset, length);
    }
    
    public RawNativeReadRegion(int fileIndex, long pageIndex, ByteBuffer buffer, int offset, int length)
    {
        this(fileIndex, pageIndex, offset, length, createRef(buffer, offset, length), null);
    }
    
    @Override
    public ByteBuffer getBuffer()
    {
        Assert.checkState(ref != null && ref.buffer != null);
        return ref.buffer.duplicate();
    }
    
    @Override
    public void init()
    {
        Assert.checkState(parent == null && offset == 0);
        Assert.checkState(ref != null && ref.address != 0);
        
        int baseSize = RawPageTypeConfiguration.MIN_PAGE_SIZE;
        for (int i = 0; i < length; i += baseSize)
            unsafe.copyMemory(initBuffer.address(), ref.address + i, baseSize);
    }
    
    @Override
    public IRawWriteRegion toWriteRegion(RawPageCache pageCache)
    {
        Assert.checkState(parent == null);
        Assert.checkState(ref != null && ref.buffer != null);

        ByteBuffer buffer;
        RawNativeReadRegion region = (RawNativeReadRegion)pageCache.acquireRegion(fileIndex, pageIndex, false, false, null);
        if (!(region instanceof RawNativeWriteRegion))
        {
            buffer = region.ref.buffer;
            Assert.checkState(buffer.limit() == length);
            region.ref = null;
            region.pageCache = null;
            
            region = new RawNativeWriteRegion(fileIndex, pageIndex, buffer, 0, length);
            region.pageCache = pageCache; 
        }
        else
            buffer = region.ref.buffer;
        
        long address = ((DirectBuffer)buffer).address();
        
        unsafe.copyMemory(ref.address, address, length);
        
        return (IRawWriteRegion)region;
    }

    @Override
    public RawRegion toReadRegion()
    {
        Assert.checkState(parent == null);
        RawRegion region = new RawNativeReadRegion(fileIndex, pageIndex, 0, length, ref, null);
        region.pageCache = pageCache;
        ref = null;
        pageCache = null;
        return region;
    }

    @Override
    public void clear()
    {
        Assert.checkState(ref != null);
        
        if (ref.buffer == null)
            return;
        
        ((DirectBuffer)ref.buffer).cleaner().clean();
        ref.buffer = null;
        ref.address = 0;
    }
    
    @Override
    public boolean isReadOnly()
    {
        return true;
    }
    
    @Override
    public boolean isNative()
    {
        return true;
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
        Assert.checkState(ref != null && ref.buffer != null);
        
        return (T)new RawNativeReadRegion(fileIndex, pageIndex, this.offset + offset, length, ref, parent != null ? parent : this);
    }

    @Override
    public final byte readByte(int index)
    {
        if (index < 0 || index + 1 > length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        return unsafe.getByte(ref.address + offset + index);
    }

    @Override
    public final ByteArray readByteArray(int index, int length)
    {
        if (index < 0 || length < 0 || index + length > this.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        byte[] value = new byte[length];
        
        unsafe.copyMemory(null, ref.address + this.offset + index, value, byteArrayOffset, length);
        
        return new ByteArray(value);
    }

    @Override
    public void readByteArray(int index, byte[] value, int offset, int length)
    {
        if (index < 0 || length < 0 || offset < 0 || index + length > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        unsafe.copyMemory(null, ref.address + this.offset + index, value, byteArrayOffset + offset, length);
    }

    @Override
    public final char readChar(int index)
    {
        if (index < 0 || index + 2 > length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        return unsafe.getChar(ref.address + offset + index);
    }

    @Override
    public void readCharArray(int index, char[] value, int offset, int length)
    {
        int l = length << 1;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        unsafe.copyMemory(null, ref.address + this.offset + index, value, charArrayOffset + (offset << 1), l);
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
        Assert.checkState(ref != null && ref.address != 0);
        
        return unsafe.getShort(ref.address + offset + index);
    }

    @Override
    public void readShortArray(int index, short[] value, int offset, int length)
    {
        int l = length << 1;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        unsafe.copyMemory(null, ref.address + this.offset + index, value, shortArrayOffset + (offset << 1), l);
    }

    @Override
    public final int readInt(int index)
    {
        if (index < 0 || index + 4 > length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        return unsafe.getInt(ref.address + offset + index);
    }

    @Override
    public void readIntArray(int index, int[] value, int offset, int length)
    {
        int l = length << 2;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        unsafe.copyMemory(null, ref.address + this.offset + index, value, intArrayOffset + (offset << 2), l);
    }

    @Override
    public final long readLong(int index)
    {
        if (index < 0 || index + 8 > length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        return unsafe.getLong(ref.address + offset + index);
    }

    @Override
    public void readLongArray(int index, long[] value, int offset, int length)
    {
        int l = length << 3;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        unsafe.copyMemory(null, ref.address + this.offset + index, value, longArrayOffset + (offset << 3), l);
    }

    @Override
    public final float readFloat(int index)
    {
        if (index < 0 || index + 4 > length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        return unsafe.getFloat(ref.address + offset + index);
    }

    @Override
    public void readFloatArray(int index, float[] value, int offset, int length)
    {
        int l = length << 2;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        unsafe.copyMemory(null, ref.address + this.offset + index, value, floatArrayOffset + (offset << 2), l);
    }

    @Override
    public final double readDouble(int index)
    {
        if (index < 0 || index + 8 > length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        return unsafe.getDouble(ref.address + offset + index);
    }

    @Override
    public void readDoubleArray(int index, double[] value, int offset, int length)
    {
        int l = length << 3;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        unsafe.copyMemory(null, ref.address + this.offset + index, value, doubleArrayOffset + (offset << 3), l);
    }

    @Override
    public String toString()
    {
        return "[file: " + fileIndex + ", page: " + pageIndex + ", offset: " + offset + ", length: " + length + ", read, native]";
    }
    
    protected static Ref createRef(ByteBuffer buffer, int offset, int length)
    {
        Assert.notNull(buffer);
        Assert.isTrue(offset >= 0 && length >= 0);
        Assert.isTrue(offset + length <= buffer.limit());
        Assert.isTrue(buffer.isDirect());
        Assert.isTrue(buffer.position() == 0);
        Assert.isTrue(buffer.limit() == buffer.capacity());
        
        long address = ((DirectBuffer)buffer).address();
        Assert.isTrue(address != 0);
        
        Ref ref = new Ref();
        ref.buffer = buffer;
        ref.address = address;
        
        return ref;
    }
    
    protected RawNativeReadRegion(int fileIndex, long pageIndex, int offset, int length, Ref ref, RawNativeReadRegion parent)
    {
        super(fileIndex, pageIndex, offset, length);
        
        Assert.notNull(ref);
        Assert.notNull(ref.buffer);
        Assert.isTrue(ref.address != 0);
        
        this.ref = ref;
        this.parent = parent;
    }
    
    protected static class Ref
    {
        public ByteBuffer buffer;
        public long address;
    }
}
