/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.perf;



/**
 * The {@link IProbe} represents specific probe to run with {@link Benchmark}.
 * 
 * @see Benchmark
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IProbe
{
    /**
     * Called before measuring is started.
     */
    void beforeMeasure();
    
    /**
     * Called after warm up is completed. Called after {@link #beforeMeasure()}.
     */
    void afterWarmUp();
    
    /**
     * Performs probe run.
     *
     * @return number of calls performed in the probe run
     */
    long run();
    
    /**
     * Called after measuring is completed.
     *
     * @param callCount total number of times calls in probe are executed
     * @param time time of single run in nanoseconds
     * @param totalTime total time of all runs in nanoseconds
     */
    void afterMeasure(long callCount, long time, long totalTime);
}
