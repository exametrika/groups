/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ITaskExecutor;
import com.exametrika.common.tasks.ITaskHandler;
import com.exametrika.common.tasks.ITaskListener;
import com.exametrika.common.tasks.ITaskSource;
import com.exametrika.common.tasks.IThreadPoolTaskExecutor;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.common.utils.InvalidArgumentException;


/**
 * The {@link TaskExecutor} is an implementation of {@link ITaskExecutor} interface.
 * 
 * @param <T> task type
 * @see ITaskExecutor
 * @threadsafety This class and its methods are thread safe.
 * @author AndreyM
 */
public final class TaskExecutor<T> implements IThreadPoolTaskExecutor<T>, ILifecycle
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ILogger logger;
    private final String name;
    private final Object data;
    private volatile LinkedHashSet<ITaskListener> listeners = new LinkedHashSet<ITaskListener>();
    private volatile int threadCount;
    private List<ExecutorThread<T>> threads;
    private final ITaskHandler<T> taskHandler;
    private final ITaskSource<T> taskSource;
    private boolean started;
    private boolean stopped;
    
    /**
     * Creates a new executor.
     *
     * @param threadCount count of threads in a thread pool of executor
     * @param taskSource task source
     * @param taskHandler task handler
     * @param name executor name. Can be null, if name is not set
     * @exception InvalidArgumentException if threadCount < 0
     */
    public TaskExecutor(int threadCount, ITaskSource<T> taskSource, ITaskHandler<T> taskHandler, String name)
    {
        this(threadCount, taskSource, taskHandler, name, true, null);
    }
    
    /**
     * Creates a new executor.
     *
     * @param threadCount count of threads in a thread pool of executor
     * @param taskSource task source
     * @param taskHandler task handler
     * @param name executor name. Can be null, if name is not set
     * @param useLogging if true logging will be used
     * @param data user data attached to all thread from pool. Can be null
     * @exception InvalidArgumentException if threadCount < 0
     */
    public TaskExecutor(int threadCount, ITaskSource<T> taskSource, ITaskHandler<T> taskHandler, String name, boolean useLogging,
        Object data)
    {   
        Assert.notNull(taskSource);
        Assert.notNull(taskHandler);

        if (threadCount < 0)
            throw new InvalidArgumentException();
        
        if (name == null)
            this.name = "Executor";
        else
            this.name = name;
        this.data = data;
        this.threadCount = threadCount;
        this.taskSource = taskSource;
        this.taskHandler = taskHandler;
        if (taskHandler instanceof ITaskListener)
            listeners.add((ITaskListener)taskHandler);
        
        if (useLogging)
            logger = Loggers.get(TaskExecutor.class);
        else
            logger = null;
    }
    
    @Override
    public int getThreadCount()
    {
        return threadCount;
    }
    
    @Override
    public synchronized void setThreadCount(int threadCount)
    {
        if (stopped)
            return;
        if (threadCount < 0)
            throw new InvalidArgumentException();
        
        if (this.threadCount == threadCount)
            return;
        
        this.threadCount = threadCount;
        
        if (started)
        {
            if (threads.size() < threadCount)
            {
                for (int i = threads.size(); i < threadCount; i++)
                    threads.add(startThread(i));
            }
            else if (threads.size() > threadCount)
            {
                for (int i = threads.size() - 1; i >= threadCount; i--)
                {
                    threads.get(i).requestToStop();
                    threads.remove(i);
                }
            }
        }
    }

    @Override
    public void addTaskListener(ITaskListener<T> listener)
    {
        Assert.notNull(listener);
        
        synchronized (this)
        {
            LinkedHashSet<ITaskListener> listeners = (LinkedHashSet<ITaskListener>)this.listeners.clone();
            listeners.add(listener);
    
            this.listeners = listeners;
        }
        
        if (logger != null && logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.listenerAdded(name, listener));
    }

    @Override
    public void removeTaskListener(ITaskListener<T> listener)
    {
        Assert.notNull(listener);
        
        synchronized (this)
        {
            if (!listeners.contains(listener))
                return;
            
            LinkedHashSet<ITaskListener> listeners = (LinkedHashSet<ITaskListener>)this.listeners.clone();
            listeners.remove(listener);
    
            this.listeners = listeners;
        }
        
        if (logger != null && logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.listenerRemoved(name, listener));
    }
    
    @Override
    public void removeAllTaskListeners()
    {
        this.listeners = new LinkedHashSet<ITaskListener>();
        
        if (logger != null && logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.allListenersRemoved(name));
    }

    @Override
    public synchronized void start()
    {
        if (started || stopped)
            return;
        
        List<ExecutorThread<T>> threads = new ArrayList<ExecutorThread<T>>();
        for (int i = 0; i < threadCount; i++)
            threads.add(startThread(i));
        
        this.threads = threads;
        started = true;
    }

    @Override
    public void stop()
    {
        List<ExecutorThread<T>> threads;
        synchronized (this)
        {
            if (!started || stopped)
                return;
            
            threads = this.threads;
            this.threads = null;
            stopped = true;
        }
        
        // Stop threads
        for (ExecutorThread thread : threads)
            thread.stopProcessing(true);
    }

    public synchronized void requestToStop()
    {
        if (stopped)
            return;
        
        for (ExecutorThread thread : threads)
            thread.stopProcessing(false);
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
    
    private ExecutorThread startThread(int threadNumber)
    {
        ExecutorThread thread = new ExecutorThread(name + "-" + threadNumber, this, logger, data);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void taskStarted(T task) 
    {
        Set<ITaskListener> taskListeners = listeners;
        for (ITaskListener listener : taskListeners)
        {
            try
            {
                listener.onTaskStarted(task);
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception from other listeners
                if (logger != null && logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }
    }
    
    private void taskCompleted(T task)
    {
        Set<ITaskListener> taskListeners = listeners;
        for (ITaskListener listener : taskListeners)
        {
            try
            {
                listener.onTaskCompleted(task);
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception from other listeners
                if (logger != null && logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }
    }
    
    private void taskFailed(T task, Throwable error)
    {
        Set<ITaskListener> taskListeners = listeners;
        for (ITaskListener listener : taskListeners)
        {
            try
            {
                listener.onTaskFailed(task, error);
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception from other listeners
                if (logger != null && logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }
    }

    private static final class ExecutorThread<T> extends Thread implements IThreadDataProvider
    {
        private boolean active;
        private volatile boolean stopRequested;
        private final WeakReference<TaskExecutor> executor;
        private final ILogger logger;
        private final WeakReference<Object> data;
        
        public ExecutorThread(String name, TaskExecutor executor, ILogger logger, Object data)
        {
            super(name);
            
            this.executor = new WeakReference<TaskExecutor>(executor);
            this.logger = logger;
            this.data = new WeakReference<Object>(data);
        }

        @Override
        public Object getData()
        {
            return data.get();
        }
        
        public synchronized void requestToStop()
        {
            stopRequested = true;
            if (!active)
                interrupt();
        }
        
        public void stopProcessing(boolean wait)
        {
            requestToStop();
            
            try
            {
                if (wait)
                    join();
            }
            catch (InterruptedException e) 
            {
                if (logger != null)
                {
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, e);
                }
                else
                    e.printStackTrace();
            }
        }
        
        @Override
        public void run()
        {
            while (!Thread.currentThread().isInterrupted() && !stopRequested)
            {
                try
                {
                    TaskExecutor<T> executor = this.executor.get();
                    if (executor == null)
                        break;

                    ITaskSource<T> taskSource = executor.taskSource;
                    executor = null;
                    
                    T task = taskSource.take();
                    
                    synchronized (this)
                    {
                        // Store interrupted flag and clear it in thread to allow normal handling of tasks
                        stopRequested = Thread.interrupted();
                        active = true;
                    }
                    
                    if (!stopRequested)
                        handleTask(task);
                }
                catch (ThreadInterruptedException e)
                {
                    break;
                }
                catch (Exception e)
                {
                    if (logger != null)
                    {
                        if (logger.isLogEnabled(LogLevel.ERROR))
                            logger.log(LogLevel.ERROR, e);
                    }
                    else
                        e.printStackTrace();
                }
                finally
                {
                    synchronized (this)
                    {
                        active = false;
                    }
                }
            }
        }
        
        private void handleTask(T task)
        {
            TaskExecutor<T> executor = this.executor.get();
            if (executor == null)
                return;
            
            try
            {
                executor.taskStarted(task);
                executor.taskHandler.handle(task);
                executor.taskCompleted(task);
            }
            catch (ThreadInterruptedException e)
            {
                // Thread interruption is not considered as a task fault
            }
            catch (Exception e)
            {
                if (!Thread.currentThread().isInterrupted())
                {
                    executor.taskFailed(task, e);
                    if (logger != null)
                    {
                        if (logger.isLogEnabled(LogLevel.ERROR))
                            logger.log(LogLevel.ERROR, e);
                    }
                    else
                        e.printStackTrace();
                }
            }
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Task listener ''{1}'' is added to executor ''{0}''.")
        ILocalizedMessage listenerAdded(Object executor, Object listener);
        @DefaultMessage("Task listener ''{1}'' is removed from executor ''{0}''.")
        ILocalizedMessage listenerRemoved(Object executor, Object listener);
        @DefaultMessage("All task listeners are removed from executor ''{0}''.")
        ILocalizedMessage allListenersRemoved(Object executor);        
    }
}
