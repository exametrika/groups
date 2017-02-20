/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.compartment.impl;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentDispatcher;
import com.exametrika.common.compartment.ICompartmentFactory;
import com.exametrika.common.compartment.ICompartmentGroup;
import com.exametrika.common.compartment.ICompartmentGroupFactory;
import com.exametrika.common.compartment.ICompartmentProcessor;
import com.exametrika.common.compartment.ICompartmentQueue;
import com.exametrika.common.compartment.ICompartmentQueue.Event;
import com.exametrika.common.compartment.ICompartmentSizeEstimator;
import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tasks.Tasks;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.tasks.impl.Daemon;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.IOs;



/**
 * The {@link Compartment} is a compartment implementation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Compartment implements ICompartment, Runnable
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(Compartment.class);
    private final String name;
    private final Daemon mainThread;
    private final ICompartmentDispatcher dispatcher;
    private volatile ArrayList<ICompartmentTimerProcessor> timerProcessors;
    private volatile ArrayList<ICompartmentProcessor> processors;
    private volatile long dispatchPeriod;
    private final ICompartmentQueue queue;
    private final boolean enableDirectCalls;
    private int minLockQueueCapacity;
    private int maxUnlockQueueCapacity;
    private volatile int taskBatchSize;
    private final IFlowController flowController;
    private final ICompartmentSizeEstimator sizeEstimator;
    private final CompartmentGroup group;
    private final IMarker marker;
    private final boolean groupOwner;
    private volatile boolean started;
    private volatile boolean stopped;
    private volatile boolean stopRequested;
    private boolean flowLocked;
    private int lockCount;
    private long lastTimerTime;
    
    public Compartment(ICompartmentFactory.Parameters parameters)
    {
        Assert.notNull(parameters);
        Assert.notNull(parameters.name);
        Assert.notNull(parameters.dispatcher);
        Assert.notNull(parameters.timerProcessors);
        Assert.notNull(parameters.processors);
        Assert.notNull(parameters.flowController);
        Assert.notNull(parameters.sizeEstimator);
        Assert.notNull(parameters.queue);
        Assert.isTrue(parameters.taskBatchSize > 0);
        
        name = parameters.name;
        marker = Loggers.getMarker(name);
        dispatcher = parameters.dispatcher;
        dispatcher.setCompartment(this);
        timerProcessors = new ArrayList<ICompartmentTimerProcessor>(parameters.timerProcessors);
        processors = new ArrayList<ICompartmentProcessor>(parameters.processors);
        dispatchPeriod = parameters.dispatchPeriod;
        minLockQueueCapacity = parameters.minLockQueueCapacity;
        maxUnlockQueueCapacity = parameters.maxUnlockQueueCapacity;
        taskBatchSize = parameters.taskBatchSize;
        flowController = parameters.flowController;
        sizeEstimator = parameters.sizeEstimator;
        queue = parameters.queue;
        enableDirectCalls = queue instanceof SimpleCompartmentQueue;
        if (parameters.group != null)
        {
            group = (CompartmentGroup)parameters.group;
            groupOwner = parameters.groupOwner;
        }
        else
        {
            ICompartmentGroupFactory.Parameters groupParameters = new ICompartmentGroupFactory.Parameters();
            groupParameters.name = parameters.name;
            groupParameters.timerPeriod = parameters.dispatchPeriod;
            group = new CompartmentGroup(groupParameters);
            groupOwner = true;
        }
        
        mainThread = new Daemon(this, "[" + name + "] compartment main thread", this);
    }
    
    public IMarker getMarker()
    {
        return marker;
    }
    
    public ICompartmentQueue getQueue()
    {
        return queue;
    }
    
    public boolean isMainThread()
    {
        return Thread.currentThread().getId() == mainThread.getId();
    }
    
    public boolean isStopRequested()
    {
        return stopRequested;
    }
    
    @Override
    public String getName()
    {
        return name;
    }
    
    @Override
    public ICompartmentGroup getGroup()
    {
        return group;
    }
    
    @Override
    public long getDispatchPeriod()
    {
        return dispatchPeriod;
    }

    @Override
    public void setDispatchPeriod(long period)
    {
        dispatchPeriod = period;
    }

    @Override
    public synchronized int getMinLockQueueCapacity()
    {
        return minLockQueueCapacity;
    }

    @Override
    public synchronized void setMinLockQueueCapacity(int value)
    {
        minLockQueueCapacity = value;
    }

    @Override
    public synchronized int getMaxUnlockQueueCapacity()
    {
        return maxUnlockQueueCapacity;
    }

    @Override
    public synchronized void setMaxUnlockQueueCapacity(int value)
    {
        maxUnlockQueueCapacity = value;
    }

    @Override
    public int getTaskBatchSize()
    {
        return taskBatchSize;
    }

    @Override
    public void setTaskBatchSize(int value)
    {
        Assert.isTrue(value > 0);
        taskBatchSize = value;
    }

    @Override
    public long getCurrentTime()
    {
        return group.getCurrentTime();
    }
    
    @Override
    public synchronized void addTimerProcessor(ICompartmentTimerProcessor processor)
    {
        Assert.notNull(processor);
        
        ArrayList<ICompartmentTimerProcessor> processors = (ArrayList<ICompartmentTimerProcessor>)this.timerProcessors.clone();
        processors.add(processor);
        this.timerProcessors = processors;
    }

    @Override
    public synchronized void removeTimerProcessor(ICompartmentTimerProcessor processor)
    {
        Assert.notNull(processor);
        
        ArrayList<ICompartmentTimerProcessor> processors = (ArrayList<ICompartmentTimerProcessor>)this.timerProcessors.clone();
        processors.remove(processor);
        this.timerProcessors = processors;
    }

    @Override
    public synchronized void addProcessor(ICompartmentProcessor processor)
    {
        Assert.notNull(processor);
        
        ArrayList<ICompartmentProcessor> processors = (ArrayList<ICompartmentProcessor>)this.processors.clone();
        processors.add(processor);
        this.processors = processors;
    }

    @Override
    public synchronized void removeProcessor(ICompartmentProcessor processor)
    {
        Assert.notNull(processor);
        
        ArrayList<ICompartmentProcessor> processors = (ArrayList<ICompartmentProcessor>)this.processors.clone();
        processors.remove(processor);
        this.processors = processors;
    }
    
    @Override
    public void offer(ICompartmentTask task)
    {
        Assert.notNull(task);

        if (enableDirectCalls && isMainThread())
            executeInMainThread(task);
        else 
        {
            ICompartment compartment = getCurrentCompartment();
            Assert.checkState(compartment != null);
            offerFromCompartmentThread(task, compartment);
        }
    }
    
    @Override
    public void offer(Runnable task)
    {
        Assert.notNull(task);

        if (enableDirectCalls && isMainThread())
            executeInMainThread(task);
        else
        {
            ICompartment compartment = getCurrentCompartment();
            if (compartment != null)
                offerFromCompartmentThread(task);
            else
                offerFromExternalThread(task);
        }
    }
    
    @Override
    public void offer(List<?> tasks)
    {
        Assert.notNull(tasks);
        if (tasks.isEmpty())
            return;
        
        Object first = tasks.get(0);
        boolean runnable;
        
        if (first instanceof Runnable)
            runnable = true;
        else
        {
            Assert.isTrue(first instanceof ICompartmentTask);
            runnable = false;
        }
        
        if (enableDirectCalls && isMainThread())
            executeInMainThread(tasks, runnable);
        else
        {
            ICompartment compartment = getCurrentCompartment();
            if (compartment != null)
                offerFromCompartmentThread(tasks, compartment, runnable);
            else if (runnable)
                offerFromExternalThread(tasks);
            else
                Assert.checkState(false);
        }
    }

    @Override
    public boolean execute(ICompartmentTask task)
    {
        return group.execute(this, task);
    }
    
    @Override
    public boolean execute(Runnable task)
    {
        return group.execute(task);
    }

    @Override
    public void wakeup()
    {
        dispatcher.wakeup();
    }
    
    @Override
    public synchronized void start()
    {
        Assert.checkState(!started);
        
        if (groupOwner)
            group.start();
        
        try
        {
            mainThread.start();
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
            dispatcher.beforeClose();

            stopRequested = true;
            
            notifyAll();
        }
        
        //Managements.unregister(MBEAN_NAME, name);
        
        dispatcher.wakeup();
        
        mainThread.join();
        
        if (groupOwner)
            group.stop();

        synchronized (this)
        {
            IOs.close(dispatcher);
            
            mainThread.stop();
    
            if (flowLocked)
                flowController.unlockFlow(null);
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.stopped());
        }
    }
    
    @Override
    public void run()
    {
        while (!dispatcher.canFinish(stopRequested))
        {
            try
            {
                dispatch();
                
                processEvents();
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, marker, e);
            }
        }
    }

    @Override
    public synchronized void lockFlow(Object flow)
    {
        lockCount++;
    }

    @Override
    public synchronized void unlockFlow(Object flow)
    {
        lockCount--;
        Assert.checkState(lockCount >= 0);
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
    
    private void dispatch()
    {
        int capacity;
        synchronized (this)
        {
            capacity = queue.getCapacity();
        }
        
        dispatcher.block(capacity == 0 ? dispatchPeriod : 0);
    }

    private void processEvents()
    {
        int count = taskBatchSize;
        for (int i = 0; i < count; i++)
        {
            Event event = poll(i == 0);
            if (event == null)
                break;

            if (event.task instanceof Runnable)
                executeInMainThread((Runnable)event.task);
            else if (event.task instanceof ICompartmentTask)
                executeInMainThread((ICompartmentTask)event.task, event.compartment);
            else if (event.task instanceof CompartmentTaskList)
            {
                CompartmentTaskList list = (CompartmentTaskList)event.task;
                executeInMainThread(list.getTasks(), event.compartment, list.isRunnable());
                break;
            }
            else
                Assert.error();
        }
        
        if (!processors.isEmpty())
        {
            for (ICompartmentProcessor processor : processors)
                processor.process();
        }
        
        long currentTime = getCurrentTime();
        
        if (lastTimerTime == 0)
            lastTimerTime = currentTime;
        else if (currentTime > lastTimerTime + dispatchPeriod)
        {
            for (ICompartmentTimerProcessor processor : timerProcessors)
                processor.onTimer(currentTime);
            
            lastTimerTime = currentTime;
        }
    }

    private void executeInMainThread(ICompartmentTask task)
    {
        try
        {
            Object result = task.execute();
            task.onSucceeded(result);
        }
        catch (ThreadInterruptedException e)
        {
        }
        catch (Exception e)
        {
            if (!Thread.currentThread().isInterrupted())
            {
                task.onFailed(e);
                if (logger != null && logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }
    }
    
    private void executeInMainThread(ICompartmentTask task, ICompartment compartment)
    {
        try
        {
            Object result = task.execute();
            compartment.offer(new CompartmentResult(task, result, null));
        }
        catch (ThreadInterruptedException e)
        {
        }
        catch (Exception e)
        {
            if (!Thread.currentThread().isInterrupted())
            {
                compartment.offer(new CompartmentResult(task, null, e));
                if (logger != null && logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }
    }
    
    private void executeInMainThread(ICompartmentTask task, List<CompartmentResult> results)
    {
        try
        {
            Object result = task.execute();
            results.add(new CompartmentResult(task, result, null));
        }
        catch (ThreadInterruptedException e)
        {
        }
        catch (Exception e)
        {
            if (!Thread.currentThread().isInterrupted())
            {
                results.add(new CompartmentResult(task, null, e));
                if (logger != null && logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }
    }

    private void executeInMainThread(Runnable task)
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
                if (logger != null && logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }
    }

    private void executeInMainThread(List<?> tasks, boolean runnable)
    {
        for (Object task : tasks)
        {
            if (runnable)
                executeInMainThread((Runnable)task);
            else
                executeInMainThread((ICompartmentTask)task);
        }
    }
    
    private void executeInMainThread(List<?> tasks, ICompartment compartment, boolean runnable)
    {
        if (runnable)
        {
            for (Object task : tasks)
                executeInMainThread((Runnable)task);
        }
        else
        {
            List<CompartmentResult> results = new ArrayList<CompartmentResult>(tasks.size());
            for (Object task : tasks)
                executeInMainThread((ICompartmentTask)task, results);
            
            compartment.offer(results);
        }
    }
    
    private ICompartment getCurrentCompartment()
    {
        Object data = Tasks.getCurrentThreadData();
        if (data != null && data instanceof ICompartment)
            return (ICompartment)data;
        else
            return null;
    }

    private synchronized void offerFromCompartmentThread(ICompartmentTask task, ICompartment compartment)
    {
        int size = sizeEstimator.estimateSize(task);
        queue.offer(new Event(task, compartment, size));
        
        if (!flowLocked && queue.getCapacity() >= minLockQueueCapacity)
        {
            flowLocked = true;
            flowController.lockFlow(null);
        }
        
        dispatcher.wakeup();
    }

    private synchronized void offerFromCompartmentThread(Runnable task)
    {
        int size = estimateSize(task);
        queue.offer(new Event(task, null, size));
        
        if (!flowLocked && queue.getCapacity() >= minLockQueueCapacity)
        {
            flowLocked = true;
            flowController.lockFlow(null);
        }
        
        dispatcher.wakeup();
    }

    private synchronized void offerFromCompartmentThread(List<?> tasks, ICompartment compartment, boolean runnable)
    {
        int size = estimateSize(tasks, runnable);
        queue.offer(new Event(new CompartmentTaskList(tasks, runnable), compartment, size));
        
        if (!flowLocked && queue.getCapacity() >= minLockQueueCapacity)
        {
            flowLocked = true;
            flowController.lockFlow(null);
        }
        
        dispatcher.wakeup();
    }

    private synchronized void offerFromExternalThread(Runnable task)
    {
        int size = estimateSize(task);
        Assert.isTrue(size < minLockQueueCapacity);
        
        while (!stopRequested && queue.getCapacity() + size >= minLockQueueCapacity)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                throw new ThreadInterruptedException(e);
            }
        }
        
        if (!stopRequested)
        {
            queue.offer(new Event(task, null, size));
            dispatcher.wakeup();
        }
    }

    private synchronized void offerFromExternalThread(List<?> tasks)
    {
        int size = estimateSize(tasks, true);
        Assert.isTrue(size < minLockQueueCapacity);
        
        while (!stopRequested && queue.getCapacity() + size >= minLockQueueCapacity)
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                throw new ThreadInterruptedException(e);
            }
        }
        
        if (!stopRequested)
        {
            queue.offer(new Event(new CompartmentTaskList(tasks, true), null, size));
            dispatcher.wakeup();
        }
    }

    private synchronized Event poll(boolean firstInBatch)
    {
        if (lockCount > 0)
            return null;
        
        Event event = queue.poll(firstInBatch);
     
        if (queue.getCapacity() <= maxUnlockQueueCapacity)
        {
            if (flowLocked)
            {
                flowLocked = false;
                flowController.unlockFlow(null);
            }
            
            notifyAll();
        }

        return event;
    }
    
    private int estimateSize(Runnable task)
    {
        if (task instanceof CompartmentResult)
        {
            CompartmentResult result = (CompartmentResult)task;
            if (result.hasError())
                return sizeEstimator.estimateSize(result.getError());
            else
                return sizeEstimator.estimateSize(result.getResult());
        }
        else
            return sizeEstimator.estimateSize(task);
    }

    private int estimateSize(List<?> tasks, boolean runnable)
    {
        int size = 0;
        for (Object task : tasks)
        {
            if (runnable)
                size += estimateSize((Runnable)task);
            else
                size += sizeEstimator.estimateSize(task);
        }
        
        return size;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Compartment has been started.")
        ILocalizedMessage started();
        @DefaultMessage("Compartment has been stopped.")
        ILocalizedMessage stopped();
    }
}
