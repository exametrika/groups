/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.utils.ByteArray;



/**
 * The {@link RawAbstractBlockDeserialization} is an abstract implementation of {@link IDataDeserialization} based on database.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public abstract class RawAbstractBlockDeserialization implements IDataDeserialization, Cloneable
{
    protected int blockSize;
    protected int blockOffset;
    private IRawReadRegion region;
    private Map<UUID, Object> extensions;

    public RawAbstractBlockDeserialization(int blockSize, int blockOffset)
    {
        this.blockSize = blockSize;
        this.blockOffset = blockOffset;
    }

    @Override
    public final <T> T getExtension(UUID id)
    {
        if (extensions != null)
            return (T)extensions.get(id);
        else
            return null;
    }
    
    @Override
    public final <T> void setExtension(UUID id, T value)
    {
        if (extensions == null)
            extensions = new HashMap<UUID, Object>();
        
        extensions.put(id, value);
    }

    @Override
    public final ByteArray read(int length)
    {
        if (blockOffset + length > blockSize)
        {
            byte[] buf = new byte[length];
            int offset = 0;
            while (length > 0)
            {
                if (blockOffset + length > blockSize)
                {
                    int l = blockSize - blockOffset;
                    region.readByteArray(blockOffset, buf, offset, l);

                    length -= l;
                    offset += l;
                    
                    nextReadRegion();
                }
                else
                {
                    region.readByteArray(blockOffset, buf, offset, length);
                    blockOffset += length;
                    length = 0;
                }
            }
            
            return new ByteArray(buf);
        }
        else
        {
            int p = blockOffset;
            blockOffset += length;  
            
            return region.readByteArray(p, length);
        }
    }
    
    @Override
    public final byte readByte()
    {
        int p = blockOffset;
        blockOffset++;  
        
        if (blockOffset > blockSize)
        {
            nextReadRegion();
            p = blockOffset;
            blockOffset += 1;
        }
        
        return region.readByte(p);
    }

    @Override
    public final char readChar()
    {
        int p = blockOffset;
        blockOffset += 2;       
        
        if (blockOffset > blockSize)
        {
            nextReadRegion();
            p = blockOffset;
            blockOffset += 2;
        }
        
        return region.readChar(p);
    }

    @Override
    public final short readShort()
    {
        int p = blockOffset;
        blockOffset += 2;  
        
        if (blockOffset > blockSize)
        {
            nextReadRegion();
            p = blockOffset;
            blockOffset += 2;
        }
        
        return region.readShort(p);
    }

    @Override
    public final int readInt()
    {
        int p = blockOffset;
        blockOffset += 4;     
        
        if (blockOffset > blockSize)
        {
            nextReadRegion();
            p = blockOffset;
            blockOffset += 4;
        }
        
        return region.readInt(p);
    }

    @Override
    public final long readLong()
    {
        int p = blockOffset;
        blockOffset += 8;       
        
        if (blockOffset > blockSize)
        {
            nextReadRegion();
            p = blockOffset;
            blockOffset += 8;
        }
        
        return region.readLong(p);
    }

    @Override
    public final boolean readBoolean()
    {
        return readByte() != 0;
    }

    @Override
    public final float readFloat()
    {
        int p = blockOffset;
        blockOffset += 4; 

        if (blockOffset > blockSize)
        {
            nextReadRegion();
            p = blockOffset;
            blockOffset += 4;
        }
        
        return region.readFloat(p);
    }
    
    @Override
    public final double readDouble()
    {
        int p = blockOffset;
        blockOffset += 8; 

        if (blockOffset > blockSize)
        {
            nextReadRegion();
            p = blockOffset;
            blockOffset += 8;
        }
        
        return region.readDouble(p);
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
        int length = readInt();
        if (length == 0)
            return null;
     
        // To distinguish null string from empty non null string length increased by one,
        // i.e. null byte array length - 0, empty byte array length - 1, etc.
        length--;
        
        if ((blockOffset & 1) == 1)
            blockOffset++;
        
        if (blockOffset + (length << 1) > blockSize)
        {
            char[] buf = new char[length];
            int offset = 0;
            while (length > 0)
            {
                if (blockOffset + (length << 1) > blockSize)
                {
                    int l = (blockSize - blockOffset) >>> 1;
                    region.readCharArray(blockOffset, buf, offset, l);

                    length -= l;
                    offset += l;
                    
                    nextReadRegion();
                }
                else
                {
                    region.readCharArray(blockOffset, buf, offset, length);
                    blockOffset += length << 1;
                    length = 0;
                }
            }
            
            return new String(buf);
        }
        else
        {
            int p = blockOffset;
            blockOffset += length << 1;  
            
            char[] buf = new char[length];
            region.readCharArray(p, buf, 0, length);
            
            return new String(buf);
        }
    }

    protected IRawReadRegion getRegion()
    {
        return region;
    }
    
    protected void setRegion(IRawReadRegion region)
    {
        this.region = region;
    }
    
    protected abstract void nextReadRegion();
    
    @Override
    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new RawDatabaseException(e);
        }
    }
}
