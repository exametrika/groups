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

/**
 * The {@link FailureAtomicExchangeDataSerializer} is a serializer for {@link FailureAtomicExchangeData}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FailureAtomicExchangeDataSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("e715c847-698a-4a9e-b426-0c85cf108d3f");
 
    public FailureAtomicExchangeDataSerializer()
    {
        super(ID, FailureAtomicExchangeData.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        FailureAtomicExchangeData data = (FailureAtomicExchangeData)object;

        serialization.writeLong(data.getId());
        
        Serializers.writeVarInt(serialization, data.getMissingMessageInfos().size());
        for (MissingMessageInfo info : data.getMissingMessageInfos())
        {
            Serializers.writeUUID(serialization, info.getFailedSenderId());
            Serializers.writeVarLong(serialization, info.getLastReceivedMessageId());
        }
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        long dataId = deserialization.readLong();
        int count = Serializers.readVarInt(deserialization);
        List<MissingMessageInfo> missingMessageInfos = new ArrayList<MissingMessageInfo>(count);
        for (int i = 0; i < count; i++)
        {
            UUID failedSenderId = Serializers.readUUID(deserialization);
            long lastReceivedMessageId = Serializers.readVarLong(deserialization);

            missingMessageInfos.add(new MissingMessageInfo(failedSenderId, lastReceivedMessageId));
        }
        
        return new FailureAtomicExchangeData(dataId, missingMessageInfos);
    }
}
