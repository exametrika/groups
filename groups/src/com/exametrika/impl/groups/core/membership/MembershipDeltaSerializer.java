/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

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
        GroupDelta group = deserialization.readTypedObject(GroupDelta.class);
        
        return new MembershipDelta(newMembershipId, group);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        MembershipDelta delta = (MembershipDelta)object;

        serialization.writeLong(delta.getId());
        serialization.writeTypedObject(delta.getGroup());
    }
}
