/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.groups.core.multicast.TotalOrderMessagePart.OrderInfo;

/**
 * The {@link TotalOrderMessagePartSerializer} is a serializer for {@link TotalOrderMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TotalOrderMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("5b2c50c8-57c5-4145-aae5-b6cb1d1cbede");
 
    public TotalOrderMessagePartSerializer()
    {
        super(ID, TotalOrderMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        TotalOrderMessagePart part = (TotalOrderMessagePart)object;

        Serializers.writeVarInt(serialization, part.getOrders().size());
        
        for (OrderInfo info : part.getOrders())
        {
            Serializers.writeUUID(serialization, info.senderId);
            Serializers.writeVarLong(serialization, info.startMessageId);
            Serializers.writeVarLong(serialization, info.startOrder);
            Serializers.writeVarInt(serialization, info.count);
        }
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int count = Serializers.readVarInt(deserialization);
        List<OrderInfo> orders = new ArrayList(count);
        
        for (int i = 0; i < count; i++)
        {
            UUID senderId = Serializers.readUUID(deserialization);
            long startMessageId = Serializers.readVarLong(deserialization);
            long startOrder = Serializers.readVarLong(deserialization);
            int messageCount = Serializers.readVarInt(deserialization);
            
            orders.add(new OrderInfo(senderId, startMessageId, startOrder, messageCount));
        }
        
        return new TotalOrderMessagePart(orders);
    }
}
