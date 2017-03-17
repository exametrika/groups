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
 * The {@link GroupDeltaSerializer} is a serializer of {@link GroupDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class GroupDeltaSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("7dc3fd77-758b-4793-a892-5dd4758ceabc");
    
    public GroupDeltaSerializer()
    {
        super(ID, GroupDelta.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        UUID groupId = Serializers.readUUID(deserialization);
        boolean primary = deserialization.readBoolean();
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
        
        return new GroupDelta(groupId, primary, joinedMembers, leftMembers, failedMembers);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        GroupDelta delta = (GroupDelta)object;

        Serializers.writeUUID(serialization, delta.getId());
        serialization.writeBoolean(delta.isPrimary());
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
