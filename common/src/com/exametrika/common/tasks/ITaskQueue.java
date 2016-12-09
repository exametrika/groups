/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;



/**
 * The {@link ITaskQueue} is a task queue used to store incoming task for further handling by {@link ITaskExecutor}.
 * 
 * @param <T> task type
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev_A
 */
public interface ITaskQueue<T>
{
    /**
     * Offers incoming task to the queue. This method must never block calling thread.
     *
     * @param task incoming task to offer
     * @return true is task is accepted, false if task is rejected
     */
    boolean offer(T task);
    
    /**
     * Puts incoming task to the queue. This method may block if queue capacity is exceeded.
     *
     * @param task incoming task to put
     */
    void put(T task);
}
