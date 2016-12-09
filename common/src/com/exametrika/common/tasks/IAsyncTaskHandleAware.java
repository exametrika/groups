/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks;

/**
 * The {@link IAsyncTaskHandleAware} is used to set asynchronous task handle.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev_A
 */
public interface IAsyncTaskHandleAware
{
    /**
     * Sets async task handle.
     *
     * @param taskHandle task handle
     */
    void setAsyncTaskHandle(Object taskHandle);
}
