/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.modelling;


import com.exametrika.common.tasks.IDelayedTaskQueue;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;


/**
 * The {@link OneTimeFailureModel} models one-time failure at specified time.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class OneTimeFailureModel implements ILifecycle
{
    private final IDelayedTaskQueue<Runnable> taskQueue;
    private final long failurePeriod;
    private final ILifecycle[] components;

    /**
     * Creates a new object.
     *
     * @param taskQueue task queue
     * @param failurePeriod period in milliseconds after which failure is occured
     * @param components components to model failure on
     */
    public OneTimeFailureModel(IDelayedTaskQueue<Runnable> taskQueue, long failurePeriod, ILifecycle[] components)
    {
        Assert.notNull(taskQueue);
        Assert.notNull(components);
        
        this.taskQueue = taskQueue;
        this.failurePeriod = failurePeriod;
        this.components = components;
    }

    @Override
    public void start()
    {
        taskQueue.offer(new Runnable()
        {
            @Override
            public void run()
            {
                for (ILifecycle component : components)
                    component.stop();
                
            }
        }, failurePeriod);
    }
 
    @Override
    public void stop()
    {
    }
}
