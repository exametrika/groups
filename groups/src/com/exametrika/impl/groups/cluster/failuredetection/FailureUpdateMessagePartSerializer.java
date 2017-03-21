/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link FailureUpdateMessagePartSerializer} is a serializer for {@link FailureUpdateMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FailureUpdateMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("a889c2d2-8b15-471f-995f-6ad831f6a940");
 
    public FailureUpdateMessagePartSerializer()
    {
        super(ID, FailureUpdateMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        FailureUpdateMessagePart part = (FailureUpdateMessagePart)object;

        serialization.writeInt(part.getFailedMembers().size());
        
        for (UUID id : part.getFailedMembers())
            Serializers.writeUUID(serialization, id);
        
        serialization.writeInt(part.getLeftMembers().size());
        
        for (UUID id : part.getLeftMembers())
            Serializers.writeUUID(serialization, id);
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int count = deserialization.readInt();
        
        Set<UUID> failedMembers = new HashSet<UUID>();
        for (int i = 0; i < count; i++)
            failedMembers.add(Serializers.readUUID(deserialization));
        
        count = deserialization.readInt();
        
        Set<UUID> leftMembers = new HashSet<UUID>();
        for (int i = 0; i < count; i++)
            leftMembers.add(Serializers.readUUID(deserialization));
        
        return new FailureUpdateMessagePart(failedMembers, leftMembers);
    }
}
