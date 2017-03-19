/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.messages;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link ActionMessageSerializer} is a serializer for {@link ActionMessage}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ActionMessageSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("5811a6e7-7a0d-4b33-83e5-e58cf1481213");
 
    public ActionMessageSerializer()
    {
        super(ID, ActionMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        ActionMessage part = (ActionMessage)object;

        serialization.writeString(part.getActionName());
        serialization.writeInt(part.getParameters().size());
        for (Map.Entry<String, Object> entry : part.getParameters().entrySet())
        {
            serialization.writeString(entry.getKey());
            serialization.writeObject(entry.getValue());
        }
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        String actionName = deserialization.readString();
        int count = deserialization.readInt();
        Map<String, Object> parameters = new LinkedHashMap<String, Object>(count);
        for (int i = 0; i < count; i++)
            parameters.put(deserialization.readString(), deserialization.readObject());
        
        return new ActionMessage(actionName, parameters);
    }
}
