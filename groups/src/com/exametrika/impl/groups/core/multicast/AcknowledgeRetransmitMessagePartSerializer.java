/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link AcknowledgeRetransmitMessagePartSerializer} is a serializer for {@link AcknowledgeRetransmitMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class AcknowledgeRetransmitMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("eca17dfc-5f4c-43bf-8e02-18ba67fe244c");
 
    public AcknowledgeRetransmitMessagePartSerializer()
    {
        super(ID, AcknowledgeRetransmitMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        AcknowledgeRetransmitMessagePart part = (AcknowledgeRetransmitMessagePart)object;

        Serializers.writeUUID(serialization, part.getFailedNodeId());
        Serializers.writeVarLong(serialization, part.getFlushId());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        UUID failedNodeId = Serializers.readUUID(deserialization);
        long flushId = Serializers.readVarLong(deserialization);
        
        return new AcknowledgeRetransmitMessagePart(failedNodeId, flushId);
    }
}
