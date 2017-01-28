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


/**
 * The {@link OrderedQueue} is a totally ordered receive queue of failure atomic protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class OrderedQueue
{
    private final IReceiver receiver;
    private final int maxUnlockQueueCapacity;
    private final int minLockQueueCapacity;
    private IFlowController<IAddress> flowController;
    private final SimpleDeque<IMessage> deque = new SimpleDeque<IMessage>();
    private long startOrder;
    private int queueCapacity;
    private boolean flowLocked;
    
    public OrderedQueue(IReceiver receiver, int maxUnlockQueueCapacity, int minLockQueueCapacity)
    {
        Assert.notNull(receiver);
        
        this.receiver = receiver;
        this.maxUnlockQueueCapacity = maxUnlockQueueCapacity;
        this.minLockQueueCapacity = minLockQueueCapacity;
    }
    
    public void setFlowController(IFlowController<IAddress> flowController)
    {
        Assert.notNull(flowController);
        Assert.isNull(this.flowController);
        
        this.flowController = flowController;
    }
    
    public void offer(long order, IMessage message)
    {
        Assert.isTrue(order >= startOrder);
        
        queueCapacity += message.getSize();
        if (!flowLocked && queueCapacity >= minLockQueueCapacity)
        {
            flowLocked = true;
            flowController.lockFlow(message.getSource());
        }
        
        int pos = (int)(order - startOrder);
        int size = deque.size();
        if (pos >= size)
        {
            for (int i = size; i < pos; i++)
                deque.offer(null);
            
            deque.offer(message);
        }
        else
            deque.set(pos, message);
        
        deliverMessages();
    }

    public void flushMessages()
    {
        while (!deque.isEmpty()) 
        {
            IMessage message = deque.poll();
            if (message != null)
                receiver.receive(message);
        }
        
        queueCapacity = 0;
        if (flowLocked)
        {
            flowLocked = false;
            flowController.unlockFlow(null);
        }
    }
    
    private void deliverMessages()
    {
        while (true) 
        {
            IMessage message = deque.peek();
            if (message == null)
                return;
            
            message = deque.poll();
            receiver.receive(message.removePart());
            
            queueCapacity -= message.getSize();
            if (flowLocked && queueCapacity <= maxUnlockQueueCapacity)
            {
                flowLocked = false;
                flowController.unlockFlow(message.getSource());
            }
        }
    }
}

