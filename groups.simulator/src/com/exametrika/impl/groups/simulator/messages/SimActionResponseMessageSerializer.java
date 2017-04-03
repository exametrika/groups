/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.messages;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link SimActionResponseMessageSerializer} is a serializer for {@link SimActionResponseMessage}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimActionResponseMessageSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("ab28394c-7f6b-47c1-9b23-9f6f92d1498");
 
    public SimActionResponseMessageSerializer()
    {
        super(ID, SimActionResponseMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        SimActionResponseMessage part = (SimActionResponseMessage)object;

        serialization.writeString(part.getActionName());
        serialization.writeObject(part.getResult());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        String actionName = deserialization.readString();
        Object result = deserialization.readObject();
        
        return new SimActionResponseMessage(actionName, result);
    }
}
