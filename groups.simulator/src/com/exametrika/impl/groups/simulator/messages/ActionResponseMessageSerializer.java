/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.messages;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link ActionResponseMessageSerializer} is a serializer for {@link ActionResponseMessage}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ActionResponseMessageSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("ab28394c-7f6b-47c1-9b23-9f6f92d1498");
 
    public ActionResponseMessageSerializer()
    {
        super(ID, ActionResponseMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        ActionResponseMessage part = (ActionResponseMessage)object;

        serialization.writeObject(part.getResult());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        Object result = deserialization.readObject();
        
        return new ActionResponseMessage(result);
    }
}
