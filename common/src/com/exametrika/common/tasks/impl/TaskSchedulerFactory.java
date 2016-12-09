/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import com.exametrika.common.component.container.FactoryNotFoundException;
import com.exametrika.common.component.factory.singleton.AbstractSingletonComponentFactory;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.tasks.ITaskHandler;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;

/**
 * The {@link TaskSchedulerFactory} is a factory for {@link TaskScheduler}.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TaskSchedulerFactory extends AbstractSingletonComponentFactory<TaskScheduler>
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int threadCount;
    private final String taskHandlerName;
    private final String timeServiceName;
    private final long schedulePeriod;
    private final String name;
    private ITaskHandler taskHandler;
    private ITimeService timeService;

    /**
     * Creates a new object.
     *
     * @param threadCount count of threads in a thread pool of scheduler
     * @param taskHandler task handler
     * @param timeService time service to get a time
     * @param schedulePeriod schedule period in milliseconds
     * @param name scheduler name. Can be null, if name is not set
     * @exception InvalidArgumentException if threadCount < 1
     */
    public TaskSchedulerFactory(int threadCount, ITaskHandler taskHandler, ITimeService timeService, long schedulePeriod,
        String name)
    {
        super(true);
        
        Assert.notNull(taskHandler);
        Assert.notNull(timeService);
        
        this.threadCount = threadCount;
        this.taskHandler = taskHandler;
        this.timeService = timeService;
        this.schedulePeriod = schedulePeriod;
        this.name = name;
        this.taskHandlerName = null;
        this.timeServiceName = null;
    }
    
    /**
     * Creates a new object. For use in container only.
     * 
     * @param threadCount count of threads in a thread pool of scheduler
     * @param taskHandlerName name of task handler component to handle activated tasks
     * @param timeServiceName name of time service component to get time
     * @param schedulePeriod schedule period in milliseconds
     * @param name scheduler name. Can be null, if name is not set
     */
    public TaskSchedulerFactory(int threadCount, String taskHandlerName, String timeServiceName, long schedulePeriod,
        String name)
    {
        super(true);
        
        Assert.notNull(taskHandlerName);
        Assert.notNull(timeServiceName);
        
        this.threadCount = threadCount;
        this.taskHandlerName = taskHandlerName;
        this.timeServiceName = timeServiceName;
        this.schedulePeriod = schedulePeriod;
        this.name = name;
    }
    
    @Override
    protected TaskScheduler createInstance()
    {
        return new TaskScheduler(threadCount, taskHandler, timeService, schedulePeriod, name, name, null);
    }
    
    @Override
    protected void setFactoryDependencies()
    {
        if (taskHandler == null)
        {
            if (getContainer() != null)
                taskHandler = getContainer().getComponent(taskHandlerName);
            else
                throw new FactoryNotFoundException(messages.taskHandlerNotFound(taskHandlerName));
        }
        
        if (timeService == null)
        {
            if (getContainer() != null)
                timeService = getContainer().getComponent(timeServiceName);
            else
                throw new FactoryNotFoundException(messages.timeServiceNotFound(timeServiceName));
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Task handler ''{0}'' is not found.")
        ILocalizedMessage taskHandlerNotFound(Object handlerName);
        @DefaultMessage("Time service ''{0}'' is not found.")
        ILocalizedMessage timeServiceNotFound(Object timeServiceName);
    }
}
