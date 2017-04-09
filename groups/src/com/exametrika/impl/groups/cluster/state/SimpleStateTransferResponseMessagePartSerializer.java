/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link SimpleStateTransferResponseMessagePartSerializer} is a serializer for {@link SimpleStateTransferResponseMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimpleStateTransferResponseMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("5f9bb159-890d-4d27-92c5-cc789eeb8b1");
 
    public SimpleStateTransferResponseMessagePartSerializer()
    {
        super(ID, SimpleStateTransferResponseMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        SimpleStateTransferResponseMessagePart part = (SimpleStateTransferResponseMessagePart)object;

        serialization.writeByteArray(part.getState());;
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        ByteArray state = deserialization.readByteArray();
        return new SimpleStateTransferResponseMessagePart(state);
    }
}
