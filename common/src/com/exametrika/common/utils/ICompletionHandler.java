/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;






/**
 * The {@link ICompletionHandler} represents a asynchronous operation completion handler.
 * 
 * @param <T> result type
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ICompletionHandler<T>
{
    /**
     * Marks operation execution as completed successfully and sets result of operation execution.
     *
     * @param result result of operation execution
     */
    void onSucceeded(T result);
    
    /**
     * Marks operation execution as failed and sets error of operation execution.
     *
     * @param error error of operation execution
     */
    void onFailed(Throwable error);
}
