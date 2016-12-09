/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.compartment.impl;

import java.util.List;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentGroup;
import com.exametrika.common.compartment.ICompartmentGroupFactory;
import com.exametrika.common.compartment.ICompartmentGroupProcessor;
import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ITaskHandler;
import com.exametrika.common.tasks.ITaskListener;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.tasks.impl.TaskExecutor;
import com.exametrika.common.tasks.impl.TaskQueue;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;



/**
 * The {@link CompartmentGroup} is a compartment group implementation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompartmentGroup implements ICompartmentGroup, ITimerListener, ITimeService
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(CompartmentGroup.class);
    private final String name;
    private final TaskExecutor<Task> taskExecutor;
    private final Timer timer;
    private final ITimeService timeService;
    private final List<ICompartmentGroupProcessor> processors;
    private final TaskQueue<Task> tasks;
    private final IMarker marker;
    private volatile long currentTime;
    private volatile boolean started;
    private volatile boolean stopped;
    private volatile boolean stopRequested;
    
    public CompartmentGroup(ICompartmentGroupFactory.Parameters parameters)
    {
        Assert.notNull(parameters);
        Assert.notNull(parameters.name);
        Assert.notNull(parameters.processors);
        Assert.notNull(parameters.timeService);
        
        name = parameters.name;
        marker = Loggers.getMarker(name);
        timeService = parameters.timeService;
        currentTime = timeService.getCurrentTime();
        tasks = new TaskQueue<Task>(parameters.taskQueueCapacity, 0);
        processors = parameters.processors;
        CompartmentTaskHandler handler = new CompartmentTaskHandler();
        taskExecutor = new TaskExecutor<Task>(parameters.threadCount, tasks, handler, 
            "[" + name + "] compartment group task thread");
        taskExecutor.addTaskListener(handler);
        timer = new Timer(parameters.timerPeriod, this, false, "[" + name + "] compartment group timer thread", null);
    }
    
    public IMarker getMarker()
    {
        return marker;
    }
    
    public boolean isStopRequested()
    {
        return stopRequested;
    }
    
    public boolean execute(ICompartment compartment, ICompartmentTask task)
    {
        Assert.notNull(task);
        
        return tasks.offer(new CompartmentTask(compartment, task));
    }
    
    public boolean execute(Runnable task)
    {
        Assert.notNull(task);
        
        return tasks.offer(new CompartmentRunnableTask(task));
    }

    @Override
    public String getName()
    {
        return name;
    }
    
    @Override
    public long getCurrentTime()
    {
        return currentTime;
    }
    
    @Override
    public long getTimerPeriod()
    {
        return timer.getPeriod();
    }

    @Override
    public void setTimerPeriod(long period)
    {
        timer.setPeriod(period);
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
    public synchronized void start()
    {
        Assert.checkState(!started);
        
        try
        {
            currentTime = timeService.getCurrentTime();
            timer.start();
            taskExecutor.start();
            started = true;
            
            //Managements.register(MBEAN_NAME, name, this);
        }
        catch (Exception e)
        {
            stop();

            Exceptions.wrapAndThrow(e);
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.started());
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
        
        stopRequested = true;
        
        //Managements.unregister(MBEAN_NAME, name);
        
        taskExecutor.stop();
        timer.stop();

        synchronized (this)
        {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.stopped());
        }
    }
    
    @Override
    public void onTimer()
    {
        currentTime = timeService.getCurrentTime();
        
        for (ICompartmentGroupProcessor processor : processors)
            processor.onTimer(currentTime);
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
    @Override
    protected void finalize()
    {
        stop();
    }
    
    private abstract class Task
    {
        public abstract void execute();
        public abstract void onCompleted();
        public abstract void onFailed(Throwable error);
    }

    private class CompartmentTask extends Task
    {
        private final ICompartment compartment;
        private final ICompartmentTask task;
        private Object result;
        private Throwable error;
        
        public CompartmentTask(ICompartment compartment, ICompartmentTask task)
        {
            Assert.notNull(compartment);
            Assert.notNull(task);
            
            this.compartment = compartment;
            this.task = task;
        }
        
        @Override
        public void execute()
        {
            result = task.execute();
        }
        
        @Override
        public void onCompleted()
        {
            compartment.offer(new CompartmentResult(task, result, error));
        }
        
        @Override
        public void onFailed(Throwable error)
        {
            this.error = error;
            onCompleted();
        }
    }

    private class CompartmentRunnableTask extends Task
    {
        private final Runnable task;
        
        public CompartmentRunnableTask(Runnable task)
        {
            Assert.notNull(task);
            
            this.task = task;
        }
        
        @Override
        public void execute()
        {
            task.run();
        }
        
        @Override
        public void onCompleted()
        {
        }
        
        @Override
        public void onFailed(Throwable error)
        {
        }
    }
    
    private class CompartmentTaskHandler implements ITaskHandler<Task>, ITaskListener<Task>
    {
        @Override
        public void handle(Task task)
        {
            task.execute();
        }

        @Override
        public void onTaskStarted(Task task)
        {
        }

        @Override
        public void onTaskCompleted(Task task)
        {
            task.onCompleted();
        }

        @Override
        public void onTaskFailed(Task task, Throwable error)
        {
            task.onFailed(error);
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Compartment group has been started.")
        ILocalizedMessage started();
        @DefaultMessage("Compartment group has been stopped.")
        ILocalizedMessage stopped();
    }
}
