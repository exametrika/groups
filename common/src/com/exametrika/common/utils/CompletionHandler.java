/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;



/**
 * The {@link CompletionHandler} is completion handler.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class CompletionHandler implements ICompletionHandler
{
    @Override
    public void onSucceeded(Object result)
    {
        onCompleted(result);
    }

    @Override
    public void onFailed(Throwable error)
    {
        onCompleted(error);
    }
    
    protected void onCompleted(Object result)
    {
    }
}
