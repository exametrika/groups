/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.exametrika.common.tasks.ITaskFilter;
import com.exametrika.common.utils.Assert;


/**
 * The {@link CompositeTaskFilter} is a {@link ITaskFilter} implementation that
 * uses specified set of filters as a single composite task filter.
 *
 * @param <T> task type
 * @see ITaskFilter
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class CompositeTaskFilter<T> implements ITaskFilter<T>
{
    private volatile ArrayList<? extends ITaskFilter<T>> filters;

    /**
     * Creates a new object.
     */
    public CompositeTaskFilter()
    {
        this((ITaskFilter)null);
    }
    
    /**
     * Creates a new object.
     *
     * @param filter initial task filter. Can be null
     */
    public CompositeTaskFilter(ITaskFilter<T> filter)
    {
        if (filter != null)
            this.filters = new ArrayList(Arrays.asList(filter));
        else
            this.filters = new ArrayList();
    }
    
    /**
     * Creates a new object.
     *
     * @param filters initial list of task filters. Can be null
     */
    public CompositeTaskFilter(List<? extends ITaskFilter<T>> filters)
    {
        if (filters != null)
            this.filters = new ArrayList(filters);
        else
            this.filters = new ArrayList();
    }
    
    public void add(ITaskFilter<T> filter)
    {
        Assert.notNull(filter);
        
        synchronized (this)
        {
            ArrayList filters = (ArrayList)this.filters.clone();
            filters.add(filter);
    
            this.filters = filters;
        }
    }
    
    public void remove(ITaskFilter<T> filter)
    {
        Assert.notNull(filter);
        
        synchronized (this)
        {
            if (!this.filters.contains(filter))
                return;
            
            ArrayList filters = (ArrayList)this.filters.clone();
            filters.remove(filter);
    
            this.filters = filters;
        }
    }

    public void clear()
    {
        this.filters = new ArrayList();
    }
    
    @Override
    public boolean accept(T task)
    {
        ArrayList<? extends ITaskFilter<T>> filters = this.filters;
        for (ITaskFilter<T> filter : filters)
        {
            if (!filter.accept(task))
                return false;
        }
        
        return true;
    }
}
