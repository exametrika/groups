/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.UUID;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleDeque;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;


/**
 * The {@link OrderedQueue} is a totally ordered receive queue of failure atomic protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class OrderedQueue
{
    private final IReceiver receiver;
    private final SimpleDeque<IMessage> deque = new SimpleDeque<IMessage>();
    private long startOrder;
    private final QueueCapacityController capacityController;
    
    public OrderedQueue(IReceiver receiver, int maxUnlockQueueCapacity, int minLockQueueCapacity,
        GroupAddress groupAddress, UUID groupId)
    {
        Assert.notNull(receiver);
        
        this.receiver = receiver;
        this.capacityController = new QueueCapacityController(minLockQueueCapacity, maxUnlockQueueCapacity, 
            groupAddress, groupId);
    }
    
    public void setFlowController(IFlowController<RemoteFlowId> flowController)
    {
        capacityController.setFlowController(flowController);
    }
    
    public void offer(long order, IMessage message)
    {
        Assert.isTrue(order >= startOrder);
        
        capacityController.addCapacity(message.getSource(), message.getSize());
        
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
        
        capacityController.clearCapacity();
    }
    
    private void deliverMessages()
    {
        while (true) 
        {
            IMessage message = deque.peek();
            if (message == null)
                return;
            
            message = deque.poll();
            receiver.receive(message);
            
            capacityController.removeCapacity(message.getSize());
        }
    }
}

