/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import com.exametrika.common.tasks.IFilteredTaskQueue;
import com.exametrika.common.tasks.ITaskFilter;
import com.exametrika.common.tasks.ITaskQueue;
import com.exametrika.common.utils.Assert;

/**
 * The {@link FilteredTaskQueue} is an implementation of {@link IFilteredTaskQueue} interface.
 * 
 * @param <T> task type
 * @see IFilteredTaskQueue
 * @threadsafety This class and its methods are thread safe.
 * @author AndreyM
 */
public final class FilteredTaskQueue<T> implements IFilteredTaskQueue<T>
{
    private final CompositeTaskFilter<T> filters;
    private final ITaskQueue<T> taskQueue;
    
    /**
     * Creates a new object.
     * 
     * @param taskQueue task queue
     */
    public FilteredTaskQueue(ITaskQueue<T> taskQueue)
    {
        Assert.notNull(taskQueue);
        
        this.taskQueue = taskQueue;
        this.filters = new CompositeTaskFilter<T>();
    }
    
    @Override
    public boolean offer(T task)
    {
        Assert.notNull(task);
        
        if (!filters.accept(task))
            return false;
        
        return taskQueue.offer(task);
    }
    
    @Override
    public void put(T task)
    {
        Assert.notNull(task);
        
        if (!filters.accept(task))
            return;
        
        taskQueue.put(task);
    }

    @Override
    public void addFilter(ITaskFilter<T> filter)
    {
        filters.add(filter);
    }

    @Override
    public void removeFilter(ITaskFilter<T> filter)
    {
        filters.remove(filter);
    }

    @Override
    public void removeAllFilters()
    {
        filters.clear();
    }
}
