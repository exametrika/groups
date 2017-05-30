/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.check;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link StateChecksumResponseMessagePartSerializer} is a serializer for {@link StateChecksumResponseMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StateChecksumResponseMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("602e1d95-1912-4318-8f75-046a3cf333c7");
 
    public StateChecksumResponseMessagePartSerializer()
    {
        super(ID, StateChecksumResponseMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        StateChecksumResponseMessagePart part = (StateChecksumResponseMessagePart)object;

        serialization.writeLong(part.getChecksum());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        long checksum = deserialization.readLong();
        
        return new StateChecksumResponseMessagePart(checksum);
    }
}
