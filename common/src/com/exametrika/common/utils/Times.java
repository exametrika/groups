/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.lang.management.ManagementFactory;

import com.exametrika.common.tasks.ThreadInterruptedException;








/**
 * The {@link Times} contains different utility methods for work with time.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Times
{
    private static boolean nativeAvailable;
    private static boolean tickCountAvailable;
    private static double frequency;
    private static boolean test;
    private static long testValue;
    private static long delta;
    
    static
    {
        try
        {
            System.loadLibrary("exaj");
        }
        catch (UnsatisfiedLinkError e)
        {
        }

        boolean available = false;
        try
        {
            nativeGetTickCount();
            available = true;
        }
        catch (UnsatisfiedLinkError e)
        {
        }
        
        if (available && checkOverhead())
            frequency = computeFrequency();
        else
        {
            available = false;
            frequency = 0;
        }
        
        nativeAvailable = available;
        tickCountAvailable = available;
    }
    
    public static boolean isTickCountAvaliable()
    {
        return tickCountAvailable;
    }
    
    public static double getTickFrequency()
    {
        return frequency;
    }
    
    public static long getTickCount()
    {
        if (nativeAvailable)
            return nativeGetTickCount();
        else if (test)
            return (long)(testValue * frequency);
        else
            return 0;
    }

    public static long getWallTime()
    {
        if (nativeAvailable)
            return (long)(nativeGetTickCount() / frequency);
        else if (test)
            return testValue;
        else
            return 0;
    }
    
    public static long getThreadCpuTime()
    {
        if (nativeAvailable)
            return nativeGetThreadCpuTime();
        else if (test)
            return testValue;
        else
            return ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
    }

    public static long getCurrentTime()
    {
        return getSystemCurrentTime() + delta;
    }

    public static long getSystemCurrentTime()
    {
        if (test)
            return testValue;
        else
            return System.currentTimeMillis();
    }
    
    public static void setDelta(long value)
    {
        delta = value;
    }
    
    public static void setTest(long value)
    {
        test = true;
        testValue = value;
        nativeAvailable = false;
    }
    
    public static void clearTest()
    {
        test = false;
        testValue = 0;
        nativeAvailable = tickCountAvailable;
    }

    private static double computeFrequency()
    {
        long start = System.nanoTime();
        long s = nativeGetTickCount();
        
        try
        {
            Thread.sleep(200);
        }
        catch (InterruptedException e)
        {
            throw new ThreadInterruptedException(e);
        }
        
        long e = nativeGetTickCount();
        long end = System.nanoTime();

        return (double)(e - s) / (end - start);
    }
    
    private static boolean checkOverhead()
    {
        long count = 100000;
        long t = System.nanoTime();
        for (int i = 0; i < count; i++)
            nativeGetTickCount();

        long overhead = (System.nanoTime() - t) / count;
        return overhead < 200;
    }
    
    private static native long nativeGetThreadCpuTime();
    private static native long nativeGetTickCount();

    private Times()
    {
    }
}
