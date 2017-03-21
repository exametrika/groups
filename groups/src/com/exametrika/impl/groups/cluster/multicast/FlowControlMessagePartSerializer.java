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
 * The {@link FlowControlMessagePartSerializer} is a serializer for {@link FlowControlMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FlowControlMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("82f13e3e-a3d9-40fc-bb2d-a7423d0c5e64");
 
    public FlowControlMessagePartSerializer()
    {
        super(ID, FlowControlMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        FlowControlMessagePart part = (FlowControlMessagePart)object;

        Serializers.writeUUID(serialization, part.getFlowId());
        serialization.writeBoolean(part.isBlocked());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        UUID flowId = Serializers.readUUID(deserialization);
        boolean blocked = deserialization.readBoolean();
        
        return new FlowControlMessagePart(flowId, blocked);
    }
}
