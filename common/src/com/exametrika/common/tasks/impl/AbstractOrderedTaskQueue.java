/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tasks.ITaskFilter;
import com.exametrika.common.tasks.ITaskListener;
import com.exametrika.common.tasks.ITaskQueue;
import com.exametrika.common.tasks.ITaskSource;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;


/**
 * The {@link AbstractOrderedTaskQueue} is a task queue that retains order of tasks from particuar task flow.
 * Order of tasks from different task flows is not retained.
 * 
 * @param <F> task flow type
 * @param <T> task type
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public abstract class AbstractOrderedTaskQueue<F, T> implements ITaskQueue<T>, ITaskSource<T>, ITaskListener<T>, 
    IFlowController<F>
{
    private final IFlowController<F> flowController;
    private final ITaskFilter<T> taskFilter;
    private final int maxUnlockQueueCapacity;
    private final int minLockQueueCapacity;
    private TaskQueue<T> taskQueue = new TaskQueue<T>();
    private Map<F, QueueInfo<T>> queues = new LinkedHashMap<F, QueueInfo<T>>();
    
    /**
     * Creates a new object.
     * 
     * @param flowController flow controller
     * @param taskFilter task filter. Can be null, if task filter is not set
     * @param maxUnlockQueueCapacity maximal capacity of task queue for particular task flow 
     * which unlocks flow of tasks after locking
     * @param minLockQueueCapacity minimal capacity of task queue for particular task flow 
     * which locks flow of tasks in order to prevent queue overflow
     */
    public AbstractOrderedTaskQueue(IFlowController<F> flowController, ITaskFilter<T> taskFilter, 
        int maxUnlockQueueCapacity, int minLockQueueCapacity)
    {
        Assert.notNull(flowController);

        this.flowController = flowController;
        this.taskFilter = taskFilter;
        this.maxUnlockQueueCapacity = maxUnlockQueueCapacity;
        this.minLockQueueCapacity = minLockQueueCapacity;
    }
    
    /**
     * Removes specified task queue.
     *
     * @param flow queue flow
     */
    public void removeQueue(F flow)
    {
        Assert.notNull(flow);
        
        boolean unlockFlow = false;
        synchronized (this)
        {
            QueueInfo<T> queue = queues.remove(flow);
            if (queue != null && queue.flowLocked)
            {
                queue.flowLocked = false;
                unlockFlow = true;
            }
        }
        
        if (unlockFlow)
            flowController.unlockFlow(flow);
    }
    
    /**
     * Removes task queues satisfying the specified condition.
     *
     * @param condition condition
     */
    public final void removeQueues(ICondition<F> condition)
    {
        Assert.notNull(condition);
        
        List<F> unlockFlows = new ArrayList<F>();
        
        synchronized (this)
        {
            Set<Map.Entry<F, QueueInfo<T>>> entrySet = queues.entrySet();
            for (Iterator<Map.Entry<F, QueueInfo<T>>> it = entrySet.iterator(); it.hasNext(); )
            {
                Map.Entry<F, QueueInfo<T>> entry = it.next();
                if (condition.evaluate(entry.getKey()))
                {
                    it.remove();
                    if (entry.getValue().flowLocked)
                    {
                        entry.getValue().flowLocked = false;
                        unlockFlows.add(entry.getKey());
                    }
                }
            }
        }
        
        for (F flow : unlockFlows)
            flowController.unlockFlow(flow);
    }
    
    /**
     * Removes all task queues.
     */
    public final void removeAllQueues()
    {
        List<F> unlockFlows = new ArrayList<F>();
        
        synchronized (this)
        {
            for (Map.Entry<F, QueueInfo<T>> entry : queues.entrySet())
            {
                if (entry.getValue().flowLocked)
                {
                    entry.getValue().flowLocked = false;
                    unlockFlows.add(entry.getKey());
                }
            }
            
            queues.clear();
        }
        
        for (F flow : unlockFlows)
            flowController.unlockFlow(flow);
    }

    @Override
    public final boolean offer(T task)
    {
        Assert.notNull(task);
        
        F flow = getTaskFlow(task);
        boolean lockFlow = false;
        
        synchronized (this)
        {
            if (taskFilter != null && !taskFilter.accept(task))
                return false;

            QueueInfo<T> queue = queues.get(flow);
            if (queue == null)
            {
                queue = new QueueInfo();
                queues.put(flow, queue);
            }
            
            if (queue.lockCount > 0)
            {
                // If task flow of queue is locked add task to pending tasks 
                queue.pendingTasks.addLast(task);
                
                if (queue.pendingTasks.size() >= minLockQueueCapacity && !queue.flowLocked)
                {
                    // If pending task queue exceeds threshold, lock flow of parent queue
                    queue.flowLocked = true;
                    lockFlow = true;
                }
            }
            else
            {
                // Add task to handling queue and lock queue for task flow (until task has been handled)
                queue.lockCount = 1;
                taskQueue.offer(task);
            }
        }
        
        if (lockFlow)
            flowController.lockFlow(flow);
        
        return true;
    }

    @Override
    public final void put(T task)
    {
        offer(task);
    }
    
    @Override
    public final T take()
    {
        return taskQueue.take();
    }

    @Override
    public final void onTaskStarted(T task)
    {
    }

    @Override
    public final void onTaskCompleted(T task)
    {
        unlockFlow(getTaskFlow(task));
    }

    @Override
    public final void onTaskFailed(T task, Throwable error)
    {
        unlockFlow(getTaskFlow(task));
    }
    
    @Override
    public final synchronized void lockFlow(F flow)
    {
        Assert.notNull(flow);
        
        QueueInfo queue = queues.get(flow);
        if (queue != null)
            queue.lockCount++;
    }

    @Override
    public final void unlockFlow(F flow)
    {
        Assert.notNull(flow);
        
        boolean unlockFlow = false;
        
        synchronized (this)
        {
            QueueInfo<T> queue = queues.get(flow);
            if (queue != null && queue.lockCount > 0)
            {
                queue.lockCount--;
                
                if (queue.lockCount == 0 && !queue.pendingTasks.isEmpty())
                {
                    // If flow of current queue has been unlocked and there are pending tasks, handle first pending task
                    queue.lockCount = 1;
                    taskQueue.offer(queue.pendingTasks.removeFirst());
                }
                
                if (queue.pendingTasks.size() <= maxUnlockQueueCapacity && queue.flowLocked)
                {
                    // If parent queue has been locked and current queue is less than threshold, unlock parent queue
                    queue.flowLocked = false;
                    unlockFlow = true;
                }
            }
        }
        
        if (unlockFlow)
            flowController.unlockFlow(flow);
    }

    /**
     * Returns task flow for specified task.
     *
     * @param task task
     * @return task flow
     */
    protected abstract F getTaskFlow(T task);
    
    private static class QueueInfo<T>
    {
        public int lockCount;
        public boolean flowLocked;
        public LinkedList<T> pendingTasks = new LinkedList<T>();
    }
}
