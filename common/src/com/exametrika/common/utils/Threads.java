/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import com.exametrika.common.tasks.ThreadInterruptedException;

/**
 * The {@link Threads} contains different utility methods for threads.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Threads
{
    /**
     * Sleeps thread for specified period.
     *
     * @param period period in milliseconds
     */
    public static void sleep(long period)
    {
        try
        {
            Thread.sleep(period);
        }
        catch (InterruptedException e)
        {
            throw new ThreadInterruptedException(e);
        }
    }
    
    @SuppressWarnings("deprecation")
    public static void suspend(Thread thread)
    {
        Assert.notNull(thread);
        
        if (Thread.currentThread() == thread)
            return;
        if (Debug.isDebug())
            return;
        
        thread.suspend();
    }
    
    @SuppressWarnings("deprecation")
    public static void resume(Thread thread)
    {
        Assert.notNull(thread);
        
        if (Thread.currentThread() == thread)
            return;
        if (Debug.isDebug())
            return;
        
        thread.resume();
    }
    
    private Threads()
    {
    }
}
