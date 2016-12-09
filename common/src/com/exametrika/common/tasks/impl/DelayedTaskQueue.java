/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.exametrika.common.tasks.IDelayedTaskQueue;
import com.exametrika.common.tasks.ITaskQueue;
import com.exametrika.common.tasks.ITaskSource;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;


/**
 * The {@link DelayedTaskQueue} is an implementation of task queue.
 * 
 * @param <T> task type
 * @see ITaskQueue
 * @see ITaskSource
 * @threadsafety This class and its methods are thread safe.
 * @author AndreyM
 */
public final class DelayedTaskQueue<T> implements IDelayedTaskQueue<T>, ITaskSource<T>
{
    private final DelayQueue<DelayedTask> queue;
    private final ITimeService timeService;
    
    /**
     * Creates a new object.
     * 
     * @param timeService time service
     */
    public DelayedTaskQueue(ITimeService timeService)
    {
        Assert.notNull(timeService);
        
        queue = new DelayQueue<DelayedTask>();
        this.timeService = timeService;
    }
    
    @Override
    public boolean offer(T task, long period)
    {
        Assert.notNull(task);
        
        queue.offer(new DelayedTask(task, timeService.getCurrentTime() + period));
        return true;
    }

    @Override
    public T take()
    {
        try
        {
            return queue.take().task;
        }
        catch (InterruptedException e)
        {
            throw new ThreadInterruptedException(e);
        }
    }
    
    private class DelayedTask implements Delayed
    {
        private final T task;
        private final long activationTime;

        public DelayedTask(T task, long activationTime)
        {
            this.task = task;
            this.activationTime = activationTime;
        }
        
        @Override
        public long getDelay(TimeUnit unit)
        {
            return unit.convert(activationTime - timeService.getCurrentTime(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o)
        {
            DelayedTask task = (DelayedTask)o;
            if (activationTime < task.activationTime)
                return -1;
            else if (activationTime > task.activationTime)
                return 1;
            else
                return 0;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (!(o instanceof DelayedTaskQueue.DelayedTask))
                return false;
            
            DelayedTask task = (DelayedTask)o;
            return activationTime == task.activationTime;
        }
        
        @Override
        public int hashCode()
        {
            return (int)(activationTime ^ (activationTime >>> 32));
        }
    }
}
