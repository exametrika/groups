/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link ClusterMembershipResponseMessagePartSerializer} is a serializer for {@link ClusterMembershipResponseMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ClusterMembershipResponseMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("cc5129dd-37c2-475b-a436-18805ce58375");
 
    public ClusterMembershipResponseMessagePartSerializer()
    {
        super(ID, ClusterMembershipResponseMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        ClusterMembershipResponseMessagePart part = (ClusterMembershipResponseMessagePart)object;

        serialization.writeLong(part.getRoundId());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        long roundId = deserialization.readLong();
        
        return new ClusterMembershipResponseMessagePart(roundId);
    }
}
