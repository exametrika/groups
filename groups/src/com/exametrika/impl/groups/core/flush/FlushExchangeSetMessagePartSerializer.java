/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.groups.core.exchange.IExchangeData;

/**
 * The {@link FlushExchangeSetMessagePartSerializer} is a serializer for {@link FlushExchangeSetMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FlushExchangeSetMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("eb0974a7-c7f6-4b23-bb40-123f9055d78e");
 
    public FlushExchangeSetMessagePartSerializer()
    {
        super(ID, FlushExchangeSetMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        FlushExchangeSetMessagePart part = (FlushExchangeSetMessagePart)object;

        serialization.writeInt(part.getDataExchanges().size());
        for (Map<UUID, IExchangeData> map : part.getDataExchanges())
        {
            serialization.writeInt(map.size());
            for (Map.Entry<UUID, IExchangeData> entry : map.entrySet())
            {
                Serializers.writeUUID(serialization, entry.getKey());
                serialization.writeObject(entry.getValue());
            }
        }
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int count = deserialization.readInt();
        List<Map<UUID, IExchangeData>> dataExchanges = new ArrayList<Map<UUID, IExchangeData>>(count);
        for (int i = 0; i < count; i++)
        {
            int mapCount = deserialization.readInt();
            Map<UUID, IExchangeData> map = new HashMap<UUID, IExchangeData>(mapCount);
            for (int k = 0; k < mapCount; k++)
            {
                UUID nodeId = Serializers.readUUID(deserialization);
                IExchangeData data = deserialization.readObject();
                
                map.put(nodeId, data);
            }
            dataExchanges.add(map);
        }
        
        return new FlushExchangeSetMessagePart(dataExchanges);
    }
}
