/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;





/**
 * The {@link ICompartmentMXBean} is a management interface of compartment.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ICompartmentMXBean
{
    /** Name of MXBean of compartment.*/
    final String MBEAN_NAME = "com.exametrika.common:type=Compartment";
    
    /**
     * Returns compartment name.
     *
     * @return compartment name
     */
    String getName();
    
    /**
     * Returns compartment group.
     *
     * @return compartment group
     */
    ICompartmentGroupMXBean getGroup();
    
    /**
     * Returns dispatch period in milliseconds.
     *
     * @return dispatch period in milliseconds
     */
    long getDispatchPeriod();
    
    /**
     * Sets dispatch period in milliseconds.
     *
     * @param period dispatch period in milliseconds
     */
    void setDispatchPeriod(long period);
    
    /** 
     * Returns minimum capacity of task queue which locks flow controller.
     * 
     * @return minimum capacity of task queue which locks flow controller
     */
    int getMinLockQueueCapacity();
    
    /** 
     * Sets minimum capacity of task queue which locks flow controller.
     * 
     * @param value minimum capacity of task queue which locks flow controller
     */
    void setMinLockQueueCapacity(int value);
    
    /** 
     * Returns maximum capacity of task queue which unlocks flow controller.
     * 
     * @return maximum capacity of task queue which unlocks flow controller
     */
    int getMaxUnlockQueueCapacity();
    
    /** 
     * Sets maximum capacity of task queue which unlocks flow controller.
     * 
     * @param value maximum capacity of task queue which unlocks flow controller
     */
    void setMaxUnlockQueueCapacity(int value);
    
    /**
     * Returns number of tasks processed in single run.
     *
     * @return number of tasks processed in single run
     */
    int getTaskBatchSize();
    
    /**
     * Sets number of tasks processed in single run.
     *
     * @param value number of tasks processed in single run
     */
    void setTaskBatchSize(int value);
}
