/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment.impl;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link CompartmentTaskList} is a list of compartment tasks.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompartmentTaskList
{
    private final List<?> tasks;
    private final boolean runnable;
    
    public CompartmentTaskList(List<?> tasks, boolean runnable)
    {
        Assert.notNull(tasks);
        
        this.tasks = Immutables.wrap(tasks);
        this.runnable = runnable;
    }
    
    public List<?> getTasks()
    {
        return tasks;
    }
    
    public boolean isRunnable()
    {
        return runnable;
    }
}
