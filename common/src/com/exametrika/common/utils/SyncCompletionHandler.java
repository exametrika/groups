/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.concurrent.Callable;

import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.SimpleList.Element;


/**
 * The {@link SyncCompletionHandler} is synchronous completion handler.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SyncCompletionHandler implements ICompletionHandler, Runnable
{
    private final SimpleList<SyncCompletionHandler> handlers;
    private final Element<SyncCompletionHandler> element;
    private final Callable callable;
    private boolean completed;
    private Object result;
    private Throwable error;
    
    public SyncCompletionHandler()
    {
        handlers = null;
        element = null;
        callable = null;
    }
    
    public SyncCompletionHandler(Callable callable)
    {
        Assert.notNull(callable);
        
        handlers = null;
        element = null;
        this.callable = callable;
    }
    
    public SyncCompletionHandler(SimpleList<SyncCompletionHandler> handlers)
    {
        Assert.notNull(handlers);
        
        this.handlers = handlers;
        element = new Element<SyncCompletionHandler>(this);
        this.callable = null;
        
        synchronized (handlers)
        {
            handlers.addLast(element);
        }
    }
    
    public <T> T await()
    {
        return await(0);
    }
    
    public synchronized <T> T await(long timeout)
    {
        long startTime = Times.getCurrentTime();
        while (!completed && Times.getCurrentTime() < startTime + timeout)
        {
            try
            {
                wait(timeout);
            }
            catch (InterruptedException e)
            {
                throw new ThreadInterruptedException(e);
            }
        }
        
        if (!completed)
            throw new TimeoutException();
        
        if (error != null)
        {
            Exceptions.wrapAndThrow(error);
            return null;
        }
        else
            return (T)result;
    }
    
    public void cancel()
    {
        onFailed(new OperationCanceledException());
    }
    
    @Override
    public void onSucceeded(Object value)
    {
        if (handlers != null)
        {
            synchronized (handlers)
            {
                element.remove();
            }
        }
        
        synchronized (this)
        {
            completed = true;
            result = value;
            notify();
        }
    }

    @Override
    public void onFailed(Throwable error)
    {
        if (handlers != null)
        {
            synchronized (handlers)
            {
                element.remove();
            }
        }
        
        synchronized (this)
        {
            completed = true;
            this.error = error;
            notify();
        }
    }

    @Override
    public void run()
    {
        try
        {
            Object result = callable.call();
            onSucceeded(result);
        }
        catch (Throwable e)
        {
            onFailed(e);
        }
    }
}
