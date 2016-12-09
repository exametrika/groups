/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.impl;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import sun.misc.Unsafe;

import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.io.SerializationException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Classes;


/**
 * The {@link DataSerialization} is an implementation of {@link IDataSerialization}.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class DataSerialization implements IDataSerialization
{
    protected static final Unsafe unsafe = Classes.getUnsafe();
    protected static final long byteArrayOffset = unsafe.arrayBaseOffset(byte[].class);
    private final ByteOutputStream stream;
    protected Map<UUID, Object> extensions = new HashMap<UUID, Object>();

    /**
     * Creates a new object.
     *
     * @param outputStream output stream to serialize into
     */
    public DataSerialization(ByteOutputStream outputStream)
    {
        Assert.notNull(outputStream);

        this.stream = outputStream;
    }
    
    @Override
    public <T> T getExtension(UUID id)
    {
        return (T)extensions.get(id);
    }
    
    @Override
    public <T> void setExtension(UUID id, T value)
    {
        extensions.put(id, value);
    }

    @Override
    public final void write(ByteArray value)
    {
        Assert.notNull(value);
        
        int newLength = stream.length + value.getLength();
        if (newLength > stream.buffer.length)
            stream.growBuffer(newLength);
        
        System.arraycopy(value.getBuffer(), value.getOffset(), stream.buffer, stream.length, value.getLength());
        stream.length = newLength;
    }
    
    @Override
    public final void writeByte(byte value)
    {
        int newLength = stream.length + 1;
        if (newLength > stream.buffer.length)
            stream.growBuffer(newLength);
        
        unsafe.putByte(stream.buffer, byteArrayOffset + stream.length, value);
        stream.length = newLength;
    }
    
    @Override
    public final void writeChar(char value)
    {
        int newLength = stream.length + 2;
        if (newLength > stream.buffer.length)
            stream.growBuffer(newLength);
        
        unsafe.putChar(stream.buffer, byteArrayOffset + stream.length, value);
        stream.length = newLength;
    }
    
    @Override
    public final void writeShort(short value)
    {
        int newLength = stream.length + 2;
        if (newLength > stream.buffer.length)
            stream.growBuffer(newLength);
        
        unsafe.putShort(stream.buffer, byteArrayOffset + stream.length, value);
        stream.length = newLength;
    }
    
    @Override
    public final void writeInt(int value)
    {
        int newLength = stream.length + 4;
        if (newLength > stream.buffer.length)
            stream.growBuffer(newLength);
        
        unsafe.putInt(stream.buffer, byteArrayOffset + stream.length, value);
        stream.length = newLength;
    }
    
    @Override
    public final void writeLong(long value)
    {
        int newLength = stream.length + 8;
        if (newLength > stream.buffer.length)
            stream.growBuffer(newLength);
        
        unsafe.putLong(stream.buffer, byteArrayOffset + stream.length, value);
        stream.length = newLength;
    }
    
    @Override
    public final void writeBoolean(boolean value)
    {
        writeByte(value ? (byte)1 : (byte)0);   
    }
    
    @Override
    public final void writeFloat(float value)
    {
        int newLength = stream.length + 4;
        if (newLength > stream.buffer.length)
            stream.growBuffer(newLength);
        
        unsafe.putFloat(stream.buffer, byteArrayOffset + stream.length, value);
        stream.length = newLength;
    }
    
    @Override
    public final void writeDouble(double value)
    {
        int newLength = stream.length + 8;
        if (newLength > stream.buffer.length)
            stream.growBuffer(newLength);
        
        unsafe.putDouble(stream.buffer, byteArrayOffset + stream.length, value);
        stream.length = newLength;
    }

    @Override
    public final void writeByteArray(ByteArray value)
    {
        if (value != null)
        {
            // To distinguish null byte array from empty byte array non null byte array length increased by one,
            // i.e. null byte array length - 0, empty byte array length - 1, etc.
            writeInt(value.getLength() + 1);

            write(value);
        }
        else
            writeInt(0);
    }
    
    @Override
    public final void writeString(String value)
    {
        if (value != null)
        {
            try
            {
                byte[] buf = value.getBytes("UTF-8");
                writeByteArray(new ByteArray(buf));
            }
            catch (UnsupportedEncodingException e)
            {
                throw new SerializationException(e);
            }
        }
        else
            writeInt(0);
    }
}
