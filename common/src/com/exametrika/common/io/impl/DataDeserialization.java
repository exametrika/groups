/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.impl;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import sun.misc.Unsafe;

import com.exametrika.common.io.EndOfStreamException;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.SerializationException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Classes;



/**
 * The {@link DataDeserialization} is an implementation of {@link IDataDeserialization}.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class DataDeserialization implements IDataDeserialization
{
    protected static final Unsafe unsafe = Classes.getUnsafe();
    protected static final long byteArrayOffset = unsafe.arrayBaseOffset(byte[].class);
    private final ByteInputStream stream;
    protected Map<UUID, Object> extensions = new HashMap<UUID, Object>();

    /**
     * Creates a new object.
     *
     * @param inputStream data input stream
     */
    public DataDeserialization(ByteInputStream inputStream)
    {
        Assert.notNull(inputStream);
        
        this.stream = inputStream;
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
    public final ByteArray read(int length)
    {
        if (stream.pos + length > stream.length)
            throw new EndOfStreamException();
        
        ByteArray res = new ByteArray(stream.buffer, stream.pos, length);
        stream.pos += length;
        return res;
    }
    
    @Override
    public final byte readByte()
    {
        if (stream.pos + 1 > stream.length)
            throw new EndOfStreamException();
        
        byte res = unsafe.getByte(stream.buffer, byteArrayOffset + stream.pos);
        stream.pos++;
        return res;
    }
    
    @Override
    public final char readChar()
    {
        if (stream.pos + 2 > stream.length)
            throw new EndOfStreamException();
        
        char res = unsafe.getChar(stream.buffer, byteArrayOffset + stream.pos);
        stream.pos += 2;
        return res;
    }

    @Override
    public final short readShort()
    {
        if (stream.pos + 2 > stream.length)
            throw new EndOfStreamException();
        
        short res = unsafe.getShort(stream.buffer, byteArrayOffset + stream.pos);
        stream.pos += 2;
        return res;
    }
    
    @Override
    public final int readInt()
    {
        if (stream.pos + 4 > stream.length)
            throw new EndOfStreamException();
        
        int res = unsafe.getInt(stream.buffer, byteArrayOffset + stream.pos);
        stream.pos += 4;
        return res;
    }
    
    @Override
    public final long readLong()
    {
        if (stream.pos + 8 > stream.length)
            throw new EndOfStreamException();
        
        long res = unsafe.getLong(stream.buffer, byteArrayOffset + stream.pos);
        stream.pos += 8;
        return res;
    }

    @Override
    public final boolean readBoolean()
    {
        return readByte() != 0;
    }
    
    @Override
    public final float readFloat()
    {
        if (stream.pos + 4 > stream.length)
            throw new EndOfStreamException();
        
        float res = unsafe.getFloat(stream.buffer, byteArrayOffset + stream.pos);
        stream.pos += 4;
        return res;
    }
    
    @Override
    public final double readDouble()
    {
        if (stream.pos + 8 > stream.length)
            throw new EndOfStreamException();
        
        double res = unsafe.getDouble(stream.buffer, byteArrayOffset + stream.pos);
        stream.pos += 8;
        return res;
    }
    
    @Override
    public final ByteArray readByteArray()
    {
        int length = readInt();
        if (length == 0)
            return null;
     
        // To distinguish null byte array from empty byte array non null byte array length increased by one,
        // i.e. null byte array length - 0, empty byte array length - 1, etc.
        length--;
        
        return read(length);
    }
    
    @Override
    public final String readString()
    {
        ByteArray buf = readByteArray();
        if (buf == null)
            return null;
        
        try
        {
            return new String(buf.getBuffer(), buf.getOffset(), buf.getLength(), "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new SerializationException(e);
        }
    }
}
