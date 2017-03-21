/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.flush;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.groups.cluster.exchange.IExchangeData;

/**
 * The {@link FlushExchangeGetMessagePartSerializer} is a serializer for {@link FlushExchangeGetMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FlushExchangeGetMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("82c22a06-affb-404e-a932-e791de5db52a");
 
    public FlushExchangeGetMessagePartSerializer()
    {
        super(ID, FlushExchangeGetMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        FlushExchangeGetMessagePart part = (FlushExchangeGetMessagePart)object;
        
        serialization.writeInt(part.getFailedMembers().size());
        
        for (UUID nodeId : part.getFailedMembers())
            Serializers.writeUUID(serialization, nodeId);
        
        serialization.writeInt(part.getLeftMembers().size());
        
        for (UUID nodeId : part.getLeftMembers())
            Serializers.writeUUID(serialization, nodeId);
        
        serialization.writeInt(part.getDataExchanges().size());
        for (IExchangeData exchange : part.getDataExchanges())
            serialization.writeObject(exchange);
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int count = deserialization.readInt();
        Set<UUID> failedMembers = new HashSet<UUID>(count);
        for (int i = 0; i < count; i++)
            failedMembers.add(Serializers.readUUID(deserialization));
        
        count = deserialization.readInt();
        Set<UUID> leftMembers = new HashSet<UUID>(count);
        for (int i = 0; i < count; i++)
            leftMembers.add(Serializers.readUUID(deserialization));
        
        count = deserialization.readInt();
        List<IExchangeData> dataExchanges = new ArrayList<IExchangeData>(count);
        for (int i = 0; i < count; i++)
        {
            IExchangeData exchange = deserialization.readObject();
            dataExchanges.add(exchange);
        }
        return new FlushExchangeGetMessagePart(failedMembers, leftMembers, dataExchanges);
    }
}
