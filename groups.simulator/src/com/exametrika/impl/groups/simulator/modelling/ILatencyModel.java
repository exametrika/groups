/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;


/**
 * The {@link ILatencyModel} represents a latency model.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ILatencyModel
{
    /**
     * Returns current latency.
     * 
     * @return current latency in milliseconds. 0 means "no delay"
     */
    long getLatency();
}
