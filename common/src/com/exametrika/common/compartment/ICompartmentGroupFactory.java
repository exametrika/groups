/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.time.ITimeService;
import com.exametrika.common.time.impl.SystemTimeService;





/**
 * The {@link ICompartmentGroupFactory} is a factory of {@link ICompartmentGroup}.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ICompartmentGroupFactory
{
    /**
     * Compartment configuration parameters.
     */
    public static class Parameters
    {
        /** Compartment name. */
        public String name = "compartment group";
        
        /** Compartment group timer period. */
        public long timerPeriod = 10;
        
        /** List of compartment group processors. */
        public List<ICompartmentGroupProcessor> processors = new ArrayList<ICompartmentGroupProcessor>();
        
        /** Number of threads in compartment group thread pool. */
        public int threadCount = Runtime.getRuntime().availableProcessors();
        
        /** Long running task queue capacity. */
        public int taskQueueCapacity = Integer.MAX_VALUE;
        
        /** Time service. */
        public ITimeService timeService = new SystemTimeService();
    }
    
    /**
     * Creates compartment group.
     *
     * @param parameters parameters
     * @return compartment group
     */
    ICompartmentGroup createCompartmentGroup(Parameters parameters);
}
