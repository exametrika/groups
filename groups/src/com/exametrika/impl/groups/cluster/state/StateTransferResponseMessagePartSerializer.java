/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link StateTransferResponseMessagePartSerializer} is a serializer for {@link StateTransferResponseMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StateTransferResponseMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("0685057b-5038-4cc0-8b41-fc8784fae932");
 
    public StateTransferResponseMessagePartSerializer()
    {
        super(ID, StateTransferResponseMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        StateTransferResponseMessagePart part = (StateTransferResponseMessagePart)object;

        serialization.writeBoolean(part.isFirst());
        serialization.writeBoolean(part.isLast());
        serialization.writeBoolean(part.isRejected());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        boolean first = deserialization.readBoolean();
        boolean last = deserialization.readBoolean();
        boolean rejected = deserialization.readBoolean();
        return new StateTransferResponseMessagePart(first, last, rejected);
    }
}
