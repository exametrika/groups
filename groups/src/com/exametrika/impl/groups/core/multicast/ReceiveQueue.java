/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleDeque;
import com.exametrika.common.utils.SimpleList.Element;
import com.exametrika.impl.groups.MessageFlags;


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
    private final int maxUnlockQueueCapacity;
    private final int minLockQueueCapacity;
    private final IFlowController<IAddress> flowController;
    private long startMessageId;
    private long lastAcknowlegedMessageId;
    private long firstUnacknowledgedReceiveTime;
    private long lastReceiveTime;
    private long lastCompletedMessageId;
    private long lastOrderedMessageId;
    private int queueCapacity;
    private boolean flowLocked;
    
    public ReceiveQueue(IAddress sender, IReceiver receiver, OrderedQueue orderedQueue, long startMessageId, 
        boolean durable, boolean ordered, int maxUnlockQueueCapacity, int minLockQueueCapacity, 
        IFlowController<IAddress> flowController)
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
        this.lastAcknowlegedMessageId = startMessageId - 1;
        this.maxUnlockQueueCapacity = maxUnlockQueueCapacity;
        this.minLockQueueCapacity = minLockQueueCapacity;
        this.flowController = flowController;
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
    
    public long getLastOrderedMessageId()
    {
        return lastOrderedMessageId;
    }
    
    public void setLastOrderedMessageId()
    {
        lastOrderedMessageId = getLastReceivedMessageId();
    }
    
    public boolean isAcknowledgementRequired()
    {
        return lastAcknowlegedMessageId != getLastReceivedMessageId();
    }
    
    public long getLastReceivedMessageId()
    {
        return startMessageId + deque.size() - 1;
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
        Assert.isTrue(messageId >= startMessageId && messageId < startMessageId + deque.size());
        
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
            queueCapacity += message.getSize();
            if (!flowLocked && queueCapacity >= minLockQueueCapacity)
            {
                flowLocked = true;
                flowController.lockFlow(message.getSource());
            }
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
            Assert.isTrue(ordered && order != 0);
            
            deliver(info);
            Assert.isTrue(deque.poll() == info);
            
            startMessageId++;
        }

        return true;
    }

    public void acknowledge()
    {
        lastAcknowlegedMessageId = getLastReceivedMessageId();
    }
    
    public void complete(long messageId) 
    {
        if (messageId == 0)
            return;
        
        boolean allowPoll = lastCompletedMessageId == startMessageId - 1;
        
        for (long i = lastCompletedMessageId + 1; i <= messageId; i++)
        {
            MessageInfo info = deque.get((int)(i - startMessageId));
            
            info.completed = true;
            
            if (allowPoll && (!ordered || info.order > 0))
            {
                deque.poll();
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
        
        queueCapacity = 0;
        if (flowLocked)
        {
            flowLocked = false;
            flowController.unlockFlow(null);
        }
    }
    
    private void deliver(MessageInfo info)
    {
        Assert.checkState(!info.delivered);
        
        info.delivered = true;
        if (!ordered || info.message.hasFlags(MessageFlags.UNORDERED))
            receiver.receive(info.message.removePart());
        else
            orderedQueue.offer(info.order, info.message);
        
        queueCapacity -= info.message.getSize();
        if (flowLocked && queueCapacity <= maxUnlockQueueCapacity)
        {
            flowLocked = false;
            flowController.unlockFlow(info.message.getSource());
        }
    }
    
    private static class MessageInfo
    {
        private IMessage message;
        private long order;
        private boolean completed;
        private boolean delivered;
    }
}

