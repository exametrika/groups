/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.impl;

import java.io.OutputStream;

import com.exametrika.common.utils.InvalidArgumentException;


/**
 * The {@link ByteOutputStream} is byte output stream.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class ByteOutputStream extends OutputStream
{
    protected byte buffer[];
    protected int length;

    /**
     * Creates stream with default buffer.
     */
    public ByteOutputStream()
    {
        this(32);
    }

    /**
     * Creates stream with buffer of specified length.
     * 
     * @param length buffer length
     * @exception InvalidArgumentException if length is negative
     */
    public ByteOutputStream(int length)
    {
        if (length < 0)
            throw new InvalidArgumentException();
        buffer = new byte[length];
    }

    /**
     * Returns byte buffer.
     *
     * @return byte buffer
     */
    public byte[] getBuffer()
    {
        return buffer;
    }

    /**
     * Returns length of byte buffer.
     *
     * @return length of byte buffer
     */
    public int getLength()
    {
        return length;
    }
    
    public Object getMark()
    {
        return this;
    }
    
    public byte[] getBuffer(Object mark)
    {
        return buffer;
    }
    
    public int getBufferLength()
    {
        return length;
    }
    
    @Override
    public void write(int value)
    {
        int newLength = length + 1;
        if (newLength > buffer.length)
            growBuffer(newLength);
        
        buffer[length] = (byte)value;
        length = newLength;
    }

    @Override
    public void write(byte value[]) 
    {
        write(value, 0, value.length);
    }
    
    @Override
    public void write(byte value[], int offset, int length)
    {
        if ((offset < 0) || (offset > value.length) || (length < 0) || ((offset + length) > value.length) || ((offset + length) < 0))
            throw new InvalidArgumentException();
        else if (length == 0)
            return;

        int newLength = this.length + length;
        if (newLength > buffer.length)
            growBuffer(newLength);
        System.arraycopy(value, offset, buffer, this.length, length);
        this.length = newLength;
    }

    /**
     * Grows buffer on specified length.
     *
     * @param length grow length
     * @exception InvalidArgumentException if length is negative
     */
    public void grow(int length)
    {
        if (length < 0)
            throw new InvalidArgumentException();
        else if (length == 0)
            return;

        int newLength = this.length + length;
        if (newLength > buffer.length)
            growBuffer(newLength);
        
        this.length = newLength;
    }

    /**
     * Returns a copy of byte array.
     *
     * @return a copy of byte array
     */
    public byte[] toByteArray()
    {
        byte[] out = new byte[buffer.length];
        System.arraycopy(buffer, 0, out, 0, buffer.length);
        
        return out;
    }
    
    protected void growBuffer(int newLength)
    {
        byte newBuffer[] = new byte[Math.max(buffer.length << 1, newLength)];
        System.arraycopy(buffer, 0, newBuffer, 0, length);
        buffer = newBuffer;
    }
}
