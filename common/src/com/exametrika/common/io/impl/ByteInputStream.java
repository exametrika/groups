/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io.impl;

import java.io.InputStream;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;


/**
 * The {@link ByteInputStream} is byte input stream.
 * 
 * @author Medvedev-A
 */
public final class ByteInputStream extends InputStream
{
    byte buffer[];
    int pos;
    int mark = 0;
    int length;

    /**
     * Creates stream from specified buffer.
     *
     * @param buffer input byte buffer
     */
    public ByteInputStream(byte buffer[])
    {
        Assert.notNull(buffer);
        
        this.buffer = buffer;
        this.pos = 0;
        this.length = buffer.length;
    }

    /**
     * Creates stream from specified buffer.
     *
     * @param buffer input byte buffer
     * @param offset buffer offset
     * @param length buffer length
     * @exception InvalidArgumentException if offset + length is greater than buffer length
     */
    public ByteInputStream(byte buffer[], int offset, int length)
    {
        Assert.notNull(buffer);
        if (offset < 0 || length < 0 || offset + length > buffer.length)
            throw new InvalidArgumentException();
        
        this.buffer = buffer;
        this.pos = offset;
        this.length = offset + length;
        this.mark = offset;
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
     * Returns current position in byte buffer
     *
     * @return current position to read from
     */
    public int getPosition()
    {
        return pos;
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
    
    @Override
    public int read()
    {
        return (pos < length) ? (buffer[pos++] & 0xff) : -1;
    }

    @Override
    public int read(byte[] value)
    {
        return read(value, 0, value.length);
    }
    
    @Override
    public int read(byte value[], int offset, int length)
    {
        Assert.notNull(value);
        if ((offset < 0) || (offset > value.length) || (length < 0) || ((offset + length) > value.length) || ((offset + length) < 0))
            throw new InvalidArgumentException();

        if (pos >= this.length)
        {
            return -1;
        }
        if (pos + length > this.length)
        {
            length = this.length - pos;
        }
        if (length <= 0)
        {
            return 0;
        }
        System.arraycopy(buffer, pos, value, offset, length);
        pos += length;
        return length;
    }

    @Override
    public long skip(long count)
    {
        if (pos + count > length)
        {
            count = length - pos;
        }
        if (count < 0)
        {
            return 0;
        }
        pos += count;
        return count;
    }

    @Override
    public int available()
    {
        return length - pos;
    }

    @Override
    public boolean markSupported()
    {
        return true;
    }

    @Override
    public void mark(int readAheadLimit)
    {
        mark = pos;
    }

    @Override
    public void reset()
    {
        pos = mark;
    }
}
