/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.groups.core.exchange.IExchangeData;

/**
 * The {@link FlushDataExchangeMessagePartSerializer} is a serializer for {@link FlushDataExchangeMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FlushDataExchangeMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("eb0974a7-c7f6-4b23-bb40-123f9055d78e");
 
    public FlushDataExchangeMessagePartSerializer()
    {
        super(ID, FlushDataExchangeMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        FlushDataExchangeMessagePart part = (FlushDataExchangeMessagePart)object;

        serialization.writeInt(part.getDataExchanges().size());
        for (Map.Entry<UUID, IExchangeData> entry : part.getDataExchanges().entrySet())
        {
            Serializers.writeUUID(serialization, entry.getKey());
            serialization.writeObject(entry.getValue());
        }
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int count = deserialization.readInt();
        
        Map<UUID, IExchangeData> dataExchanges = new HashMap<UUID, IExchangeData>(count);
        for (int i = 0; i < count; i++)
        {
            UUID nodeId = Serializers.readUUID(deserialization);
            IExchangeData data = deserialization.readObject();
            
            dataExchanges.put(nodeId, data);
        }
        
        return new FlushDataExchangeMessagePart(dataExchanges);
    }
}
