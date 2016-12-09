/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.exametrika.common.tasks.ITaskQueue;
import com.exametrika.common.tasks.ITaskSource;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;


/**
 * The {@link TaskQueue} is an implementation of task queue.
 * 
 * @param <T> task type
 * @see ITaskQueue
 * @see ITaskSource
 * @threadsafety This class and its methods are thread safe.
 * @author AndreyM
 */
public class TaskQueue<T> implements ITaskQueue<T>, ITaskSource<T>
{
    private final LinkedBlockingQueue<T> queue;
    private final long waitPeriod;
    
    /**
     * Creates a new object.
     */
    public TaskQueue()
    {
        this.waitPeriod = 0;
        queue = new LinkedBlockingQueue<T>();
    }

    /**
     * Creates a new object.
     * 
     * @param capacity queue capacity
     * @param waitPeriod task wait period in milliseconds, 0 means unlimited
     */
    public TaskQueue(int capacity, long waitPeriod)
    {
        this.waitPeriod = waitPeriod;
        queue = new LinkedBlockingQueue<T>(capacity);
    }

    public LinkedBlockingQueue<T> getQueue()
    {
        return queue;
    }
    
    @Override
    public boolean offer(T task)
    {
        Assert.notNull(task);
        
        return queue.offer(task);
    }

    @Override
    public void put(T task)
    {
        Assert.notNull(task);
        
        try
        {
            queue.put(task);
        }
        catch (InterruptedException e)
        {
            throw new ThreadInterruptedException(e);
        }
    }

    @Override
    public T take()
    {
        try
        {
            if (waitPeriod == 0)
                return queue.take();
            else
                return queue.poll(waitPeriod, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            throw new ThreadInterruptedException(e);
        }
    }
}
