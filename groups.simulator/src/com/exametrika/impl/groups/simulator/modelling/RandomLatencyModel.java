/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;

import java.util.Random;


/**
 * The {@link RandomLatencyModel} is a random latency model.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RandomLatencyModel implements ILatencyModel
{
    private final long minLatency;
    private final long maxLatency;
    private final Random random = new Random();

    /**
     * Creates a new object.
     *
     * @param minLatency minimal latency in milliseconds
     * @param maxLatency maximal latency in milliseconds
     */
    public RandomLatencyModel(long minLatency, long maxLatency)
    {
        this.minLatency = minLatency;
        this.maxLatency = maxLatency;
    }
    
    @Override
    public long getLatency()
    {
        return minLatency + (long)((maxLatency - minLatency) * random.nextDouble());
    }
}
