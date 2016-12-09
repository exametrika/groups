/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;



/**
 * The {@link RawHeapWriteRegion} is an implementation of {@link IRawWriteRegion} that uses heap memory.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class RawHeapWriteRegion extends RawHeapReadRegion implements IRawWriteRegion
{
    private static final boolean DEBUG = RawDbDebug.DEBUG;
    
    public RawHeapWriteRegion(byte[] buffer, int offset, int length)
    {
        this(0, 0, buffer, offset, length, null);
    }
    
    public RawHeapWriteRegion(int fileIndex, long pageIndex, byte[] buffer, int offset, int length)
    {
        this(fileIndex, pageIndex, buffer, offset, length, null);
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
        
        return (T)new RawHeapWriteRegion(fileIndex, pageIndex, buffer, this.offset + offset, length, 
            parent != null ? (RawHeapWriteRegion)parent : this);
    }

    @Override
    public void writeByte(int index, byte value)
    {
        if (index < 0 || index + 1 > length)
            throw new IndexOutOfBoundsException();
        
        if (DEBUG)
            debug(offset + index, 1);
        
        unsafe.putByte(buffer, byteArrayOffset + offset + index, value);
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
        
        if (DEBUG)
            debug(offset + index, length);
        
        unsafe.copyMemory(value, byteArrayOffset + offset, buffer, byteArrayOffset + this.offset + index, length);
    }
    
    @Override
    public void writeChar(int index, char value)
    {
        if (index < 0 || index + 2 > length)
            throw new IndexOutOfBoundsException();
        
        if (DEBUG)
            debug(offset + index, 2);
        
        unsafe.putChar(buffer, byteArrayOffset + offset + index, value);
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
        
        if (DEBUG)
            debug(this.offset + index, l);
        
        unsafe.copyMemory(value, charArrayOffset + (offset << 1), buffer, byteArrayOffset + this.offset + index, l);
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
        
        if (DEBUG)
            debug(offset + index, 2);
        
        unsafe.putShort(buffer, byteArrayOffset + offset + index, value);
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
        
        if (DEBUG)
            debug(this.offset + index, l);
        
        unsafe.copyMemory(value, shortArrayOffset + (offset << 1), buffer, byteArrayOffset + this.offset + index, l);
    }
    
    @Override
    public void writeInt(int index, int value)
    {
        if (index < 0 || index + 4 > length)
            throw new IndexOutOfBoundsException();
        
        if (DEBUG)
            debug(offset + index, 4);
        
        unsafe.putInt(buffer, byteArrayOffset + offset + index, value);
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
        
        if (DEBUG)
            debug(this.offset + index, l);
        
        unsafe.copyMemory(value, intArrayOffset + (offset << 2), buffer, byteArrayOffset + this.offset + index, l);
    }

    @Override
    public void writeLong(int index, long value)
    {
        if (index < 0 || index + 8 > length)
            throw new IndexOutOfBoundsException();
        
        if (DEBUG)
            debug(offset + index, 8);
        
        unsafe.putLong(buffer, byteArrayOffset + offset + index, value);
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
        
        if (DEBUG)
            debug(this.offset + index, l);
        
        unsafe.copyMemory(value, longArrayOffset + (offset << 3), buffer, byteArrayOffset + this.offset + index, l);
    }

    @Override
    public void writeFloat(int index, float value)
    {
        if (index < 0 || index + 4 > length)
            throw new IndexOutOfBoundsException();
        
        if (DEBUG)
            debug(offset + index, 4);
        
        unsafe.putFloat(buffer, byteArrayOffset + offset + index, value);
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
        
        if (DEBUG)
            debug(this.offset + index, l);
        
        unsafe.copyMemory(value, floatArrayOffset + (offset << 2), buffer, byteArrayOffset + this.offset + index, l);
    }

    @Override
    public void writeDouble(int index, double value)
    {
        if (index < 0 || index + 8 > length)
            throw new IndexOutOfBoundsException();
        
        if (DEBUG)
            debug(offset + index, 8);
        
        unsafe.putDouble(buffer, byteArrayOffset + offset + index, value);
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
        
        if (DEBUG)
            debug(this.offset + index, l);
        
        unsafe.copyMemory(value, doubleArrayOffset + (offset << 3), buffer, byteArrayOffset + this.offset + index, l);
    }
    
    @Override
    public void fill(int index, int length, byte value)
    {
        if (index < 0 || length < 0 || index + length > this.length)
            throw new IndexOutOfBoundsException();
        
        if (DEBUG)
            debug(offset + index, length);
        
        unsafe.setMemory(buffer, byteArrayOffset + offset + index, length, value);
    }
    
    @Override
    public void copy(int fromIndex, int toIndex, int length)
    {
        Assert.checkState(offset == 0);
        
        if (DEBUG)
            debug(toIndex, length);
        
        System.arraycopy(buffer, fromIndex, buffer, toIndex, length);
    }

    @Override
    public String toString()
    {
        return "[file: " + fileIndex + ", page: " + pageIndex + ", offset: " + offset + ", length: " + length + ", write, heap]";
    }

    protected RawHeapWriteRegion(int fileIndex, long pageIndex, byte[] buffer, int offset, int length, RawHeapWriteRegion parent)
    {
        super(fileIndex, pageIndex, buffer, offset, length, parent);
    }

    protected void debug(int offset, int length)
    {
        RawDbDebug.debug(fileIndex, pageIndex, offset, length);
    }
}
