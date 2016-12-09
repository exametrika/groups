/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;

/**
 * The {@link ITaskListener} is a listener of task events.
 *
 * @param <T> task type
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ITaskListener<T>
{
    /**
     * Called when task is about to be started.
     * 
     * @param task starting task
     */
    void onTaskStarted(T task);

    /**
     * Called when task is successfully completed.
     * 
     * @param task completed task
     */
    void onTaskCompleted(T task);

    /**
     * Called when task is failed.
     * 
     * @param task failed task
     * @param error uncaught exception caused the task to fail
     */
    void onTaskFailed(T task, Throwable error);
}
