/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.nio.ByteBuffer;

import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;



/**
 * The {@link RawNativeWriteRegion} is an implementation of {@link IRawWriteRegion} that uses native non-heap memory.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawNativeWriteRegion extends RawNativeReadRegion implements IRawWriteRegion
{
    private static final boolean DEBUG = RawDbDebug.DEBUG;
    
    public RawNativeWriteRegion(ByteBuffer buffer, int offset, int length)
    {
        this(0, 0, buffer, offset, length);
    }
    
    public RawNativeWriteRegion(int fileIndex, long pageIndex, ByteBuffer buffer, int offset, int length)
    {
        this(fileIndex, pageIndex, offset, length, createRef(buffer, offset, length), null);
    }
    
    @Override
    public boolean isReadOnly()
    {
        return false;
    }

    @Override
    public <T extends IRawReadRegion> T getRegion(int offset, int length)
    {
        if (offset < 0 || length < 0 || offset + length > this.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.buffer != null);
        
        return (T)new RawNativeWriteRegion(fileIndex, pageIndex, this.offset + offset, length, ref, 
            parent != null ? (RawNativeWriteRegion)parent : this);
    }

    @Override
    public void writeByte(int index, byte value)
    {
        if (index < 0 || index + 1 > length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(offset + index, 1);
        
        unsafe.putByte(ref.address + offset + index, value);
    }

    @Override
    public void writeByteArray(int index, ByteArray value)
    {
        writeByteArray(index, value.getBuffer(), value.getOffset(), value.getLength());
    }
    
    @Override
    public void writeByteArray(int index, byte[] value)
    {
        writeByteArray(index, value, 0, value.length);
    }

    @Override
    public void writeByteArray(int index, byte[] value, int offset, int length)
    {
        if (index < 0 || length < 0 || offset < 0 || index + length > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(offset + index, length);
        
        unsafe.copyMemory(value, byteArrayOffset + offset, null, ref.address + this.offset + index, length);
    }
    
    @Override
    public void writeChar(int index, char value)
    {
        if (index < 0 || index + 2 > length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(offset + index, 2);
        
        unsafe.putChar(ref.address + offset + index, value);
    }

    @Override
    public void writeCharArray(int index, char[] value)
    {
        writeCharArray(index, value, 0, value.length);
    }

    @Override
    public void writeCharArray(int index, char[] value, int offset, int length)
    {
        int l = length << 1;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(this.offset + index, l);
        
        unsafe.copyMemory(value, charArrayOffset + (offset << 1), null, ref.address + this.offset + index, l);
    }

    @Override
    public void writeString(int index, String value)
    {
        writeCharArray(index, value.toCharArray());
    }

    @Override
    public void writeShort(int index, short value)
    {
        if (index < 0 || index + 2 > length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(offset + index, 2);
        
        unsafe.putShort(ref.address + offset + index, value);
    }

    @Override
    public void writeShortArray(int index, short[] value)
    {
        writeShortArray(index, value, 0, value.length);
    }

    @Override
    public void writeShortArray(int index, short[] value, int offset, int length)
    {
        int l = length << 1;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(this.offset + index, l);
        
        unsafe.copyMemory(value, shortArrayOffset + (offset << 1), null, ref.address + this.offset + index, l);
    }
    
    @Override
    public void writeInt(int index, int value)
    {
        if (index < 0 || index + 4 > length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(offset + index, 4);
        
        unsafe.putInt(ref.address + offset + index, value);
    }

    @Override
    public void writeIntArray(int index, int[] value)
    {
        writeIntArray(index, value, 0, value.length);
    }

    @Override
    public void writeIntArray(int index, int[] value, int offset, int length)
    {
        int l = length << 2;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(this.offset + index, l);
        
        unsafe.copyMemory(value, intArrayOffset + (offset << 2), null, ref.address + this.offset + index, l);
    }

    @Override
    public void writeLong(int index, long value)
    {
        if (index < 0 || index + 8 > length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(offset + index, 8);
        
        unsafe.putLong(ref.address + offset + index, value);
    }

    @Override
    public void writeLongArray(int index, long[] value)
    {
        writeLongArray(index, value, 0, value.length);
    }

    @Override
    public void writeLongArray(int index, long[] value, int offset, int length)
    {
        int l = length << 3;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(this.offset + index, l);
        
        unsafe.copyMemory(value, longArrayOffset + (offset << 3), null, ref.address + this.offset + index, l);
    }

    @Override
    public void writeFloat(int index, float value)
    {
        if (index < 0 || index + 4 > length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(offset + index, 4);
        
        unsafe.putFloat(ref.address + offset + index, value);
    }

    @Override
    public void writeFloatArray(int index, float[] value)
    {
        writeFloatArray(index, value, 0, value.length);
    }

    @Override
    public void writeFloatArray(int index, float[] value, int offset, int length)
    {
        int l = length << 2;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(this.offset + index, l);
        
        unsafe.copyMemory(value, floatArrayOffset + (offset << 2), null, ref.address + this.offset + index, l);
    }

    @Override
    public void writeDouble(int index, double value)
    {
        if (index < 0 || index + 8 > length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(offset + index, 8);
        
        unsafe.putDouble(ref.address + offset + index, value);
    }

    @Override
    public void writeDoubleArray(int index, double[] value)
    {
        writeDoubleArray(index, value, 0, value.length);
    }

    @Override
    public void writeDoubleArray(int index, double[] value, int offset, int length)
    {
        int l = length << 3;
        if (index < 0 || length < 0 || offset < 0 || index + l > this.length || offset + length > value.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(this.offset + index, l);
        
        unsafe.copyMemory(value, doubleArrayOffset + (offset << 3), null, ref.address + this.offset + index, l);
    }
    
    @Override
    public void fill(int index, int length, byte value)
    {
        if (index < 0 || length < 0 || index + length > this.length)
            throw new IndexOutOfBoundsException();
        Assert.checkState(ref != null && ref.address != 0);
        
        if (DEBUG)
            debug(offset + index, length);
        
        unsafe.setMemory(ref.address + offset + index, length, value);
    }
    
    @Override
    public void copy(int fromIndex, int toIndex, int length)
    {
        if (fromIndex < 0 || toIndex < 0 || length < 0 || fromIndex + length > this.length || toIndex + length > this.length)
            throw new IndexOutOfBoundsException();
        
        Assert.checkState(ref != null && ref.address != 0 && offset == 0);
        
        if (DEBUG)
            debug(toIndex, length);
        
        unsafe.copyMemory(ref.address + fromIndex, ref.address + toIndex, length);
    }

    @Override
    public String toString()
    {
        return "[file: " + fileIndex + ", page: " + pageIndex + ", offset: " + offset + ", length: " + length + ", write, native]";
    }

    protected RawNativeWriteRegion(int fileIndex, long pageIndex, int offset, int length, Ref ref, RawNativeWriteRegion parent)
    {
        super(fileIndex, pageIndex, offset, length, ref, parent);
    }

    protected void debug(int offset, int length)
    {
        RawDbDebug.debug(fileIndex, pageIndex, offset, length);
    }
}
