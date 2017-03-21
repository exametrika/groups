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
 * The {@link CompleteMessagePartSerializer} is a serializer for {@link CompleteMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompleteMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("87de1ef1-4e3a-4b62-b15b-c6532d8cf1f0");
 
    public CompleteMessagePartSerializer()
    {
        super(ID, CompleteMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        CompleteMessagePart part = (CompleteMessagePart)object;

        Serializers.writeVarLong(serialization, part.getCompletedMessageId());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        long completedMessageId = Serializers.readVarLong(deserialization);
        
        return new CompleteMessagePart(completedMessageId);
    }
}
