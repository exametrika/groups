/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;


/**
 * The {@link ConstantLatencyModel} is a latency model that provides constant latencies.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ConstantLatencyModel implements ILatencyModel
{
    private final long latency;

    /**
     * Creates a new object.
     *
     * @param latency latency in milliseconds
     */
    public ConstantLatencyModel(long latency)
    {
        this.latency = latency;
    }
    
    @Override
    public long getLatency()
    {
        return latency;
    }
}
