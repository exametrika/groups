/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.compartment.impl;

import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.utils.Assert;



/**
 * The {@link CompartmentResult} is a compartment result implementation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompartmentResult implements Runnable
{
    private final ICompartmentTask task;
    private Object result;
    private Throwable error;
    
    public CompartmentResult(ICompartmentTask task, Object result, Throwable error)
    {
        Assert.notNull(task);
        
        this.task = task;
        this.result = result;
        this.error = error;
    }
    
    public boolean hasError()
    {
        return error != null;
    }
    
    public Object getResult()
    {
        return result;
    }
    
    public Throwable getError()
    {
        return error;
    }
    
    @Override
    public void run()
    {
        if (error == null)
            task.onSucceeded(result);
        else
            task.onFailed(error);
    }
}
