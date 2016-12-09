/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.groups.core.membership.IMembershipDelta;
import com.exametrika.impl.groups.core.membership.Membership;
import com.exametrika.impl.groups.core.membership.MembershipDelta;

/**
 * The {@link FlushStartMessagePartSerializer} is a serializer for {@link FlushStartMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FlushStartMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("449c7609-9ac1-4314-8882-ce1c583e744e");
 
    public FlushStartMessagePartSerializer()
    {
        super(ID, FlushStartMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        FlushStartMessagePart part = (FlushStartMessagePart)object;
        serialization.writeBoolean(part.isGroupForming());
        serialization.writeTypedObject(part.getPreparedMembership());
        serialization.writeTypedObject(part.getPreparedMembershipDelta());
        
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
        boolean groupForming = deserialization.readBoolean();
        IMembership preparedMembership = deserialization.readTypedObject(Membership.class);
        IMembershipDelta preparedMembershipDelta = deserialization.readTypedObject(MembershipDelta.class);
        int count = deserialization.readInt();
        
        Set<UUID> failedMembers = new HashSet<UUID>();
        for (int i = 0; i < count; i++)
            failedMembers.add(Serializers.readUUID(deserialization));
        
        count = deserialization.readInt();
        
        Set<UUID> leftMembers = new HashSet<UUID>();
        for (int i = 0; i < count; i++)
            leftMembers.add(Serializers.readUUID(deserialization));
        
        return new FlushStartMessagePart(groupForming, preparedMembership, preparedMembershipDelta, failedMembers, leftMembers);
    }
}
