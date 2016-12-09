/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;



/**
 * The {@link IDelayedTaskQueue} is a task queue that delays execution of task on specified time period.
 * 
 * @param <T> task type
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev_A
 */
public interface IDelayedTaskQueue<T>
{
    /**
     * Offers incoming task to the queue. This method must never block calling thread.
     *
     * @param task incoming task to offer
     * @param period delay period in milliseconds
     * @return true is task is accepted, false if task is rejected
     */
    boolean offer(T task, long period);
}
