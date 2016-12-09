/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.messaging.IAddress;

/**
 * The {@link MembershipResponseMessagePartSerializer} is a serializer for {@link MembershipResponseMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MembershipResponseMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("1afdb7af-4b45-488e-9cfa-982e5b52083a");
 
    public MembershipResponseMessagePartSerializer()
    {
        super(ID, MembershipResponseMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        MembershipResponseMessagePart part = (MembershipResponseMessagePart)object;

        serialization.writeLong(part.getMembershipId());
        serialization.writeInt(part.getHealthyMembers().size());
        for (IAddress address : part.getHealthyMembers())
            serialization.writeObject(address);
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        long membershipId = deserialization.readLong();
        int count = deserialization.readInt();
        
        List<IAddress> healthyMembers = new ArrayList<IAddress>();
        for (int i = 0; i < count; i++)
            healthyMembers.add((IAddress)deserialization.readObject());
        
        return new MembershipResponseMessagePart(membershipId, healthyMembers);
    }
}
