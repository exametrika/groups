/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import java.util.UUID;

import com.exametrika.api.groups.core.IGroup;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link MembershipSerializer} is a serializer of {@link Membership}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class MembershipSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("4eddd68a-43a6-44b2-bec1-6edcd6415cf9");
    
    public MembershipSerializer()
    {
        super(ID, Membership.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        long membershipId = deserialization.readLong();
        IGroup group = deserialization.readTypedObject(Group.class);
        
        return new Membership(membershipId, group);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        Membership membership = (Membership)object;
        serialization.writeLong(membership.getId());
        serialization.writeTypedObject(membership.getGroup());
    }
}
