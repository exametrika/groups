/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;


/**
 * The {@link ITaskFilter} is used to accept or reject incoming tasks in task queue.
 * 
 * @param <T> task type
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev_A
 */
public interface ITaskFilter<T>
{
    /**
     * Does incoming task have to be accepted or rejected in task queue.
     *
     * @param task incoming task
     * @return true if task is accepted, false if task is rejected
     */
    boolean accept(T task);
}
