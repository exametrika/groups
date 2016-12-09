/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;

import com.exametrika.common.utils.InvalidArgumentException;

/**
 * The {@link IThreadPoolTaskExecutor} is a {@link ITaskExecutor} that uses pool of threads
 * to execute tasks.
 * 
 * @param <T> task type
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IThreadPoolTaskExecutor<T> extends ITaskExecutor<T>
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
}
