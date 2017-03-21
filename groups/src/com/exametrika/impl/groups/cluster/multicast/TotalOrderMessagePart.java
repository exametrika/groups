/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.List;
import java.util.UUID;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link TotalOrderMessagePart} is a total order message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TotalOrderMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<OrderInfo> orders;

    public static final class OrderInfo
    {
        public final UUID senderId;
        public final long startMessageId;
        public final long startOrder;
        public final int count;
        
        public OrderInfo(UUID senderId, long startMessageId, long startOrder, int count)
        {
            Assert.notNull(senderId);
            
            this.senderId = senderId;
            this.startMessageId = startMessageId;
            this.startOrder = startOrder;
            this.count = count;
        }
        
        @Override 
        public String toString()
        {
            return messages.order(senderId, startMessageId, startOrder, count).toString();
        }
    }
    
    public TotalOrderMessagePart(List<OrderInfo> orders)
    {
        Assert.notNull(orders);
        
        this.orders = Immutables.wrap(orders);
    }
    
    public List<OrderInfo> getOrders()
    {
        return orders;
    }
    
    @Override
    public int getSize()
    {
        return orders.size() * 36;
    }
    
    @Override 
    public String toString()
    {
        return orders.toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("sender-id: {0}, start message-id: {1}, start order: {2}, count: {3}")
        ILocalizedMessage order(UUID senderId, long startMessageId, long startOrder, int count);
    }
}

