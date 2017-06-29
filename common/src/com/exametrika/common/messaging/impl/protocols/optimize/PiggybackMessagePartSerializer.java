/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.optimize;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.messaging.IMessagePart;

/**
 * The {@link PiggybackMessagePartSerializer} is a serializer for {@link PiggybackMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class PiggybackMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("1b0e129d-1916-4c85-ab8a-44553ce5c742");
 
    public PiggybackMessagePartSerializer()
    {
        super(ID, PiggybackMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        PiggybackMessagePart part = (PiggybackMessagePart)object;
        
        serialization.writeInt(part.getParts().size());
        for (IMessagePart child : part.getParts())
            serialization.writeObject(child);
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int count = deserialization.readInt();
        List<IMessagePart> parts = new ArrayList<IMessagePart>(count);
        for (int i = 0; i < count; i++)
        {
            IMessagePart child = deserialization.readObject();
            parts.add(child);
        }
        
        return new PiggybackMessagePart(parts);
    }
}
