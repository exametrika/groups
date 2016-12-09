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
import com.exametrika.common.tasks.ITaskSource;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;

/**
 * The {@link TaskExecutorFactory} is a factory for {@link TaskExecutor}.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TaskExecutorFactory extends AbstractSingletonComponentFactory<TaskExecutor>
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int threadCount;
    private final String name;
    private ITaskHandler taskHandler;
    private ITaskSource taskSource;
    private final String taskSourceName;
    private final String taskHandlerName;
    
    /**
     * Creates a new object.
     *
     * @param threadCount maximal count of threads in a thread pool of executor.@param threadCount count of threads in a thread pool of executor
     * @param taskSource task source
     * @param taskHandler task handler
     * @param name executor name. Can be null, if name is not set
     * @exception InvalidArgumentException if threadCount < 1
     */
    public TaskExecutorFactory(int threadCount, ITaskSource taskSource, ITaskHandler taskHandler, String name)
    {
        super(false);
        
        if (threadCount < 1)
            throw new InvalidArgumentException();
        Assert.notNull(taskSource);
        Assert.notNull(taskHandler);
        
        this.name = name;
        this.threadCount = threadCount;
        this.taskSource = taskSource;
        this.taskHandler = taskHandler;
        this.taskSourceName = null;
        this.taskHandlerName = null;
    }
    
    /**
     * Creates a new object. For use in container only.
     *
     * @param threadCount maximal count of threads in a thread pool of executor.@param threadCount count of threads in a thread pool of executor
     * @param taskSourceName task source name
     * @param taskHandlerName name of component for task handler
     * @param name executor name. Can be null, if name is not set
     * @exception InvalidArgumentException if threadCount < 1
     */
    public TaskExecutorFactory(int threadCount, String taskSourceName, 
        String taskHandlerName, String name)
    {
        super(true);
        
        if (threadCount < 1)
            throw new InvalidArgumentException();
        Assert.notNull(taskSourceName);
        Assert.notNull(taskHandlerName);
        
        this.name = name;
        this.threadCount = threadCount;
        this.taskSourceName = taskSourceName;
        this.taskHandlerName = taskHandlerName;
    }
    
    @Override
    protected TaskExecutor createInstance()
    {
        return new TaskExecutor(threadCount, taskSource, taskHandler, name, true, null);
    }
    
    @Override
    protected void setFactoryDependencies()
    {
        if (taskSource == null)
        {
            if (getContainer() != null)
                taskSource = getContainer().getComponent(taskSourceName);
            else
                throw new FactoryNotFoundException(messages.taskSourceNotFound(taskSourceName));
        }
        
        if (taskHandler == null)
        {
            if (getContainer() != null)
                taskHandler = getContainer().getComponent(taskHandlerName);
            else
                throw new FactoryNotFoundException(messages.taskHandlerNotFound(taskHandlerName));
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Task source ''{0}'' is not found.")
        ILocalizedMessage taskSourceNotFound(Object sourceName);
        @DefaultMessage("Task handler ''{0}'' is not found.")
        ILocalizedMessage taskHandlerNotFound(Object handlerName);
    }
}
