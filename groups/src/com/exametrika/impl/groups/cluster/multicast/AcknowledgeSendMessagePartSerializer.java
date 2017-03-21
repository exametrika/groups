/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link AcknowledgeSendMessagePartSerializer} is a serializer for {@link AcknowledgeSendMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class AcknowledgeSendMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("5e0fd9dd-329b-4482-8eb1-0644aef09234");
 
    public AcknowledgeSendMessagePartSerializer()
    {
        super(ID, AcknowledgeSendMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        AcknowledgeSendMessagePart part = (AcknowledgeSendMessagePart)object;

        Serializers.writeVarLong(serialization, part.getLastReceivedMessageId());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        long lastReceivedMessageId = Serializers.readVarLong(deserialization);
        
        return new AcknowledgeSendMessagePart(lastReceivedMessageId);
    }
}
