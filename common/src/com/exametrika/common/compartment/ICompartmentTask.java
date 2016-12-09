/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.compartment;

import com.exametrika.common.utils.ICompletionHandler;





/**
 * The {@link ICompartmentTask} is a compartment task, representing some functionality executed asynchronously
 * in one of threads of calling compartment thread pool or in main thread of external compartment. 
 * Results or error of task execution are returned back to the main thread of calling compartment.
 * 
 * @param <T> task return type
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ICompartmentTask<T> extends ICompletionHandler<T>
{
    /**
     * Executes task. Called in one of threads of calling compartment thread pool or in main thread of external compartment. 
     * When called in thread of pool, can contain long running functionality.
     *
     * @return result of task execution or null if task returns nothing
     */
    T execute();

    /**
     * Called in main thread of calling compartment when task is successfully completed.
     * 
     * @param result result of task execution or null if task returns nothing
     */
    @Override
    void onSucceeded(T result);

    /**
     * Called in main thread of calling compartment when task is failed.
     * 
     * @param error uncaught exception caused the task to fail
     */
    @Override
    void onFailed(Throwable error);
}
