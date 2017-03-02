/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link ClusterMembershipMessagePartSerializer} is a serializer for {@link ClusterMembershipMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ClusterMembershipMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("4b3c70ec-4dd7-470a-9af6-c21a2913839d");
 
    public ClusterMembershipMessagePartSerializer()
    {
        super(ID, ClusterMembershipMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        ClusterMembershipMessagePart part = (ClusterMembershipMessagePart)object;

        serialization.writeTypedObject(part.getDelta());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        ClusterMembershipDelta delta = deserialization.readTypedObject(ClusterMembershipDelta.class);
        
        return new ClusterMembershipMessagePart(delta);
    }
}
