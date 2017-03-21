/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.flush;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link FlushResponseMessagePartSerializer} is a serializer for {@link FlushResponseMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FlushResponseMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("2a41ce68-832c-4747-a309-2cbb3f8293de");
 
    public FlushResponseMessagePartSerializer()
    {
        super(ID, FlushResponseMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        FlushResponseMessagePart part = (FlushResponseMessagePart)object;
        serialization.writeBoolean(part.isFlushProcessingRequired());
        
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
        boolean flushProcessingRequired = deserialization.readBoolean();
        
        int count = deserialization.readInt();
        Set<UUID> failedMembers = new HashSet<UUID>(count);
        for (int i = 0; i < count; i++)
            failedMembers.add(Serializers.readUUID(deserialization));
        
        count = deserialization.readInt();
        Set<UUID> leftMembers = new HashSet<UUID>(count);
        for (int i = 0; i < count; i++)
            leftMembers.add(Serializers.readUUID(deserialization));
        
        return new FlushResponseMessagePart(flushProcessingRequired, failedMembers, leftMembers);
    }
}
