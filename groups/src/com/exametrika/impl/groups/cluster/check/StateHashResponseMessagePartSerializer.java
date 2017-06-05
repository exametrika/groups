/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.check;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link StateHashResponseMessagePartSerializer} is a serializer for {@link StateHashResponseMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StateHashResponseMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("602e1d95-1912-4318-8f75-046a3cf333c7");
 
    public StateHashResponseMessagePartSerializer()
    {
        super(ID, StateHashResponseMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        StateHashResponseMessagePart part = (StateHashResponseMessagePart)object;

        serialization.writeString(part.getHash());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        String hash = deserialization.readString();
        
        return new StateHashResponseMessagePart(hash);
    }
}
