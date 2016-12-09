/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.ICompletionHandler;


/**
 * The {@link KeyCompartmentTaskExecutor} is an asynchronous executor of compartment tasks identifiable by keys.
 * 
 * @param <K> task key type
 * @param <T> task type
 * @param <R> task result type
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class KeyCompartmentTaskExecutor<K, T extends ICompartmentTask<R>, R>
{
    private final ICompartment compartment;
    private final Map<K, ExecuteTask> executingTasks = new HashMap<K, ExecuteTask>();

    public KeyCompartmentTaskExecutor(ICompartment compartment)
    {
        Assert.notNull(compartment);
        
        this.compartment = compartment;
    }
    
    public boolean execute(K key, T task, ICompletionHandler<R> completionHandler)
    {
        Assert.notNull(key);
        Assert.notNull(task);
        Assert.notNull(completionHandler);
        
        ExecuteTask executeTask = executingTasks.get(key);
        if (executeTask == null)
        {
            executeTask = new ExecuteTask(key, task, completionHandler);
            executingTasks.put(key, executeTask);
        }
        else
            executeTask.completionHandlers.add(completionHandler);
        
        return compartment.execute(task);
    }
    
    public R execute(K key, T task)
    {
        Assert.notNull(key);
        Assert.notNull(task);
        
        ExecuteTask executeTask = executingTasks.get(key);
        
        try
        {
            R result = task.execute();
            
            if (executeTask != null)
            {
                executeTask.result = result;
                executeTask.completed = true;
            }

            task.onSucceeded(result);
            
            return result;
        }
        catch (Exception e)
        {
            if (executeTask != null)
            {
                executeTask.error = e;
                executeTask.completed = true;
            }

            task.onFailed(e);
            
            return Exceptions.wrapAndThrow(e);
        }
    }
    
    private class ExecuteTask implements ICompartmentTask<R>
    {
        private final K key;
        private final T task;
        private final List<ICompletionHandler<R>> completionHandlers = new ArrayList<ICompletionHandler<R>>();
        private boolean completed;
        private R result;
        private Throwable error;

        public ExecuteTask(K key, T task, ICompletionHandler<R> completionHandler)
        {
            Assert.notNull(key);
            Assert.notNull(task);
            Assert.notNull(completionHandler);
            
            this.key = key;
            this.task = task;
            this.completionHandlers.add(completionHandler);
        }
        
        @Override
        public R execute()
        {
            return task.execute();
        }

        @Override
        public void onSucceeded(R result)
        {
            if (completed)
            {
                if (error != null)
                {
                    onFailed(error);
                    return;
                }
                else
                    result = this.result;
            }
            
            task.onSucceeded(result);
            executingTasks.remove(key);
            
            for (ICompletionHandler<R> completionHandler : completionHandlers)
                completionHandler.onSucceeded(result);
        }

        @Override
        public void onFailed(Throwable error)
        {
            if (completed)
            {
                if (this.error != null)
                    error = this.error;
                else 
                {
                    onSucceeded(result);
                    return;
                }
            }
            
            task.onFailed(error);
            executingTasks.remove(key);
            
            for (ICompletionHandler<R> completionHandler : completionHandlers)
                completionHandler.onFailed(error);
        }
    }
}
