/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.INode;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link MembershipDeltaSerializer} is a serializer of {@link MembershipDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class MembershipDeltaSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("83e0b3d4-ff81-4196-9923-da60be51c8f0");
    
    public MembershipDeltaSerializer()
    {
        super(ID, MembershipDelta.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        long newMembershipId = deserialization.readLong();
        int count = deserialization.readInt();
        List<INode> joinedMembers = new ArrayList<INode>(count);
        for (int i = 0; i < count; i++)
            joinedMembers.add(deserialization.readTypedObject(Node.class));
        
        count = deserialization.readInt();
        Set<UUID> leftMembers = new LinkedHashSet<UUID>(count);
        for (int i = 0; i < count; i++)
            leftMembers.add(Serializers.readUUID(deserialization));
        
        count = deserialization.readInt();
        Set<UUID> failedMembers = new LinkedHashSet<UUID>(count);
        for (int i = 0; i < count; i++)
            failedMembers.add(Serializers.readUUID(deserialization));
        
        return new MembershipDelta(newMembershipId, joinedMembers, leftMembers, failedMembers);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        MembershipDelta delta = (MembershipDelta)object;

        serialization.writeLong(delta.getId());
        serialization.writeInt(delta.getJoinedMembers().size());
        for (INode member : delta.getJoinedMembers())
            serialization.writeTypedObject(member);
        
        serialization.writeInt(delta.getLeftMembers().size());
        for (UUID member : delta.getLeftMembers())
            Serializers.writeUUID(serialization, member);
        
        serialization.writeInt(delta.getFailedMembers().size());
        for (UUID member : delta.getFailedMembers())
            Serializers.writeUUID(serialization, member);
    }
}
