/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.io;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;


/**
 * The {@link MultiByteOutputStream} is multi-buffer byte output stream.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class MultiByteOutputStream extends ByteOutputStream
{
    private List<ByteArray> buffers = new ArrayList<ByteArray>();
    private final int threshold;
    private int buffersLength;
    private Mark mark;

    public MultiByteOutputStream()
    {
        this(100, 32);
    }
    
    public MultiByteOutputStream(int threshold)
    {
        this(threshold, 32);
    }

    public MultiByteOutputStream(int threshold, int length)
    {
        super(length);
        
        this.threshold = threshold;
        mark = new Mark();
        mark.buffer = buffer;
    }

    public List<ByteArray> getBuffers()
    {
        return buffers;
    }
    
    @Override
    public int getLength()
    {
        return buffersLength + length;
    }
    
    @Override
    public Object getMark()
    {
        return mark;
    }

    @Override
    public byte[] getBuffer(Object mark)
    {
        return ((Mark)mark).buffer;
    }

    @Override
    public void write(byte value[], int offset, int length)
    {
        if (length > threshold)
        {
            close();
            
            buffers.add(new ByteArray(value, offset, length));
            buffersLength += length;
        }
        else
            super.write(value, offset, length);
    }

    @Override
    public byte[] toByteArray()
    {
        Assert.supports(false);
        return null;
    }
    
    @Override
    public void close()
    {
        if (length > 0)
        {
            buffers.add(new ByteArray(buffer, 0, length));
            buffersLength += length;
            
            buffer = new byte[32];
            length = 0;
            mark = new Mark();
            mark.buffer = buffer;
        }  
    }
    
    @Override
    protected void growBuffer(int newLength)
    {
        super.growBuffer(newLength);
        
        mark.buffer = buffer;
    }
    
    private static class Mark
    {
        byte[] buffer;
    }
}
