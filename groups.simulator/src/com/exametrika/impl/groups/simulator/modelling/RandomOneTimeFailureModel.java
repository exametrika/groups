/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;

import java.util.Random;

import com.exametrika.common.tasks.IDelayedTaskQueue;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;


/**
 * The {@link RandomOneTimeFailureModel} models one-time failure at random time in specified period.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RandomOneTimeFailureModel implements ILifecycle
{
    private final IDelayedTaskQueue<Runnable> taskQueue;
    private final long minFailurePeriod;
    private final long maxFailurePeriod;
    private final ILifecycle[] components;

    /**
     * Creates a new object.
     *
     * @param taskQueue task queue
     * @param minFailurePeriod minimal period in milliseconds after which failure is occured
     * @param maxFailurePeriod maximal period in milliseconds after which failure is occured
     * @param components components to model failure on
     */
    public RandomOneTimeFailureModel(IDelayedTaskQueue<Runnable> taskQueue, long minFailurePeriod, 
        long maxFailurePeriod, ILifecycle[] components)
    {
        Assert.notNull(taskQueue);
        Assert.notNull(components);
        
        this.taskQueue = taskQueue;
        this.minFailurePeriod = minFailurePeriod;
        this.maxFailurePeriod = maxFailurePeriod;
        this.components = components;
    }

    @Override
    public void start()
    {
        Random random = new Random();
        long failurePeriod = minFailurePeriod + (long)((maxFailurePeriod - minFailurePeriod) * random.nextDouble());
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
