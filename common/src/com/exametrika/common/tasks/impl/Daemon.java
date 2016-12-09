/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import java.lang.ref.WeakReference;

import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.common.utils.InvalidStateException;

/**
 * The {@link Daemon} represents a daemon thread.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author AndreyM
 */
public final class Daemon implements ILifecycle
{
    private static final ILogger logger = Loggers.get(Daemon.class);
    private final Runnable task;
    private final String name;
    private final Object data;
    private volatile DaemonThread daemonThread;
    private boolean started;
    private volatile boolean stopped;
    
    /**
     * Creates a new object.
     * 
     * @param task task to execute
     */
    public Daemon(Runnable task)
    {
        this(task, null, null);
    }
    
    /**
     * Creates a new object.
     *
     * @param task task to execute
     * @param name daemon name. Can be null, if name is not set
     * @param data user data attached to daemon thread. Can be null
     */
    public Daemon(Runnable task, String name, Object data)
    {
        Assert.notNull(task);
        
        this.task = task;
        if (name == null)
            this.name = "Daemon";
        else
            this.name = name;
        this.data = data;
    }

    public long getId()
    {
        Thread daemonThread = this.daemonThread;
        if (daemonThread != null)
            return daemonThread.getId();
        
        return 0;
    }
    
    @Override
    public synchronized void start()
    {
        if (started || stopped)
            throw new InvalidStateException();
        
        daemonThread = new DaemonThread(name, task, data);
        daemonThread.setDaemon(true);
        daemonThread.start();
        started = true;
    }

    public void requestToStop()
    {
        Thread daemonThread = this.daemonThread;
        if (daemonThread != null)
            daemonThread.interrupt();
    }
    
    public void join()
    {
        try
        {
            Thread daemonThread = this.daemonThread;
            if (daemonThread != null)
                daemonThread.join();
        }
        catch (InterruptedException e) 
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        }
    }
    
    @Override
    public void stop()
    {
        if (stopped)
            return;
        
        DaemonThread daemonThread = this.daemonThread;
        if (daemonThread != null && daemonThread.getId() == Thread.currentThread().getId())
            return;
        
        synchronized (this)
        {
            if (stopped)
                return;
            
            daemonThread = this.daemonThread;
            this.daemonThread = null;
            stopped = true;
            
            if (daemonThread == null)
                return;
        }
        
        daemonThread.interrupt();
        
        try
        {
            daemonThread.join();
        }
        catch (InterruptedException e) 
        {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        }
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
    
    private static final class DaemonThread extends Thread implements IThreadDataProvider
    {
        private final Runnable task;
        private final WeakReference<Object> data;

        public DaemonThread(String name, Runnable task, Object data)
        {
            super(name);
            this.task = task;
            this.data = new WeakReference<Object>(data);
        }
        
        @Override
        public Object getData()
        {
            return data.get();
        }
        
        @Override
        public void run()
        {
            try
            {
                task.run();
            }
            catch (ThreadInterruptedException e)
            {
            }
            catch (Exception e)
            {
                if (!Thread.currentThread().isInterrupted())
                {
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, e);
                }
            }
        }
    }
}
