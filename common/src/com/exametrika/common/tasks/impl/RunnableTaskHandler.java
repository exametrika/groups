/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import com.exametrika.common.tasks.ITaskHandler;

/**
 * The {@link RunnableTaskHandler} is an implementation of {@link ITaskHandler} interface that use {@link Runnable} interface
 * to handle tasks.
 * 
 * @param <T> task type
 * @see ITaskHandler
 * @see Runnable
 * @threadsafety This class and its methods are thread safe.
 * @author AndreyM
 */
public final class RunnableTaskHandler<T extends Runnable> implements ITaskHandler<T>
{
    @Override
    public void handle(T task)
    {
        task.run();
    }
}
