/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.Assert;

/**
 * The {@link QueueCapacityController} controls maximum queue capacity by locking/unlocking incoming flow.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class QueueCapacityController
{
    private final int minLockQueueCapacity;
    private final int maxUnlockQueueCapacity;
    private IFlowController<RemoteFlowId> flowController;
    private final IAddress receiver;
    private final UUID flowId;
    private int capacity;
    private Set<IAddress> lockedSenders = new LinkedHashSet<IAddress>();
    private boolean locked;

    public QueueCapacityController(int minLockQueueCapacity, int maxUnlockQueueCapacity, IAddress receiver, UUID flowId)
    {
        Assert.notNull(receiver);
        Assert.notNull(flowId);
        
        this.minLockQueueCapacity = minLockQueueCapacity;
        this.maxUnlockQueueCapacity = maxUnlockQueueCapacity;
        this.receiver = receiver;
        this.flowId = flowId;
    }

    public void setFlowController(IFlowController<RemoteFlowId> flowController)
    {
        Assert.notNull(flowController);
        Assert.isNull(this.flowController);
        
        this.flowController = flowController;
    }
    
    public int getCapacity()
    {
        return capacity;
    }
    
    public boolean isLocked()
    {
        return locked;
    }
    
    public void addCapacity(IAddress sender, int value)
    {
        capacity += value;
        if (capacity >= minLockQueueCapacity)
            lockFlow(sender);
    }
    
    public void removeCapacity(int value)
    {
        capacity -= value;
        if (capacity <= maxUnlockQueueCapacity)
            unlockFlow();
    }
    
    public void clearCapacity()
    {
        capacity = 0;
        unlockFlow();
    }
    
    private void lockFlow(IAddress sender)
    {
        locked = true;
        if (lockedSenders.add(sender))
            flowController.lockFlow(new RemoteFlowId(sender, receiver, flowId));
    }
    
    private void unlockFlow()
    {
        locked = false;
        if (!lockedSenders.isEmpty())
        {
            for (IAddress sender : lockedSenders)
            {
                RemoteFlowId flowId = new RemoteFlowId(sender, receiver, this.flowId);
                flowController.unlockFlow(flowId);
            }
            lockedSenders.clear();
        }
    }
}
