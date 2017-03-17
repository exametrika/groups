/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.IGroup;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.groups.core.membership.Group;
import com.exametrika.impl.groups.core.membership.GroupDelta;
import com.exametrika.impl.groups.core.membership.IGroupDelta;

/**
 * The {@link GroupMembershipDeltaSerializer} is a serializer of {@link GroupMembershipDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class GroupMembershipDeltaSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("52874a5a-514b-4148-ad72-e6cc94090ddb");
    
    public GroupMembershipDeltaSerializer()
    {
        super(ID, GroupMembershipDelta.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int count = deserialization.readInt();
        Set<IGroup> newGroups = new LinkedHashSet<IGroup>(count);
        for (int i = 0; i < count; i++)
            newGroups.add(deserialization.readTypedObject(Group.class));
        
        count = deserialization.readInt();
        Set<IGroupDelta> changedGroups = new LinkedHashSet<IGroupDelta>(count);
        for (int i = 0; i < count; i++)
            changedGroups.add(deserialization.readTypedObject(GroupDelta.class));
        
        count = deserialization.readInt();
        Set<UUID> removedGroups = new LinkedHashSet<UUID>(count);
        for (int i = 0; i < count; i++)
            removedGroups.add(Serializers.readUUID(deserialization));
        
        return new GroupMembershipDelta(newGroups, changedGroups, removedGroups);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        GroupMembershipDelta delta = (GroupMembershipDelta)object;

        serialization.writeInt(delta.getNewGroups().size());
        for (IGroup group : delta.getNewGroups())
            serialization.writeTypedObject(group);
        
        serialization.writeInt(delta.getChangedGroups().size());
        for (IGroupDelta group : delta.getChangedGroups())
            serialization.writeTypedObject(group);
        
        serialization.writeInt(delta.getRemovedGroups().size());
        for (UUID member : delta.getRemovedGroups())
            Serializers.writeUUID(serialization, member);
    }
}
