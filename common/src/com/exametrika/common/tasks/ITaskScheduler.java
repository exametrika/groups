/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;

import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.InvalidArgumentException;

/**
 * The {@link ITaskScheduler} is used to schedule tasks accordingly to their activation conditions. Only one task of 
 * particular task instance, activation condition and recurrent status can be active (i.e. executing) at time. 
 * Tasks with different combination of task instance, activation condition and recurrent status can be activated concurrently.
 * 
 * @param <T> task type
 * @see ITaskExecutor
 * @see ICondition
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ITaskScheduler<T> extends ITaskQueue<T>
{
    /**
     * Returns count of threads in a thread pool of executor.
     *
     * @return thread count
     */
    int getThreadCount();
    
    /**
     * Sets count of threads in a thread pool of executor.
     *
     * @param threadCount thread count
     * @exception InvalidArgumentException if threadCount is less than 1
     */
    void setThreadCount(int threadCount);
    
    /**
     * Returns schedule period.
     *
     * @return schedule period in milliseconds
     */
    long getSchedulePeriod();
    
    /**
     * Sets schedule period.
     *
     * @param schedulePeriod schedule period in milliseconds
     */
    void setSchedulePeriod(long schedulePeriod);

    /**
     * Returns task by name.
     *
     * @param name name of task
     * @return task or null if task is not found
     */
    T findTask(String name);
    
    /**
     * Is task active?
     *
     * @param name name of task
     * @return true if task is active (i.e. executing now)
     */
    boolean isTaskActive(String name);
    
    /**
     * Adds a new task to scheduler.
     *
     * @param name task name
     * @param task task to be scheduled
     * @param activationCondition task's activation condition, based on current time.
     * @param recurrent is a task recurrent (i.e. repeatable several times). If task is not recurrent it is automatically removed
     *        from scheduler after completion.
     * @param async if true task is asynchronous
     * @param holder async task handle holder if task is asynchronous or null if task is synchronous       
     */
    void addTask(String name, T task, ICondition<Long> activationCondition, boolean recurrent, boolean async, 
        IAsyncTaskHandleAware holder);
    
    /**
     * Removes the specified task from scheduler.
     *
     * @param name name of task to remove
     * @return removed task
     */
    T removeTask(String name);
    
    /**
     * Removes all tasks from scheduler.
     */
    void removeAllTasks();
    
    /**
     * Notifies task scheduler that asynchronous task has been successfully completed.
     *
     * @param taskHandle task handle
     */
    void onAsyncTaskSucceeded(Object taskHandle);
    
    /**
     * Notifies task scheduler that asynchronous task has been failed.
     *
     * @param taskHandle task handle
     * @param error task error
     */
    void onAsyncTaskFailed(Object taskHandle, Throwable error);
}
