/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import com.exametrika.common.tasks.ITaskHandler;
import com.exametrika.common.tasks.ITaskQueue;
import com.exametrika.common.utils.Assert;

/**
 * The {@link RedirectTaskHandler} is an implementation of {@link ITaskHandler} interface that redirects task to
 * task queue.
 *
 * @param <T> task type
 * @see ITaskHandler
 * @threadsafety This class and its methods are thread safe.
 * @author AndreyM
 */
public final class RedirectTaskHandler<T> implements ITaskHandler<T>
{
    private final ITaskQueue<T> taskQueue;

    /**
     * Creates a new object.
     *
     * @param taskQueue task queue to redirect to
     */
    public RedirectTaskHandler(ITaskQueue<T> taskQueue)
    {
        Assert.notNull(taskQueue);
        
        this.taskQueue = taskQueue;
    }
    
    @Override
    public void handle(T task)
    {
        taskQueue.put(task);
    }
}
