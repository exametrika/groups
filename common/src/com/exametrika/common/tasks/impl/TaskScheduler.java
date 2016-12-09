/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.IActivationCondition;
import com.exametrika.common.tasks.IAsyncTaskHandleAware;
import com.exametrika.common.tasks.ITaskContext;
import com.exametrika.common.tasks.ITaskHandler;
import com.exametrika.common.tasks.ITaskListener;
import com.exametrika.common.tasks.ITaskScheduler;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.InvalidStateException;
import com.exametrika.common.utils.Objects;


/**
 * The {@link TaskScheduler} is an implementation of {@link ITaskScheduler} interface.
 * 
 * @param <T> task type
 * @see ITaskScheduler
 * @threadsafety This class and its methods are thread safe.
 * @author AndreyM
 */
public final class TaskScheduler<T> implements ITaskScheduler<T>,  
    ILifecycle, ITaskListener<TaskScheduler.TaskInfo<T>>, ITimerListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TaskScheduler.class);
    private final String name;
    private final TaskExecutor<TaskInfo<T>> taskExecutor;
    private final TaskQueue<TaskInfo<T>> taskQueue;
    private final ITimeService timeService;
    private final Timer timer;
    private final TaskContext taskContext = new TaskContext();
    private final Map<String, TaskInfo> tasks = new LinkedHashMap<String, TaskInfo>();
    private final Map<String, TaskInfo> scheduledTasks = new LinkedHashMap<String, TaskInfo>();
    private final Map<String, TaskInfo> activeTasks = new LinkedHashMap<String, TaskInfo>();
    private final List<Object> modifiedTasks = new ArrayList<Object>();
    private final List<TaskInfo> completedTasks = new ArrayList<TaskInfo>();
    private boolean started;
    private boolean stopped;
    
    /**
     * Creates a new scheduler.
     *
     * @param threadCount count of threads in a thread pool of scheduler
     * @param taskHandler task handler
     * @param timeService time service to get a time
     * @param schedulePeriod schedule period in milliseconds
     * @param schedulerName scheduler name. Can be null, if name is not set
     * @param executorName executor name. Can be null, if name is not set
     * @param data user data attached to thread pool
     * @exception InvalidArgumentException if threadCount < 1
     */
    public TaskScheduler(int threadCount, ITaskHandler<T> taskHandler, ITimeService timeService, long schedulePeriod,
        String schedulerName, String executorName, Object data)
    {
        Assert.notNull(taskHandler);
        Assert.notNull(timeService);
        
        if (schedulerName == null)
            this.name = "Scheduler";
        else
            this.name = schedulerName;
        this.taskQueue = new TaskQueue<TaskInfo<T>>();
        this.taskExecutor = new TaskExecutor<TaskInfo<T>>(threadCount, taskQueue, new TaskHandler<T>(taskHandler), executorName);
        this.taskExecutor.addTaskListener(this);
        this.timeService = timeService;
        this.timer = new Timer(schedulePeriod, this, false, this.name, data);
    }

    @Override
    public int getThreadCount()
    {
        return taskExecutor.getThreadCount();
    }

    @Override
    public void setThreadCount(int threadCount)
    {
        taskExecutor.setThreadCount(threadCount);
    }

    @Override
    public long getSchedulePeriod()
    {
        return timer.getPeriod();
    }

    @Override
    public void setSchedulePeriod(long schedulePeriod)
    {
        timer.setPeriod(schedulePeriod);
    }

    @Override
    public boolean offer(T task)
    {
        return taskQueue.offer(new TaskInfo<T>(task));
    }

    @Override
    public void put(T task)
    {
        taskQueue.put(new TaskInfo<T>(task));
    }

    @Override
    public synchronized T findTask(String name)
    {
        Assert.notNull(name);
        
        TaskInfo taskInfo = tasks.get(name);
        if (taskInfo == null)
            return null;
        else
            return (T)taskInfo.task;
    }

    @Override
    public synchronized boolean isTaskActive(String name)
    {
        Assert.notNull(name);
        
        TaskInfo taskInfo = tasks.get(name);
        if (taskInfo == null)
            return false;
        else
            return taskInfo.active;
    }

    @Override
    public synchronized void addTask(String name, T task, ICondition<Long> activationCondition, boolean recurrent, boolean async,
        IAsyncTaskHandleAware holder)
    {
        Assert.notNull(name);
        Assert.checkState(!stopped);
        
        TaskInfo taskInfo = new TaskInfo(name, task, activationCondition, recurrent, async);
        
        if (async)
            holder.setAsyncTaskHandle(taskInfo);
        
        if (started)
            modifiedTasks.add(taskInfo);

        tasks.put(name, taskInfo);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.taskAdded(this.name, name));
    }

    @Override
    public synchronized T removeTask(String name)
    {
        Assert.notNull(name);
        Assert.checkState(!stopped);
        
        if (started)
            modifiedTasks.add(name);
        
        TaskInfo taskInfo = tasks.remove(name);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.taskRemoved(this.name, name));
        
        if (taskInfo != null)
            return (T)taskInfo.task;
        else
            return null;
    }
    
    @Override
    public synchronized void removeAllTasks()
    {
        Assert.notNull(name);
        Assert.checkState(!stopped);
        
        if (started)
            modifiedTasks.add(null);

        tasks.clear();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.allTasksRemoved(name));
    }

    @Override
    public synchronized void start()
    {
        if (started)
            throw new InvalidStateException(messages.alreadyStarted(name));
        
        activeTasks.clear();
        completedTasks.clear();
        modifiedTasks.clear();
        scheduledTasks.clear();
        scheduledTasks.putAll(tasks);
        taskContext.parameters.clear();
        
        taskExecutor.start();
        timer.start();
        
        started = true;
    }

    @Override
    public void stop()
    {
        synchronized (this)
        {
            if (stopped)
                return;
            
            stopped = true;
        }
        
        timer.stop();
        taskExecutor.stop();
        
        syncTasks();
        
        activeTasks.clear();
        taskContext.parameters.clear();
        completedTasks.clear();
        scheduledTasks.clear();
        
        Assert.checkState(modifiedTasks.isEmpty());
    }

    @Override
    public void onTaskStarted(TaskInfo<T> task)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.taskStarting(name, task.name));
    }
    
    @Override
    public synchronized void onTaskCompleted(TaskInfo<T> task)
    {
        if (task.async)
            return;
        
        onAsyncTaskSucceeded(task);
    }
    
    @Override
    public synchronized void onAsyncTaskSucceeded(Object t)
    {
        TaskInfo<T> task = (TaskInfo<T>)t;
        if (started && task.activationCondition != null)
            completedTasks.add(task);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.taskCompleted(name, task.name));
    }

    @Override
    public synchronized void onTaskFailed(TaskInfo<T> task, Throwable error)
    {
        if (task.async)
            return;
        
        onAsyncTaskFailed(task, error);
    }
    
    @Override
    public synchronized void onAsyncTaskFailed(Object t, Throwable error)
    {
        TaskInfo<T> task = (TaskInfo<T>)t;
        if (started && task.activationCondition != null)
            completedTasks.add(task);
        
        if (logger.isLogEnabled(LogLevel.ERROR))
            logger.log(LogLevel.ERROR, messages.taskFailed(name, task.name, error));
    }

    @Override
    public void onTimer()
    {
        schedule();
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
    private synchronized void syncTasks()
    {
        for (Object task : modifiedTasks)
        {
            if (task == null)
            {
                scheduledTasks.clear();
                activeTasks.clear();
            }
            else if (task instanceof TaskInfo)
            {
                TaskInfo taskInfo = (TaskInfo)task;
                scheduledTasks.put(taskInfo.name, taskInfo);
                
                activeTasks.remove(taskInfo.name);
            }
            else
            {
                Assert.isInstanceOf(String.class, task);
                scheduledTasks.remove(task);
                
                activeTasks.remove(task);
            }
        }
        
        modifiedTasks.clear();
        
        for (TaskInfo task : completedTasks)
        {
            task.setCompleted();
            if (task.activationCondition instanceof IActivationCondition)
                ((IActivationCondition)task.activationCondition).onCompleted(taskContext);
            if (activeTasks.get(task.name) == task)
            {
                activeTasks.remove(task.name);
                scheduledTasks.put(task.name, task);
            }
        }
        
        completedTasks.clear();
    }
    
    private void schedule()
    {
        syncTasks();
        
        long currentTime = timeService.getCurrentTime();
        
        for (Iterator<TaskInfo> it = scheduledTasks.values().iterator(); it.hasNext(); )
        {
            TaskInfo taskInfo = it.next();
            
            if (!taskInfo.isRecurrent() && taskInfo.isCompleted())
            {
                // Task is active or one-time task has been completed. Skip it
                it.remove();
                continue;
            }
            
            if (taskInfo.getActivationCondition() instanceof IActivationCondition)
            {
                IActivationCondition activationCondition = (IActivationCondition)taskInfo.getActivationCondition();
                if (activationCondition.canActivate(currentTime, taskContext))
                {
                    it.remove();
                    activate(taskInfo);
                }
            }
            else if (taskInfo.getActivationCondition().evaluate(currentTime))
            {
                it.remove();
                activate(taskInfo);
            }
        }
        
        for (TaskInfo taskInfo : activeTasks.values())
        {
            if (taskInfo.activationCondition instanceof IActivationCondition)
                ((IActivationCondition)taskInfo.activationCondition).tryInterrupt(currentTime);
        }
    }
    
    private void activate(TaskInfo taskInfo)
    {
        taskInfo.active = true;
        activeTasks.put(taskInfo.name, taskInfo);
        
        taskQueue.put(taskInfo);
    }
    
    static final class TaskInfo<T>
    {
        private final String name;
        private final T task;
        private final ICondition<Long> activationCondition;
        private final boolean recurrent;
        private final boolean async;
        private boolean completed = false;
        private volatile boolean active;

        public TaskInfo(T task)
        {
            Assert.notNull(task);
            
            this.name = null;
            this.task = task;
            this.activationCondition = null;
            this.recurrent = false;
            this.async = false;
        }
        
        public TaskInfo(String name, T task, ICondition<Long> activationCondition, boolean recurrent, boolean async)
        {
            Assert.notNull(name);
            Assert.notNull(task);
            Assert.notNull(activationCondition);
            
            this.name = name;
            this.task = task;
            this.activationCondition = activationCondition;
            this.recurrent = recurrent;
            this.async = async;
        }
        
        public String getName()
        {
            return name;
        }
        
        public T getTask()
        {
            return task;
        }

        public ICondition<Long> getActivationCondition()
        {
            return activationCondition;
        }
        
        public boolean isRecurrent()
        {
            return recurrent;
        }
        
        public boolean isCompleted()
        {
            return completed;
        }
        
        public void setCompleted()
        {
            completed = true;
            active = false;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;

            if (!(o instanceof TaskInfo))
                return false;
            
            TaskInfo taskInfo = (TaskInfo)o;
            return name.equals(taskInfo.name);
        }
        
        @Override
        public int hashCode()
        {
            return Objects.hashCode(name);
        }
        
        @Override
        public String toString()
        {
            return name;
        }
    }
    
    private static class TaskHandler<T> implements ITaskHandler<TaskInfo<T>>
    {
        private final ITaskHandler<T> taskHandler;

        public TaskHandler(ITaskHandler<T> taskHandler)
        {
            Assert.notNull(taskHandler);
            
            this.taskHandler = taskHandler;
        }
        
        @Override
        public void handle(TaskInfo<T> task)
        {
            taskHandler.handle(task.getTask());
        }
    }

    private static class TaskContext implements ITaskContext
    {
        private final Map<String, Object> parameters = new HashMap<String, Object>();

        @Override
        public Map<String, Object> getParameters()
        {
            return parameters;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Task scheduler ''{0}'' already started.")
        ILocalizedMessage alreadyStarted(Object scheduler);
        @DefaultMessage("Task ''{1}'' is added to scheduler ''{0}''.")
        ILocalizedMessage taskAdded(Object scheduler, Object task);
        @DefaultMessage("Task ''{1}'' is removed from scheduler ''{0}''.")
        ILocalizedMessage taskRemoved(Object scheduler, Object task);
        @DefaultMessage("All tasks are removed from scheduler ''{0}''.")
        ILocalizedMessage allTasksRemoved(Object scheduler);
        @DefaultMessage("Task ''{1}'' is starting in scheduler ''{0}''.")
        ILocalizedMessage taskStarting(Object scheduler, Object task);
        @DefaultMessage("Task ''{1}'' is completed in scheduler ''{0}''.")
        ILocalizedMessage taskCompleted(Object scheduler, Object task);
        @DefaultMessage("Task ''{1}'' is failed in scheduler ''{0}'' with exception ''{2}''.")
        ILocalizedMessage taskFailed(Object scheduler, Object task, Object error);
    }
}
