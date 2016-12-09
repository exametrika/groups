/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;



/**
 * The {@link IFlowController} represents a controller of tasks flow.
 * 
 * @param <T> task flow type
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IFlowController<T>
{
    /**
     * Locks flow of tasks from specified task flow. Flow locking is reentrant, that is this method can be called
     * several times and each call of {@link #lockFlow} must have corresponding call of {@link #unlockFlow}.
     * First call of {@link #lockFlow} actually locks flow of tasks and last call of {@link #unlockFlow}
     * actually unlocks flow of tasks.
     *
     * @param flow flow of tasks to lock. Can be null if any flow must be locked
     */
    void lockFlow(T flow);
    
    /**
     * Unlocks flow of tasks from specified task flow. Flow locking is reentrant, that is this method can be called
     * several times and each call of {@link #lockFlow} must have corresponding call of {@link #unlockFlow}.
     * First call of {@link #lockFlow} actually locks flow of tasks and last call of {@link #unlockFlow}
     * actually unlocks flow of tasks.
     *
     * @param flow flow of tasks to unlock. Can be null if any flow must be unlocked
     */
    void unlockFlow(T flow);
}
