/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;


/**
 * The {@link BlockingFlowController} is an implementation of {@link IFlowController} interface that uses blocking
 * on sender part to control flow of tasks.
 * 
 * @param <F> task flow type
 * @threadsafety This class and its methods are thread safe.
 * @author AndreyM
 */
public final class BlockingFlowController<F> implements IFlowController<F>
{
    private Map<F, FlowInfo> flows = new LinkedHashMap<F, FlowInfo>();
    
    /**
     * Waits until specified flow of tasks is unlocked.
     *
     * @param flow task flow to wait unlock for
     */
    public void await(F flow)
    {
        Assert.notNull(flow);
        
        FlowInfo info;
        synchronized (this)
        {
            info = flows.get(flow);
            if (info == null)
                return;
        }
            
        synchronized (info)
        {
            try
            {
                while (info.lockCount > 0)
                    info.wait();
            }
            catch (InterruptedException e)
            {
                throw new ThreadInterruptedException(e);
            }
        }
    }
    
    /**
     * Unlocks and removes specified task flow.
     *
     * @param flow flow to unlock and remove
     */
    public synchronized void removeFlow(F flow)
    {
        Assert.notNull(flow);
        
        forceUnlockFlow(flows.remove(flow));
    }
    
    /**
     * Unlocks and removes task flows satisfying the specified condition.
     *
     * @param condition condition
     */
    public synchronized void removeFlows(ICondition<F> condition)
    {
        Assert.notNull(condition);
        
        for (Iterator<Map.Entry<F, FlowInfo>> it = flows.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<F, FlowInfo> entry = it.next();
            if (condition.evaluate(entry.getKey()))
            {
                forceUnlockFlow(entry.getValue());
                it.remove();
            }
        }
    }
    
    /**
     * Unlocks and removes all task flows.
     */
    public synchronized void removeAllFlows()
    {
        for (Map.Entry<F, FlowInfo> entry : flows.entrySet())
            forceUnlockFlow(entry.getValue());
        
        flows.clear();
    }
    
    @Override
    public synchronized void lockFlow(F flow)
    {
        Assert.notNull(flow);
        
        FlowInfo info = flows.get(flow);
        if (info == null)
        {
            info = new FlowInfo();
            flows.put(flow, info);
        }
         
        synchronized (info)
        {
            info.lockCount++;
        }
    }

    @Override
    public synchronized void unlockFlow(F flow)
    {
        Assert.notNull(flow);
        
        FlowInfo info = flows.get(flow);
        if (info != null)
        {
            synchronized (info)
            {
                if (info.lockCount > 0)
                {
                    info.lockCount--;
                    
                    if (info.lockCount == 0)
                    {
                        info.notifyAll();
                        flows.remove(flow);
                    }
                }
            }
        }
    }
    
    private void forceUnlockFlow(FlowInfo info)
    {
        if (info == null)
            return;
        
        synchronized (info)
        {
            info.lockCount = 0;
            info.notifyAll();
        }
    }

    private static class FlowInfo
    {
        public int lockCount;
    }
}
