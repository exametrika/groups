/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link GroupMembershipSerializer} is a serializer of {@link GroupMembership}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class GroupMembershipSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("4eddd68a-43a6-44b2-bec1-6edcd6415cf9");
    
    public GroupMembershipSerializer()
    {
        super(ID, GroupMembership.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        long membershipId = deserialization.readLong();
        IGroup group = deserialization.readTypedObject(Group.class);
        
        return new GroupMembership(membershipId, group);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        GroupMembership membership = (GroupMembership)object;
        serialization.writeLong(membership.getId());
        serialization.writeTypedObject(membership.getGroup());
    }
}
