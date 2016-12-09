/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.exchange;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link DataExchangeMessagePartSerializer} is a serializer for {@link DataExchangeMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DataExchangeMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("d30e852f-9ed1-479a-96ac-7d0587fa5c06");
 
    public DataExchangeMessagePartSerializer()
    {
        super(ID, DataExchangeMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        DataExchangeMessagePart part = (DataExchangeMessagePart)object;

        serialization.writeInt(part.getProviderExchanges().size());
        for (Map.Entry<UUID, ProviderExchangeData> providerEntry : part.getProviderExchanges().entrySet())
        {
            Serializers.writeUUID(serialization, providerEntry.getKey());
            
            ProviderExchangeData data = providerEntry.getValue();
            serialization.writeInt(data.getNodeExchanges().size());
            
            for (Map.Entry<UUID, NodeExchangeData> nodeEntry : data.getNodeExchanges().entrySet())
            {
                Serializers.writeUUID(serialization, nodeEntry.getKey());
                serialization.writeLong(nodeEntry.getValue().getId());
                serialization.writeObject(nodeEntry.getValue().getData());
            }
        }
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int providerCount = deserialization.readInt();
        
        Map<UUID, ProviderExchangeData> providerExchanges = new HashMap<UUID, ProviderExchangeData>();
        for (int i = 0; i < providerCount; i++)
        {
            UUID providerId = Serializers.readUUID(deserialization);
            int nodeCount = deserialization.readInt();
            
            Map<UUID, NodeExchangeData> nodeExchanges = new HashMap<UUID, NodeExchangeData>();
            for (int k = 0; k < nodeCount; k++)
            {
                UUID nodeId = Serializers.readUUID(deserialization);
                long exchangeId = deserialization.readLong();
                IExchangeData data = deserialization.readObject();
                
                nodeExchanges.put(nodeId, new NodeExchangeData(exchangeId, data));
            }
            
            providerExchanges.put(providerId, new ProviderExchangeData(nodeExchanges));
        }
        
        return new DataExchangeMessagePart(providerExchanges);
    }
}
