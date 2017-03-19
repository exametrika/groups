/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;

import java.util.Random;

import com.exametrika.common.tasks.IDelayedTaskQueue;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;


/**
 * The {@link RandomPeriodicFailureModel} models random periodic failures.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RandomPeriodicFailureModel implements ILifecycle
{
    private final IDelayedTaskQueue<Runnable> taskQueue;
    private final long minFailureTime;
    private final long maxFailureTime;
    private final long failurePeriod;
    private final long minRecoveryPeriod;
    private final long maxRecoveryPeriod;
    private final ILifecycle[] components;
    private final ITimeService timeService;
    private final Random random = new Random();
    private volatile boolean started;
    
    /**
     * Creates a new object.
     *
     * @param taskQueue task queue
     * @param minFailureTime minimal failure time in milliseconds
     * @param maxFailureTime maximal failure time in milliseconds
     * @param failurePeriod failure period in milliseconds
     * @param minRecoveryPeriod minimal period to recover after failure in milliseconds
     * @param maxRecoveryPeriod maximal period to recover after failure in milliseconds
     * @param components components to model failure on
     * @param timeService time service
     */
    public RandomPeriodicFailureModel(IDelayedTaskQueue<Runnable> taskQueue, long minFailureTime, long maxFailureTime, long failurePeriod,
        long minRecoveryPeriod, long maxRecoveryPeriod, ILifecycle[] components, ITimeService timeService)
    {
        Assert.notNull(taskQueue);
        Assert.notNull(components);

        this.taskQueue = taskQueue;
        this.minFailureTime = minFailureTime;
        this.maxFailureTime = maxFailureTime;
        this.failurePeriod = failurePeriod;
        this.minRecoveryPeriod = minRecoveryPeriod;
        this.maxRecoveryPeriod = maxRecoveryPeriod;
        this.components = components;
        this.timeService = timeService;
    }

    @Override
    public void start()
    {
        long currentTime = timeService.getCurrentTime();
        
        started = true;
        long currentFailurePeriod = (long)(failurePeriod * random.nextDouble());
        if (currentTime + currentFailurePeriod < maxFailureTime)
        {
            if (currentTime + currentFailurePeriod < minFailureTime)
                currentFailurePeriod = minFailureTime - currentTime;
            
            taskQueue.offer(new Failure(), currentFailurePeriod);
        }
    }
    
    @Override
    public void stop()
    {
        started = false;
    }
    
    private class Failure implements Runnable
    {
        @Override
        public void run()
        {
            if (!started)
                return;
            
            for (ILifecycle component : components)
                component.stop();
            
            long recoveryPeriod = minRecoveryPeriod + (long)((maxRecoveryPeriod - minRecoveryPeriod) * random.nextDouble());
            taskQueue.offer(new Recovery(), recoveryPeriod);
        }
    }
    
    private class Recovery implements Runnable
    {
        @Override
        public void run()
        {
            if (!started)
                return;
            
            for (ILifecycle component : components)
                component.start();
            
            long currentTime = timeService.getCurrentTime();
            
            long currentFailurePeriod = (long)(failurePeriod * random.nextDouble());
            if (currentTime + currentFailurePeriod < maxFailureTime)
                taskQueue.offer(new Failure(), currentFailurePeriod);
        }
    }
}
