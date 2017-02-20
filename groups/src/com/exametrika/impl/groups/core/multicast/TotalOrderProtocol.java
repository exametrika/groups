/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipService;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.core.multicast.TotalOrderMessagePart.OrderInfo;




/**
 * The {@link TotalOrderProtocol} is a total order protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class TotalOrderProtocol
{
    private final FailureAtomicMulticastProtocol parent;
    private final IMembershipService membershipService;
    private final IMessageFactory messageFactory;
    private final ISender sender;
    private final ITimeService timeService;
    private final long maxBundlingPeriod;
    private final int maxBundlingMessageCount;
    private final SimpleList<ReceiveQueue> orderingQueues = new SimpleList<ReceiveQueue>();
    private boolean coordinator;
    private long nextOrder = 1;
    private long lastOrderSendTime;
    private boolean flushStarted;
    
    public TotalOrderProtocol(FailureAtomicMulticastProtocol parent, IMembershipService membershipService, IMessageFactory messageFactory,
        ISender sender, ITimeService timeService, long maxBundlingPeriod, int maxBundlingMessageCount)
    {
        Assert.notNull(parent);
        Assert.notNull(membershipService);
        Assert.notNull(messageFactory);
        Assert.notNull(sender);
        Assert.notNull(timeService);
        
        this.parent = parent;
        this.membershipService = membershipService;
        this.messageFactory = messageFactory;
        this.sender = sender;
        this.timeService = timeService;
        this.maxBundlingPeriod = maxBundlingPeriod;
        this.maxBundlingMessageCount = maxBundlingMessageCount;
    }
    
    public boolean isCoordinator()
    {
        return coordinator;
    }
    
    public long acquireOrder(int count)
    {
        Assert.isTrue(count > 0);
       
        long res = nextOrder;
        nextOrder += count;
        
        return res;
    }
    
    public void setCoordinator()
    {
        coordinator = true;
    }
    
    public void startFlush()
    {
        flushStarted = true;
        nextOrder = 1;
        orderingQueues.clear();
    }
    
    public void endFlush()
    {
        flushStarted = false;
    }
    
    public void onTimer()
    {
        if (!flushStarted && !orderingQueues.isEmpty() && (timeService.getCurrentTime() > lastOrderSendTime + maxBundlingPeriod))
            sendOrder();
    }
    
    public void onReceiveMulticast(ReceiveQueue receiveQueue, boolean noDelay)
    {
        Assert.checkState(!flushStarted);
        
        if (orderingQueues.isEmpty())
            lastOrderSendTime = timeService.getCurrentTime();
        
        receiveQueue.getElement().reset();
        orderingQueues.addLast(receiveQueue.getElement());

        if (noDelay || (timeService.getCurrentTime() > lastOrderSendTime + maxBundlingPeriod) ||
            receiveQueue.getLastReceivedMessageId() - receiveQueue.getLastOrderedMessageId() > maxBundlingMessageCount)
            sendOrder();
    }
    
    public boolean receive(IMessage message)
    {
        if (message.getPart() instanceof TotalOrderMessagePart)
        {
            TotalOrderMessagePart part = message.getPart();
            
            IMembership membership = membershipService.getMembership();
            long currentTime = timeService.getCurrentTime();
            
            for (OrderInfo info : part.getOrders())
            {
                INode sender = membership.getGroup().findMember(info.senderId);
                
                ReceiveQueue receiveQueue = parent.ensureReceiveQueue(sender.getAddress(), info.startMessageId);
            
                for (int i = 0; i < info.count; i++)
                    receiveQueue.receive(info.startMessageId + i, info.startOrder + i, null, currentTime);
                
            }
            return true;
            
        }
        else
            return false;
    }

    private void sendOrder()
    {
        List<OrderInfo> orders = new ArrayList<OrderInfo>();
        for (ReceiveQueue receiveQueue : orderingQueues.values())
        {
            long startMessageId = receiveQueue.getLastOrderedMessageId() + 1;
            int count = (int)(receiveQueue.getLastReceivedMessageId() - receiveQueue.getLastOrderedMessageId());
            long startOrder = acquireOrder(count);
            
            orders.add(new OrderInfo(receiveQueue.getSender().getId(), startMessageId, startOrder, count));
            receiveQueue.setLastOrderedMessageId();
        }
        
        orderingQueues.clear();
        
        IMembership membership = membershipService.getMembership();
        Assert.checkState(membership != null);
        
        sender.send(messageFactory.create(membership.getGroup().getAddress(), new TotalOrderMessagePart(orders), 
            MessageFlags.UNORDERED | MessageFlags.NO_DELAY));
    }
}

