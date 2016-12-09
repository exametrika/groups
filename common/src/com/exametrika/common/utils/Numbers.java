/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.UUID;



/**
 * The {@link Numbers} is utility class for numbers.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Numbers
{
    private static long sequence = 1;
    private static boolean test;
    
    /**
     * Compute percents of two values.
     *
     * @param value1 value1
     * @param value2 value2
     * @return percents of value1 in value2 or 0 if value2 is 0
     */
    public static double percents(double value1, double value2)
    {
        if (value2 != 0)
            return value1 / value2 * 100;
        else
            return 0;
    }
    
    /**
     * Is specified value a power of two.
     *
     * @param value value must be positive
     * @return true if value is a power of two
     */
    public static boolean isPowerOfTwo(int value)
    {
        return (value & (value - 1)) == 0;
    }
    
    public static int roundToPowerOfTwo(int value)
    {
        if (isPowerOfTwo(value))
            return value;
        else
            return 1 << (log2(value) + 1);
    }
    
    /**
     * Calculates log2 logarithm.
     *
     * @param value
     * @return result
     */
    public static int log2(int value)
    {
        Assert.isTrue(value > 0);
        
        return 31 - Integer.numberOfLeadingZeros(value);
    }
    
    /**
     * Calculates log2 logarithm.
     *
     * @param value
     * @return result
     */
    public static int log2(long value)
    {
        Assert.isTrue(value > 0);
        
        return 63 - Long.numberOfLeadingZeros(value);
    }
    
    public static double log(double value, double base)
    {
        return Math.log(value) / Math.log(base);
    }
    
    public static int pad(int value, int boundary)
    {
        if (value % boundary != 0)
            value = (value / boundary + 1) * boundary;
        
        return value;
    }
    
    public static int compare(int x, int y) 
    {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
    
    public static int compare(long x, long y) 
    {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
    
    public static UUID randomUUID()
    {
        if (!test)
            return UUID.randomUUID();
        
        return new UUID(sequence++, sequence++);
    }
    
    public static void setTest()
    {
        test = true;
        sequence = 1;
    }
    
    public static void clearTest()
    {
        test = false;
    }
    
    private Numbers()
    {
    }
}
