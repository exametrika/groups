/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment.impl;

import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;


/**
 * The {@link CompletionCompartmentTask} is a completion task.
 * 
 * @param <T> task result type
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class CompletionCompartmentTask<T> implements ICompartmentTask<T>
{
    private final ICompletionHandler completionHandler;

    public CompletionCompartmentTask(ICompletionHandler completionHandler)
    {
        Assert.notNull(completionHandler);
        
        this.completionHandler = completionHandler;
    }
    
    @Override
    public void onSucceeded(T result)
    {
        onCompleted(result);
        completionHandler.onSucceeded(result);
    }

    @Override
    public void onFailed(Throwable error)
    {
        onCompleted(error);
        completionHandler.onFailed(error);
    }
    
    protected void onCompleted(Object result)
    {
    }
}
