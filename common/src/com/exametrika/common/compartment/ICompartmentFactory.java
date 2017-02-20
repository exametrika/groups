/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.compartment.impl.SimpleCompartmentDispatcher;
import com.exametrika.common.compartment.impl.SimpleCompartmentQueue;
import com.exametrika.common.compartment.impl.SimpleCompartmentSizeEstimator;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tasks.impl.NoFlowController;





/**
 * The {@link ICompartmentFactory} is a factory of {@link ICompartment}.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ICompartmentFactory
{
    /**
     * Compartment configuration parameters.
     */
    class Parameters
    {
        /** Compartment name. */
        public String name = "compartment";
        
        /** Compartment dispatch period - maximum period in milliseconds on which main compartment thread is blocked. */
        public long dispatchPeriod = 100;
        
        /** Compartment dispatcher. */
        public ICompartmentDispatcher dispatcher = new SimpleCompartmentDispatcher();
        
        /** List of compartment timer processors. */
        public List<ICompartmentTimerProcessor> timerProcessors = new ArrayList<ICompartmentTimerProcessor>();
        
        /** List of compartment processors. */
        public List<ICompartmentProcessor> processors = new ArrayList<ICompartmentProcessor>();
        
        /** Compartment group. Can be null if default compartment group is used. */
        public ICompartmentGroup group;
        
        /** Does compartment own a group or not? */
        public boolean groupOwner = false;
        
        /** Flow controller of compartment. */
        public IFlowController flowController = new NoFlowController();
        
        /** Minimum capacity of task queue which locks flow controller. */
        public int minLockQueueCapacity = Integer.MAX_VALUE;
        
        /** Maximum capacity of task queue which unlocks flow controller. */
        public int maxUnlockQueueCapacity = Integer.MAX_VALUE;
        
        /** Number of tasks processed in single run. */
        public int taskBatchSize = 10;
        
        /** Size estimator used in estimating task queue capacity. */
        public ICompartmentSizeEstimator sizeEstimator = new SimpleCompartmentSizeEstimator();
        
        /** Task queue. */
        public ICompartmentQueue queue = new SimpleCompartmentQueue();
    }
    
    /**
     * Creates compartment.
     *
     * @param parameters parameters
     * @return compartment
     */
    ICompartment createCompartment(Parameters parameters);
}
