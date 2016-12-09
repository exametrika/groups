/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link FlushMessagePartSerializer} is a serializer for {@link FlushMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FlushMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("4a250f04-da96-47cb-b40c-a0c0b9e13681");
 
    public FlushMessagePartSerializer()
    {
        super(ID, FlushMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        FlushMessagePart part = (FlushMessagePart)object;
        Serializers.writeEnum(serialization, part.getType());
        
        serialization.writeInt(part.getFailedMembers().size());
        
        for (UUID nodeId : part.getFailedMembers())
            Serializers.writeUUID(serialization, nodeId);
        
        serialization.writeInt(part.getLeftMembers().size());
        
        for (UUID nodeId : part.getLeftMembers())
            Serializers.writeUUID(serialization, nodeId);
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        FlushMessagePart.Type type = Serializers.readEnum(deserialization, FlushMessagePart.Type.class);
        int count = deserialization.readInt();
        
        Set<UUID> failedMembers = new HashSet<UUID>();
        for (int i = 0; i < count; i++)
            failedMembers.add(Serializers.readUUID(deserialization));
        
        count = deserialization.readInt();
        
        Set<UUID> leftMembers = new HashSet<UUID>();
        for (int i = 0; i < count; i++)
            leftMembers.add(Serializers.readUUID(deserialization));
        
        return new FlushMessagePart(type, failedMembers, leftMembers);
    }
}
