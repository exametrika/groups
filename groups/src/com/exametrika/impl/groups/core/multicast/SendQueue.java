/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.groups.core.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleDeque;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;
import com.exametrika.impl.groups.core.flush.IFlush;


/**
 * The {@link SendQueue} is a send queue of durable failure atomic protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class SendQueue
{
    private final IFailureDetector failureDetector;
    private final ITimeService timeService;
    private final IDeliveryHandler deliveryHandler;
    private final boolean durable;
    private final int maxUnlockQueueCapacity;
    private final int minLockQueueCapacity;
    private IFlowController<IAddress> flowController;
    private final SimpleDeque<IMessage> deque = new SimpleDeque<IMessage>();
    private final Map<IAddress, Long> acknowledgedMessageIds = new HashMap<IAddress, Long>();
    private boolean completionRequired;
    private long startMessageId = 1;
    private long lastCompletedMessageId;
    private long lastCompletionSendTime;
    private long nextSendMessageId = 1;
    private long bundleCreationTime;
    private long lastSendMessageId;
    private long lastOldMembershipMessageId;
    private int queueCapacity;
    private boolean flowLocked;
    
    public SendQueue(IFailureDetector failureDetector, ITimeService timeService, IDeliveryHandler deliveryHandler, 
        boolean durable, int maxUnlockQueueCapacity, int minLockQueueCapacity)
    {
        Assert.notNull(failureDetector);
        Assert.notNull(timeService);
        Assert.isTrue(durable == (deliveryHandler != null));
        
        this.failureDetector = failureDetector;
        this.timeService = timeService;
        this.deliveryHandler = deliveryHandler;
        this.durable = durable;
        this.maxUnlockQueueCapacity = maxUnlockQueueCapacity;
        this.minLockQueueCapacity = minLockQueueCapacity;
    }
    
    public void setFlowController(IFlowController<IAddress> flowController)
    {
        Assert.notNull(flowController);
        Assert.isNull(this.flowController);
        
        this.flowController = flowController;
    }
    
    public boolean isCompletionRequired()
    {
        return completionRequired;
    }
    
    public long getLastCompletionSendTime()
    {
        return lastCompletionSendTime;
    }
    
    public long getBundleCreationTime()
    {
        return bundleCreationTime;
    }
    
    public boolean isLastOldMembershipMessageCompleted()
    {
        return lastCompletedMessageId >= lastOldMembershipMessageId;
    }
    
    public void setLastOldMembershipMessageId()
    {
        lastOldMembershipMessageId = startMessageId + deque.size() - 1;
    }
    
    public int getQueueCapacity()
    {
        return queueCapacity;
    }
    
    public void onMemberFailed(INode member)
    {
        if (acknowledgedMessageIds.remove(member.getAddress()) != null)
            setCompletionRequired();
    }

    public void beforeProcessFlush(IFlush flush)
    {
        Set<INode> nodes = new HashSet<INode>(flush.getNewMembership().getGroup().getMembers());
        nodes.removeAll(failureDetector.getFailedMembers());
        nodes.removeAll(failureDetector.getLeftMembers());
        
        acknowledgedMessageIds.clear();
        for (INode node : nodes)
            acknowledgedMessageIds.put(node.getAddress(), lastCompletedMessageId);
    }
    
    public long acquireMessageId()
    {
        return nextSendMessageId++;
    }
    
    public void offer(IMessage message)
    {
        if (lastSendMessageId == startMessageId + deque.size() - 1)
            bundleCreationTime = timeService.getCurrentTime();
        
        deque.offer(message);
        
        queueCapacity += message.getSize();
        if (!flowLocked && queueCapacity >= minLockQueueCapacity)
        {
            flowLocked = true;
            flowController.lockFlow(message.getSource());
        }
    }
    
    public List<IMessage> createBundle()
    {
        int pos = (int)(lastSendMessageId - startMessageId + 1);
        if (pos >= deque.size())
            return null;
        
        List<IMessage> bundle = new ArrayList<IMessage>();
        for (int i = pos; i < deque.size(); i++)
            bundle.add(deque.get(i));
        
        lastSendMessageId = startMessageId + deque.size() - 1;
        
        if (!durable)
        {
            startMessageId += deque.size();
            deque.clear();
            
            queueCapacity = 0;
            if (flowLocked)
            {
                flowLocked = false;
                flowController.unlockFlow(null);
            }
        }
        
        return bundle;
    }
    
    public void acknowledge(IAddress address, long lastReceivedMessageId)
    {
        long prevReceivedMessageId = acknowledgedMessageIds.put(address, lastReceivedMessageId);
        Assert.isTrue(prevReceivedMessageId < lastReceivedMessageId);
        
        setCompletionRequired();
    }
    
    public long complete()
    {
        if (!completionRequired)
            return 0;
        
        long minCompletedMessageId = Long.MAX_VALUE;
        for (long value : acknowledgedMessageIds.values())
        {
            if (minCompletedMessageId > value)
                minCompletedMessageId = value;
        }
        
        completionRequired = false;
        
        if (minCompletedMessageId <= lastCompletedMessageId || minCompletedMessageId == Long.MAX_VALUE)
            return 0;
        
        if (durable)
        {
            for (long i = lastCompletedMessageId + 1; i <= minCompletedMessageId; i++)
            {
                IMessage message = deque.poll();
                Assert.checkState(message != null);
                
                deliveryHandler.onDelivered(message);
                
                queueCapacity -= message.getSize();
                if (flowLocked && queueCapacity <= maxUnlockQueueCapacity)
                {
                    flowLocked = false;
                    flowController.unlockFlow(message.getSource());
                }
            }
            
            startMessageId = minCompletedMessageId + 1;
        }
        
        lastCompletedMessageId = minCompletedMessageId;
        lastCompletionSendTime = timeService.getCurrentTime();
        
        return minCompletedMessageId;
    }
    
    private void setCompletionRequired()
    {
        if (!completionRequired)
        {
            lastCompletionSendTime = timeService.getCurrentTime();
        
            completionRequired = true;
        }
    }
}

