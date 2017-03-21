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
 * The {@link FailureAtomicMessagePartSerializer} is a serializer for {@link FailureAtomicMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FailureAtomicMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("a91137b1-9fdd-46a3-bef8-8ead0500a4eb");
 
    public FailureAtomicMessagePartSerializer()
    {
        super(ID, FailureAtomicMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        FailureAtomicMessagePart part = (FailureAtomicMessagePart)object;

        Serializers.writeVarLong(serialization, part.getMessageId());
        Serializers.writeVarLong(serialization, part.getOrder());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        long messageId = Serializers.readVarLong(deserialization);
        long order = Serializers.readVarLong(deserialization);
        
        return new FailureAtomicMessagePart(messageId, order);
    }
}
