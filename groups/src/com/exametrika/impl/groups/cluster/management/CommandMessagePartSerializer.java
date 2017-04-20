/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.management;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link CommandMessagePartSerializer} is a serializer for {@link CommandMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CommandMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("7147c52f-d4fb-4143-a7d8-d173ff32cb4c");
 
    public CommandMessagePartSerializer()
    {
        super(ID, CommandMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        CommandMessagePart part = (CommandMessagePart)object;

        serialization.writeObject(part.getCommand());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        ICommand command = deserialization.readObject();
        
        return new CommandMessagePart(command);
    }
}
