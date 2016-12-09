/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;

import com.exametrika.common.tasks.impl.IThreadDataProvider;

/**
 * The {@link Tasks} contains different utility methods for task manipulation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Tasks
{
    /**
     * Returns user data, attached to current thread.
     *
     * @return user data, attached to current thread or null if there is no any user data attached to the current thread
     */
    public static <T> T getCurrentThreadData()
    {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof IThreadDataProvider)
            return (T)((IThreadDataProvider)currentThread).getData();
        else
            return null;
    }
    
    private Tasks()
    {
    }
}
