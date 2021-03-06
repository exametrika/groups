/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.UUID;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleDeque;
import com.exametrika.common.utils.SimpleList.Element;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;


/**
 * The {@link ReceiveQueue} is a receive queue of failure atomic protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ReceiveQueue
{
    private final IAddress sender;
    private final IReceiver receiver;
    private final OrderedQueue orderedQueue;
    private final boolean durable;
    private final boolean ordered;
    private final SimpleDeque<MessageInfo> deque = new SimpleDeque<MessageInfo>();
    private final Element<ReceiveQueue> element = new Element<ReceiveQueue>(this);
    private long startMessageId;
    private long lastReceivedMessageId;
    private long lastAcknowlegedMessageId;
    private long firstUnacknowledgedReceiveTime;
    private long lastReceiveTime;
    private long lastCompletedMessageId;
    private long lastOrderedMessageId;
    private final QueueCapacityController capacityController;
    
    public ReceiveQueue(IAddress sender, IReceiver receiver, OrderedQueue orderedQueue, long startMessageId, 
        boolean durable, boolean ordered, int maxUnlockQueueCapacity, int minLockQueueCapacity, 
        IFlowController<RemoteFlowId> flowController, long currentTime, GroupAddress groupAddress, UUID groupId)
    {
        Assert.notNull(sender);
        Assert.notNull(receiver);
        Assert.isTrue(ordered == (orderedQueue != null));
        
        this.sender = sender;
        this.receiver = receiver;
        this.orderedQueue = orderedQueue;
        this.durable = durable;
        this.ordered = ordered;
        this.startMessageId = startMessageId;
        this.lastReceivedMessageId = startMessageId - 1;
        this.lastAcknowlegedMessageId = startMessageId - 1;
        this.lastCompletedMessageId = startMessageId - 1;
        this.lastOrderedMessageId = startMessageId - 1;
        this.lastReceiveTime = currentTime;
        this.capacityController = new QueueCapacityController(minLockQueueCapacity, maxUnlockQueueCapacity, 
            groupAddress, groupId);
        this.capacityController.setFlowController(flowController);
    }
    
    public IAddress getSender()
    {
        return sender;
    }
    
    public boolean isEmpty()
    {
        return deque.isEmpty();
    }

    public Element<ReceiveQueue> getElement()
    {
        return element;
    }
    
    public long getStartMessageId()
    {
        return startMessageId;
    }
    
    public long getLastOrderedMessageId()
    {
        return lastOrderedMessageId;
    }
    
    public void setLastOrderedMessageId()
    {
        lastOrderedMessageId = lastReceivedMessageId;
    }
    
    public boolean isAcknowledgementRequired()
    {
        return lastAcknowlegedMessageId != lastReceivedMessageId;
    }
    
    public long getLastReceivedMessageId()
    {
        return lastReceivedMessageId;
    }
    
    public long getLastAcknowledgedMessageId()
    {
        return lastAcknowlegedMessageId;
    }
    
    public long getFirstUnacknowledgedReceiveTime()
    {
        return firstUnacknowledgedReceiveTime;
    }
    
    public long getLastReceiveTime()
    {
        return lastReceiveTime;
    }
    
    public IMessage getMessage(long messageId)
    {
        Assert.isTrue(messageId >= startMessageId && messageId <= lastReceivedMessageId);
        
        int pos = (int)(messageId - startMessageId);
        IMessage message = deque.get(pos).message;
        
        Assert.notNull(message);
        return message;
    }
    
    public boolean receive(long messageId, long order, IMessage message, long currentTime)
    {
        if (messageId < startMessageId)
            return false;
        
        Assert.isTrue((!ordered && message != null && order == 0) || (ordered && (order != 0 || message != null)));
        
        MessageInfo info;
        int pos = (int)(messageId - startMessageId);
        int size = deque.size();
        if (pos == size)
        {
            if (messageId == lastAcknowlegedMessageId + 1)
                firstUnacknowledgedReceiveTime = currentTime;
            
            info = new MessageInfo();
            info.message = message;
            info.order = order;
            deque.offer(info);
        }
        else if (pos < size)
        {
            info = deque.get(pos);
            if (message != null)
            {
                if (info.message != null)
                    return false;
                else
                    info.message = message;
            }
            
            if (order != 0)
            {
                Assert.isTrue(info.order == 0);
                info.order = order;
            }
        }
        else
            return Assert.error();
        
        lastReceiveTime = currentTime;
        
        if (message != null)
        {
            Assert.isTrue(messageId == lastReceivedMessageId + 1);
            lastReceivedMessageId++;
            capacityController.addCapacity(message.getSource(), message.getSize());
        }
        
        if (!durable)
        {
            if (!ordered || (info.message != null && info.order != 0))
                deliver(info);
            
            if (info.completed)
            {
                Assert.isTrue(deque.poll() == info);
                Assert.isTrue(ordered && order != 0);
                startMessageId++;
            }
        }
        else if (info.completed)
        {
            deliver(info);
            
            Assert.isTrue(deque.poll() == info);
            Assert.isTrue(ordered && (order != 0 || info.message.hasFlags(MessageFlags.UNORDERED)));
            startMessageId++;
        }

        return true;
    }

    public void acknowledge()
    {
        lastAcknowlegedMessageId = lastReceivedMessageId;
    }
    
    public void complete(long messageId) 
    {
        if (messageId == 0 || messageId < startMessageId)
            return;
        
        Assert.isTrue(messageId >= startMessageId && messageId < startMessageId + deque.size());
        
        boolean allowPoll = lastCompletedMessageId == startMessageId - 1;
        
        for (long i = lastCompletedMessageId + 1; i <= messageId; i++)
        {
            MessageInfo info = deque.get((int)(i - startMessageId));
            
            info.completed = true;
            
            if (allowPoll && (!ordered || info.order > 0 || info.message.hasFlags(MessageFlags.UNORDERED)))
            {
                Assert.isTrue(deque.poll() == info);
                startMessageId++;
                
                if (durable)
                    deliver(info);
            }
        }
        
        lastCompletedMessageId = messageId;
    }
    
    public void completeAll()
    {
        startMessageId += deque.size();
        Assert.isTrue(startMessageId == lastReceivedMessageId + 1);
        
        lastAcknowlegedMessageId = startMessageId - 1;
        lastCompletedMessageId = startMessageId - 1;
        lastOrderedMessageId = startMessageId - 1;
        
        deque.clear();
    }
    
    public void flushMessages()
    {
        for (int i = 0; i < deque.size(); i++)
        {
            MessageInfo info = deque.get(i);
            
            if (info.message != null && !info.delivered)
                receiver.receive(info.message);
        }
        
        capacityController.clearCapacity();
    }
    
    private void deliver(MessageInfo info)
    {
        Assert.checkState(!info.delivered);
        
        info.delivered = true;
        if (!ordered || info.message.hasFlags(MessageFlags.UNORDERED))
            receiver.receive(info.message);
        else
            orderedQueue.offer(info.order, info.message);
        
        capacityController.removeCapacity(info.message.getSize());
    }
    
    private static class MessageInfo
    {
        private IMessage message;
        private long order;
        private boolean completed;
        private boolean delivered;
    }
}

