/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb.impl;

import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;



/**
 * The {@link RawAbstractBlockSerialization} is an abstract implementation of {@link IDataSerialization} based on database.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public abstract class RawAbstractBlockSerialization extends RawAbstractBlockDeserialization implements IDataSerialization
{
    private static final boolean DEBUG = RawDbDebug.DEBUG;
    private IRawWriteRegion region;
    
    public RawAbstractBlockSerialization(int blockSize, int blockOffset)
    {
        super(blockSize, blockOffset);
    }

    @Override
    public final void write(ByteArray value)
    {
        Assert.notNull(value);
        
        int length = value.getLength();
        byte[] buf = value.getBuffer();
        int offset = value.getOffset();
        
        if (blockOffset + length > blockSize)
        {
            while (length > 0)
            {
                if (blockOffset + length > blockSize)
                {
                    int l = blockSize - blockOffset;
                    
                    if (DEBUG)
                        debug(blockOffset, l);
                    
                    region.writeByteArray(blockOffset, buf, offset, l);

                    length -= l;
                    offset += l;
                    
                    nextWriteRegion();
                }
                else
                {
                    if (DEBUG)
                        debug(blockOffset, length);
                    
                    region.writeByteArray(blockOffset, buf, offset, length);
                    blockOffset += length;
                    length = 0;
                }
            }
        }
        else
        {
            int p = blockOffset;
            blockOffset += length;  
            
            if (DEBUG)
                debug(p, length);

            region.writeByteArray(p, buf, offset, length);
        }
    }

    @Override
    public final void writeByte(byte value)
    {
        int p = blockOffset;
        blockOffset++;
        
        if (blockOffset > blockSize)
        {
            nextWriteRegion();
            p = blockOffset;
            blockOffset += 1;
        }
        
        if (DEBUG)
            debug(p, 1);
        
        region.writeByte(p, value);
    }

    @Override
    public final void writeChar(char value)
    {
        int p = blockOffset;
        blockOffset += 2;       
        
        if (blockOffset > blockSize)
        {
            nextWriteRegion();
            p = blockOffset;
            blockOffset += 2;
        }
        
        if (DEBUG)
            debug(p, 2);
        
        region.writeChar(p, value);
    }

    @Override
    public final void writeShort(short value)
    {
        int p = blockOffset;
        blockOffset += 2;       
        
        if (blockOffset > blockSize)
        {
            nextWriteRegion();
            p = blockOffset;
            blockOffset += 2;
        }
        
        if (DEBUG)
            debug(p, 2);
        
        region.writeShort(p, value);
    }

    @Override
    public final void writeInt(int value)
    {
        int p = blockOffset;
        blockOffset += 4;       
        
        if (blockOffset > blockSize)
        {
            nextWriteRegion();
            p = blockOffset;
            blockOffset += 4;
        }
        
        if (DEBUG)
            debug(p, 4);
        
        region.writeInt(p, value);
    }

    @Override
    public final void writeLong(long value)
    {
        int p = blockOffset;
        blockOffset += 8;       
        
        if (blockOffset > blockSize)
        {
            nextWriteRegion();
            p = blockOffset;
            blockOffset += 8;
        }
        
        if (DEBUG)
            debug(p, 8);
        
        region.writeLong(p, value);
    }

    @Override
    public final void writeBoolean(boolean value)
    {
        writeByte(value ? (byte)1 : (byte)0);
    }

    @Override
    public final void writeFloat(float value)
    {
        int p = blockOffset;
        blockOffset += 4;       
        
        if (blockOffset > blockSize)
        {
            nextWriteRegion();
            p = blockOffset;
            blockOffset += 4;
        }
        
        if (DEBUG)
            debug(p, 4);
        
        region.writeFloat(p, value);
    }
    
    @Override
    public final void writeDouble(double value)
    {
        int p = blockOffset;
        blockOffset += 8;       
        
        if (blockOffset > blockSize)
        {
            nextWriteRegion();
            p = blockOffset;
            blockOffset += 8;
        }
        
        if (DEBUG)
            debug(p, 8);
        
        region.writeDouble(p, value);
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
            // To distinguish null string from empty non null string length increased by one,
            // i.e. null string length - 0, empty byte array length - 1, etc.
            writeInt(value.length() + 1);

            if ((blockOffset & 1) == 1)
                blockOffset++;
            
            int length = value.length();
            char[] buf = value.toCharArray();
            int offset = 0;
            
            if (blockOffset + (length << 1) > blockSize)
            {
                while (length > 0)
                {
                    if (blockOffset + (length << 1) > blockSize)
                    {
                        int l = (blockSize - blockOffset) >>> 1;
                    
                        if (DEBUG)
                            debug(blockOffset, l << 1);
                        
                        region.writeCharArray(blockOffset, buf, offset, l);

                        length -= l;
                        offset += l;
                        
                        nextWriteRegion();
                    }
                    else
                    {
                        if (DEBUG)
                            debug(blockOffset, length << 1);
                        
                        region.writeCharArray(blockOffset, buf, offset, length);
                        blockOffset += length << 1;
                        length = 0;
                    }
                }
            }
            else
            {
                int p = blockOffset;
                blockOffset += length << 1;  

                if (DEBUG)
                    debug(p, length << 1);

                region.writeCharArray(p, buf, 0, length);
            }
        }
        else
            writeInt(0);
    }

    @Override
    protected void setRegion(IRawReadRegion region)
    {
        this.region = (IRawWriteRegion)region;
        super.setRegion(region);
    }
    
    protected abstract void nextWriteRegion();
    
    protected void debug(int offset, int length)
    {
    }
}
