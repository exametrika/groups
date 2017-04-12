/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.exchange;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link FeedbackMessagePartSerializer} is a serializer for {@link FeedbackMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FeedbackMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("1b672970-29cc-4ef2-ae01-12cc591ce34b");
 
    public FeedbackMessagePartSerializer()
    {
        super(ID, FeedbackMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        FeedbackMessagePart part = (FeedbackMessagePart)object;

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
            UUID providerId = Serializers.readUUID(deserialization);
            IExchangeData data = deserialization.readObject();
            
            dataExchanges.put(providerId, data);
        }
        
        return new FeedbackMessagePart(dataExchanges);
    }
}
