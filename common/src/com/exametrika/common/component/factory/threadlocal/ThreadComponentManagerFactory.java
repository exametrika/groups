/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.factory.threadlocal;

import com.exametrika.common.component.factory.singleton.AbstractSingletonComponentFactory;

/**
 * The {@link ThreadComponentManagerFactory} is a factory for {@link ThreadComponentManager}.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ThreadComponentManagerFactory extends AbstractSingletonComponentFactory<ThreadComponentManager>
{
    public ThreadComponentManagerFactory()
    {
        super(true);
    }
    
    @Override
    protected ThreadComponentManager createInstance()
    {
        return new ThreadComponentManager();
    }

}
