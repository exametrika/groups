/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;




/**
 * The {@link ICompartmentSizeEstimator} is an estimator of sizes occuped by compartment tasks and their results in
 * compartment queue.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ICompartmentSizeEstimator
{
    /**
     * Estimates size of specified value. Can be called on compartment tasks, runnables, compartment results and exceptions
     * occured during execution of compartment tasks. Estimator is used in estimating compartment queue capacity.
     *
     * @param value value. Can be null
     * @return value size
     */
    int estimateSize(Object value);
}
