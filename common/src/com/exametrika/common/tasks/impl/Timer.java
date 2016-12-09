/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import java.lang.ref.WeakReference;
import java.util.LinkedHashSet;
import java.util.Set;

import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ITimer;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.common.utils.InvalidStateException;


/**
 * The {@link Timer} is an implementation of {@link ITimer} interface.
 * 
 * @see ITimer
 * @threadsafety This class and its methods are thread safe.
 * @author AndreyM
 */
public final class Timer implements ITimer, ILifecycle
{
    private static final ILogger logger = Loggers.get(Timer.class);
    private final String name;
    private final Object data;
    private volatile long period;
    private volatile LinkedHashSet<ITimerListener> listeners = new LinkedHashSet<ITimerListener>();
    private TimerThread timerThread;
    private boolean started;
    private volatile boolean stopped;
    private boolean suspended;
    
    /**
     * Creates a new object.
     *
     * @param period timer period in milliseconds
     */
    public Timer(long period)
    {
        this(period, null, false, null, null);
    }
    
    /**
     * Creates a new object.
     *
     * @param period timer period in milliseconds
     * @param listener initial timer listener. Can be null
     * @param suspended is timer initially suspended?
     * @param name timer name. Can be null, if name is not set
     * @param data user data attached to timer thread. Can be null
     */
    public Timer(long period, ITimerListener listener, boolean suspended, String name, Object data)
    {
        this.period = period;
        this.data = data;
        if (name == null)
            this.name = "Timer";
        else
            this.name = name;
        
        this.suspended = suspended;
        
        if (listener != null)
            addTimerListener(listener);
    }

    @Override
    public long getPeriod()
    {
        return period;
    }
    
    @Override
    public void setPeriod(long period)
    {
        this.period = period;
    }
    
    @Override
    public synchronized void suspend()
    {
        if (timerThread != null)
            timerThread.suspendThread();
        else
            suspended = true;
    }
    
    @Override
    public synchronized void resume()
    {
        if (timerThread != null)
            timerThread.resumeThread();
        else
            suspended = false;
    }
    
    @Override
    public synchronized void signal()
    {
        if (timerThread != null)
            timerThread.signalThread();
    }
    
    @Override
    public void addTimerListener(ITimerListener listener)
    {
        Assert.notNull(listener);
        
        synchronized (this)
        {
            LinkedHashSet<ITimerListener> listeners = (LinkedHashSet<ITimerListener>)this.listeners.clone();
            listeners.add(listener);
    
            this.listeners = listeners;
        }
    }

    @Override
    public void removeTimerListener(ITimerListener listener)
    {
        Assert.notNull(listener);
        
        synchronized (this)
        {
            if (!listeners.contains(listener))
                return;
            
            LinkedHashSet<ITimerListener> listeners = (LinkedHashSet<ITimerListener>)this.listeners.clone();
            listeners.remove(listener);
    
            this.listeners = listeners;
        }
    }
    
    @Override
    public void removeAllTimerListeners()
    {
        this.listeners = new LinkedHashSet<ITimerListener>();
    }

    @Override
    public synchronized void start()
    {
        if (started || stopped)
            throw new InvalidStateException();
        
        timerThread = new TimerThread(name, period, this, suspended, data);
        timerThread.setDaemon(true);
        timerThread.start();
        started = true;
    }

    @Override
    public void stop()
    {
        if (stopped)
            return;
        
        TimerThread timerThread = this.timerThread;
        if (timerThread != null && timerThread.getId() == Thread.currentThread().getId())
        {
            timerThread.stopRequested = true;
            return;
        }
        
        synchronized (this)
        {
            if (stopped)
                return;
            
            timerThread = this.timerThread;
            this.timerThread = null;
            stopped = true;
            
            if (timerThread == null)
                return;
        }
        
        timerThread.requestToStop();
        
        try
        {
            timerThread.join();
        }
        catch (InterruptedException e) 
        {
            e.printStackTrace(System.err);
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
    
    private boolean onTimer()
    {
        Set<ITimerListener> listeners = this.listeners;
        
        for (ITimerListener listener : listeners)
        {
            try
            {
                listener.onTimer();
            }
            catch (ThreadInterruptedException e)
            {
                return false;
            }
            catch (Exception e)
            {
                if (!Thread.currentThread().isInterrupted())
                {
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, e);
                }
                else
                    return false;
            }
        }
        
        return true;
    }
    
    private static final class TimerThread extends Thread implements IThreadDataProvider
    {
        private final long period;
        private final WeakReference<Timer> timer;
        private final WeakReference<Object> data;
        private volatile boolean stopRequested;
        private final Object suspendSync = new Object();
        private final Object timerSync = new Object(); 
        private boolean suspended;

        public TimerThread(String name, long period, Timer timer, boolean suspended, Object data)
        {
            super(name);
            this.period = period;
            this.data = new WeakReference<Object>(data);
            this.timer = new WeakReference<Timer>(timer);
            this.suspended = suspended;
        }

        @Override
        public Object getData()
        {
            return data.get();
        }
        
        public void suspendThread()
        {
            synchronized (suspendSync)
            {
                suspended = true;
            }
        }
        
        public void resumeThread()
        {
            synchronized (suspendSync)
            {
                suspended = false;
                suspendSync.notify();
            }
        }
        
        public void signalThread()
        {
            synchronized (timerSync)
            {
                timerSync.notify();
            }
        }

        public void requestToStop()
        {
            stopRequested = true;
            signalThread();
            resumeThread();
        }
        
        @Override
        public void run()
        {
            while (!Thread.currentThread().isInterrupted() && !stopRequested)
            {
                try
                {
                    synchronized (suspendSync)
                    {
                        while (suspended)
                            suspendSync.wait();
                    }
                }
                catch (InterruptedException e)
                {
                    break;
                }
    
                try
                {
                    synchronized (timerSync)
                    {
                        timerSync.wait(period);
                    }
                }
                catch (InterruptedException e)
                {
                    break;
                }
                
                Timer timer = this.timer.get();
                if (timer == null)
                    break;
                
                if (stopRequested || !timer.onTimer())
                    break;
            }
        }
    }
}
