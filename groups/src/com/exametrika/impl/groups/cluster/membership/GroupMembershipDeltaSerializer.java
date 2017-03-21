/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link GroupMembershipDeltaSerializer} is a serializer of {@link GroupMembershipDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class GroupMembershipDeltaSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("83e0b3d4-ff81-4196-9923-da60be51c8f0");
    
    public GroupMembershipDeltaSerializer()
    {
        super(ID, GroupMembershipDelta.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        long newMembershipId = deserialization.readLong();
        GroupDelta group = deserialization.readTypedObject(GroupDelta.class);
        
        return new GroupMembershipDelta(newMembershipId, group);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        GroupMembershipDelta delta = (GroupMembershipDelta)object;

        serialization.writeLong(delta.getId());
        serialization.writeTypedObject(delta.getGroup());
    }
}
