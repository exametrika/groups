/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.Arrays;




/**
 * The {@link BitArray} represents a bit array of specified size.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class BitArray
{
    public static final int ADDRESS_BITS_PER_WORD = 6;
    public static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private static int byteBitCounts[];
    private final long[] buffer;
    private final int length;
    
    static
    {
        byteBitCounts = new int[256];
        for (int i = 0; i < byteBitCounts.length; i++)
            byteBitCounts[i] = computeBitCount(i);
    }
    
    /**
     * Creates a new object.
     *
     * @param length length of bit array in bits
     */
    public BitArray(int length)
    {
        this.buffer = new long[(length >>> ADDRESS_BITS_PER_WORD) + 1];
        this.length = length;
    }
    
    /**
     * Creates a new object.
     *
     * @param buffer bit array buffer, all buffer bits outside [0..length-1] must be set to <c>false<c>.
     * @param length length of bit array in bits
     * @exception InvalidArgumentException if length != (length >>> ADDRESS_BITS_PER_WORD) + 1
     */
    public BitArray(long[] buffer, int length)
    {
        Assert.notNull(buffer);
        if (length != ((length >>> ADDRESS_BITS_PER_WORD) + 1))
            throw new InvalidArgumentException();
        
        this.buffer = buffer;
        this.length = length;
    }
    
    /**
     * Returns length of bit array.
     * 
     * @return bit array length
     */
    public int getLength()
    {
        return length;
    }
    
    /**
     * Returns a bit value.
     *
     * @param index bit index
     * @return bit value
     * @exception IndexOutOfBoundsException if index is out of bounds
     */
    public boolean get(int index)
    {
        if (index < 0 || index > length)
            throw new IndexOutOfBoundsException();
        
        return (buffer[index >>> ADDRESS_BITS_PER_WORD] & (1L << index)) != 0;
    }
    
    /**
     * Sets a bit value.
     *
     * @param index bit index
     * @param value bit value to set
     * @exception IndexOutOfBoundsException if index is out of bounds
     */
    public void set(int index, boolean value)
    {
        if (value)
            set(index);
        else
            clear(index);
    }
    
    /**
     * Sets a bit value to <c>true<c>.
     *
     * @param index bit index
     * @exception IndexOutOfBoundsException if index is out of bounds
     */
    public void set(int index)
    {
        if (index < 0 || index > length)
            throw new IndexOutOfBoundsException();

        buffer[index >>> ADDRESS_BITS_PER_WORD] |= 1L << index;
    }
    
    /**
     * Sets a bit value to <c>false<c>.
     *
     * @param index bit index
     * @exception IndexOutOfBoundsException if index is out of bounds
     */
    public void clear(int index)
    {
        if (index < 0 || index > length)
            throw new IndexOutOfBoundsException();
     
        buffer[index >>> ADDRESS_BITS_PER_WORD] &= ~(1L << index);
    }
    
    /**
     * Performs logical <c>and<c> with specified bit array.
     *
     * @param array array to combine
     * @exception InvalidArgumentException if array length is not equal to length of this array
     */
    public void and(BitArray array)
    {
        Assert.notNull(array);
        if (array.length != length)
            throw new InvalidArgumentException();
        
        for (int i = 0; i < buffer.length; i++)
            buffer[i] &= array.buffer[i];
    }
    
    /**
     * Performs logical <c>or<c> with specified bit array.
     *
     * @param array array to combine
     * @exception InvalidArgumentException if array length is not equal to length of this array
     */
    public void or(BitArray array)
    {
        Assert.notNull(array);
        if (array.length != length)
            throw new InvalidArgumentException();
        
        for (int i = 0; i < buffer.length; i++)
            buffer[i] |= array.buffer[i];
    }
    
    /**
     * Performs logical <c>xor<c> with specified bit array.
     *
     * @param array array to combine
     * @exception InvalidArgumentException if array length is not equal to length of this array
     */
    public void xor(BitArray array)
    {
        Assert.notNull(array);
        if (array.length != length)
            throw new InvalidArgumentException();
        
        for (int i = 0; i < buffer.length; i++)
            buffer[i] ^= array.buffer[i];
    }
    
    /**
     * Returns number of bits whose value is set to <c>true<c>.
     *
     * @return number of bits whose value is set to <c>true<c>
     */
    public int getBitCount()
    {
        int count = 0;
        for (int i = 0; i < buffer.length; i++)
        {
            long value = buffer[i];
            if (value != 0)
                count += getBitCount(value);
        }
        
        return count;
    }
    
    /**
     * Returns buffer.
     *
     * @return buffer
     */
    public long[] getBuffer()
    {
        return buffer;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
            return true;
        
        if (!(o instanceof BitArray))
            return false;
        
        BitArray vector = (BitArray)o;
        return Arrays.equals(buffer, vector.buffer);
    }
    
    @Override
    public int hashCode()
    {
        return Arrays.hashCode(buffer);
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
            buf.append(i);
            buf.append('(');
            buf.append(get(i) ? "true" : "false");
            buf.append(')');
        }
        
        buf.append("]");
        return buf.toString();
    }

    private static int getBitCount(long value)
    {
        return byteBitCounts[(int)(value & 0xff)] + byteBitCounts[(int)((value >>> 8) & 0xff)] + 
            byteBitCounts[(int)((value >>> 16) & 0xff)] + byteBitCounts[(int)((value >>> 24) & 0xff)] + 
            byteBitCounts[(int)((value >>> 32) & 0xff)] + byteBitCounts[(int)((value >>> 40) & 0xff)] + 
            byteBitCounts[(int)((value >>> 48) & 0xff)] + byteBitCounts[(int)((value >>> 56) & 0xff)];
    }
    
    private static int computeBitCount(int value) 
    {
        int count = 0;
        while (value != 0)
        {
            count += value & 0x1;
            value >>>= 1;
        }
        return count;
    }
}
