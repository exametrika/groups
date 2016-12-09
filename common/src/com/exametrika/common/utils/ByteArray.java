/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.Serializable;




/**
 * The {@link ByteArray} represents a byte array. Byte array is considered as immutable even if it offers access to underlying
 * mutable byte array (for efficiency reasons).
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ByteArray implements Comparable<ByteArray>, Cloneable, Serializable
{
    public static final ByteArray EMPTY = new ByteArray(new byte[0]);
    private final byte[] buffer;
    private final int offset;
    private final int length;
    
    /**
     * Creates byte array.
     *
     * @param length array length
     * @exception InvalidArgumentException if length is negative
     */
    public ByteArray(int length)
    {
        if (length < 0)
            throw new InvalidArgumentException();
        
        this.buffer = new byte[length];
        this.offset = 0;
        this.length = length; 
    }
    
    /**
     * Creates byte array.
     *
     * @param buffer byte buffer
     */
    public ByteArray(byte[] buffer)
    {
        Assert.notNull(buffer);    
        
        this.buffer = buffer;
        this.offset = 0;
        this.length = buffer.length; 
    }
    
    /**
     * Creates byte array.
     *
     * @param buffer byte buffer
     * @param offset buffer offset
     * @param length buffer length
     * @exception InvalidArgumentException if offset + length is greater than buffer length
     */
    public ByteArray(byte[] buffer, int offset, int length)
    {
        Assert.notNull(buffer);
        if (offset < 0 || length < 0 || offset + length > buffer.length)
            throw new InvalidArgumentException();
        
        this.buffer = buffer;
        this.offset = offset;
        this.length = length; 
    }
    
    public byte[] getBuffer()
    {
        return buffer;
    }
    
    public int getOffset()
    {
        return offset;
    }
    
    public int getLength()
    {
        return length;
    }
    
    public boolean isEmpty()
    {
        return length == 0;
    }
    
    public int get(int index)
    {
        return (buffer[offset + index] & 0xFF);
    }
    
    public ByteArray subArray(int beginIndex)
    {
        return subArray(beginIndex, length);
    }
    
    public ByteArray subArray(int beginIndex, int endIndex)
    {
        if (beginIndex == 0 && endIndex == length)
            return this;
        else
            return new ByteArray(buffer, offset + beginIndex, endIndex - beginIndex);
    }
    
    public boolean startsWith(ByteArray o)
    {
        if (length < o.length)
            return false;
        
        for (int i = 0; i < o.length; i++)
        {
            if (buffer[offset + i] != o.buffer[o.offset + i])
                return false;
        }
        
        return true;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        
        if (!(o instanceof ByteArray))
            return false;
        
        ByteArray buf = (ByteArray)o;
        if (buffer == buf.buffer && length == buf.length && offset == buf.offset)
            return true;
        
        if (length != buf.length)
            return false;
        
        for (int i = 0; i < length; i++)
        {
            if (buffer[offset + i] != buf.buffer[buf.offset + i])
                return false;
        }
        
        return true;
    }
    
    @Override
    public int hashCode()
    {
        int result = 1;
        for (int i = 0; i < length; i++)
            result = 31 * result + buffer[offset + i];
 
        return result;
    }

    @Override
    public int compareTo(ByteArray o)
    {
        int n = Math.min(length, o.length);
        for (int i = 0; i < n; i++) 
        {
            int cmp = (buffer[offset + i] & 0xFF) - (o.buffer[o.offset + i] & 0xFF);
            if (cmp != 0)
                return cmp;
        }
        return length - o.length;
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
 
        for (int i = 0; i < length; i++) 
        {
            if (i > 0)
                buf.append(", ");
            buf.append(buffer[offset + i] & 0xFF);
        }
 
        buf.append("]");
        return buf.toString();
    }
    
    @Override
    public ByteArray clone()
    {
        return new ByteArray(toByteArray());
    }
    
    public byte[] toByteArray()
    {
        byte[] buf = new byte[length];
        System.arraycopy(buffer, offset, buf, 0, length);
        return buf;
    }
}
