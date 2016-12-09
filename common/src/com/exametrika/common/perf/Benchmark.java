/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.perf;

import java.lang.management.ManagementFactory;
import java.text.MessageFormat;

import com.exametrika.common.utils.Assert;


/**
 * The {@link Benchmark} represents benchmark measuring execution time of specified probe. 
 *
 * @param <T> probe type
 * @see Runnable
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev_A
 */
public final class Benchmark<T extends IProbe>
{
    private final T probe;
    private final int count;
    private final long warmUpTime;
    private long time;
    private long totalTime;
    private long callCount;

    /**
     * Creates a new object.
     *
     * @param probe probe to run
     * @exception NullPointerException if probe is null
     */
    public Benchmark(T probe)
    {
        this(probe, 1, 1000000000L);
    }
    
    /**
     * Creates a new object.
     *
     * @param probe probe to run
     * @param count number of times to run specified probe
     * @exception NullPointerException if probe is null
     */
    public Benchmark(T probe, int count)
    {
        this(probe, count, 1000000000L);
    }

    /**
     * Creates a new object.
     *
     * @param probe probe to run
     * @param count number of times to run specified probe
     * @param warmUpTime time in nanoseconds to warm up JVM before measurement takes place
     */
    public Benchmark(T probe, int count, long warmUpTime)
    {
        Assert.notNull(probe);
        
        this.probe = probe;
        this.count = count;
        this.warmUpTime = warmUpTime;
        
        probe.beforeMeasure();
        
        try
        {
            warmUp();
            
            probe.afterWarmUp();
            
            measure();
        }
        finally
        {
            probe.afterMeasure(callCount, time, totalTime);
        }
    }
    
    /**
     * Returns total number of times calls in probe are executed.
     *
     * @return total number of times calls in probe are executed
     */
    public long getCount()
    {
        return callCount;
    }
    
    /**
     * Returns probe to run.
     *
     * @return probe to run
     */
    public T getProbe()
    {
        return probe;
    }
    
    /**
     * Returns the time of single run in nanoseconds.
     *
     * @return time of single run in nanoseconds
     */
    public long getTime()
    {
        return time;
    }
    
    /**
     * Returns total time of all runs in nanoseconds. 
     *
     * @return total time of all runs in nanoseconds
     */
    public long getTotalTime()
    {
        return totalTime;
    }
    
    public void print()
    {
        print("");
    }
    
    public void print(String message)
    {
        System.out.println(message + toString());
    }
    
    @Override
    public String toString()
    {
        return MessageFormat.format("count: {0,number}; total time: {1,number} ms; time: {2,number} ns", callCount, totalTime / 1000000, time);
    }
    
    private void warmUp()
    {
        cleanUp();
        
        int n = 1;
        for (long start = System.nanoTime(); System.nanoTime() - start < warmUpTime; n *= 2)
        {
            for (int i = 0; i < n; i++)
                run();
        }
    }
    
    private void measure()
    {
        cleanUp();
        
        time = 0;
        totalTime = 0;
        callCount = 0;
        
        long t = System.nanoTime();
        
        for (int i = 0; i < count; i++)
            run();
        
        totalTime = System.nanoTime() - t;
        time = totalTime / callCount;
    }
    
    private void run()
    {
        callCount += probe.run();
    }
    
    private void cleanUp()
    {
        long usedMemoryBefore = getUsedMemory();
        for (int i = 0; i < 100; i++)
        {
            System.runFinalization();
            System.gc();
            
            long usedMemoryAfter = getUsedMemory();
            
            if (ManagementFactory.getMemoryMXBean().getObjectPendingFinalizationCount() == 0 &&
                usedMemoryAfter >= usedMemoryBefore)
                break;
            
            usedMemoryBefore = usedMemoryAfter;
        }
    }
    
    private long getUsedMemory()
    {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
