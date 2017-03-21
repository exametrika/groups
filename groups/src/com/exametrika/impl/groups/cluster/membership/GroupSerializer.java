/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link GroupSerializer} is a serializer of {@link Group}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class GroupSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("98ddac0c-9236-4611-9ec5-f9f3566a6af5");
    
    public GroupSerializer()
    {
        super(ID, Group.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        UUID groupId = Serializers.readUUID(deserialization);
        String name = deserialization.readString();
        boolean primary = deserialization.readBoolean();
        
        int count = deserialization.readInt();
        List<INode> members = new ArrayList<INode>(count);
        for (int i = 0; i < count; i++)
            members.add(deserialization.readTypedObject(Node.class));
        
        return new Group(new GroupAddress(groupId, name), primary, members);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        Group group = (Group)object;
        Serializers.writeUUID(serialization, group.getId());
        serialization.writeString(group.getName());
        serialization.writeBoolean(group.isPrimary());
        
        serialization.writeInt(group.getMembers().size());
        for (INode member : group.getMembers())
            serialization.writeTypedObject(member);
    }
}
