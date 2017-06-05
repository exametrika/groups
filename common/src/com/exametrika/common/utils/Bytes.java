/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.security.MessageDigest;
import java.util.UUID;



/**
 * The {@link Bytes} contains different utility methods for work with {@link ByteArray}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Bytes
{
    public static ByteArray combine(ByteArray first, ByteArray second)
    {
        return combine(first, second, false);
    }
    
    public static ByteArray combine(ByteArray first, ByteArray second, boolean copy)
    {
        if (first.isEmpty())
            return copy ? second.clone() : second;
        if (second.isEmpty())
            return copy ? first.clone() : first;
        
        byte[] buffer = new byte[first.getLength() + second.getLength()];
        System.arraycopy(first.getBuffer(), first.getOffset(), buffer, 0, first.getLength());
        System.arraycopy(second.getBuffer(), second.getOffset(), buffer, first.getLength(), second.getLength());
        
        return new ByteArray(buffer);
    }
    
    public static byte readByte(byte[] data, int offset)
    {
        return data[offset];
    }
    
    public static char readChar(byte[] data, int offset)
    {
        return (char)((data[offset] & 0xFF) + ((data[offset + 1] &  0xFF)<< 8));
    }
    
    public static short readShort(byte[] data, int offset)
    {
        return (short)((data[offset] & 0xFF) + ((data[offset + 1] & 0xFF) << 8));
    }
    
    public static int readInt(byte[] data, int offset)
    {
        return (data[offset] & 0xFF) + ((data[offset + 1] & 0xFF) << 8) + ((data[offset + 2] & 0xFF) << 16) + 
            ((data[offset + 3] & 0xFF) << 24);
    }
    
    public static long readLong(byte[] data, int offset)
    {
        return ((long)(data[offset] & 0xFF) << 0) + ((long)(data[offset + 1] & 0xFF) << 8) + ((long)(data[offset + 2] & 0xFF) << 16) + 
            ((long)(data[offset + 3] & 0xFF) << 24) + ((long)(data[offset + 4] & 0xFF) << 32) + ((long)(data[offset + 5] & 0xFF) << 40) +
            ((long)(data[offset + 6] & 0xFF) << 48) + ((long)(data[offset + 7] & 0xFF) << 56);
    }
    
    public static float readFloat(byte[] data, int offset)
    {
        return Float.intBitsToFloat(readInt(data, offset));
    }
    
    public static double readDouble(byte[] data, int offset)
    {
        return Double.longBitsToDouble(readLong(data, offset));
    }
    
    public static UUID readUUID(byte[] data, int offset)
    {
        return new UUID(readLong(data, offset + 8), readLong(data, offset));
    }
    
    public static String readString(byte[] data, int offset)
    {
        int length = readInt(data, offset);
        offset += 4;
        char[] buffer = new char[length];
        for (int i = 0; i < length; i++)
        {
            buffer[i] = readChar(data, offset);
            offset += 2;
        }
        
        return new String(buffer);
    }
    
    public static void writeByte(byte[] data, int offset, byte value)
    {
        data[offset + 0] = value;
    }

    public static void writeChar(byte[] data, int offset, char value)
    {
        data[offset + 0] = (byte)(value >>> 0);
        data[offset + 1] = (byte)(value >>> 8);
    }

    public static void writeShort(byte[] data, int offset, short value)
    {
        data[offset + 0] = (byte)(value >>> 0);
        data[offset + 1] = (byte)(value >>> 8);
    }
    
    public static void writeInt(byte[] data, int offset, int value)
    {
        data[offset + 0] = (byte)(value >>>  0);
        data[offset + 1] = (byte)(value >>>  8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
    
    public static void writeLong(byte[] data, int offset, long value)
    {
        data[offset + 0] = (byte)(value >>>  0);
        data[offset + 1] = (byte)(value >>>  8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
        data[offset + 4] = (byte)(value >>> 32);
        data[offset + 5] = (byte)(value >>> 40);
        data[offset + 6] = (byte)(value >>> 48);
        data[offset + 7] = (byte)(value >>> 56); 
    }
    
    public static void writeFloat(byte[] data, int offset, float value)
    {
        writeInt(data, offset, Float.floatToIntBits(value));
    }
    
    public static void writeDouble(byte[] data, int offset, double value)
    {
        writeLong(data, offset, Double.doubleToLongBits(value));
    }
    
    public static void writeUUID(byte[] data, int offset, UUID value)
    {
        writeLong(data, offset, value.getLeastSignificantBits());
        writeLong(data, offset + 8, value.getMostSignificantBits());
    }
    
    public static void writeString(byte[] data, int offset, String value)
    {
        writeInt(data, offset, value.length());
        offset += 4;
        for (int i = 0; i < value.length(); i++)
        {
            writeChar(data, offset, value.charAt(i));
            offset += 2;
        }
    }
    
    public static String md5Hash(ByteArray value)
    {
        Assert.notNull(value);
        
        try 
        {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(value.getBuffer(), value.getOffset(), value.getLength());
            
            return Strings.digestToString(digest.digest());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private Bytes()
    {
    }
}
