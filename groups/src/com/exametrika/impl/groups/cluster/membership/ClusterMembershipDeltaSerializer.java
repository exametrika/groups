/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link ClusterMembershipDeltaSerializer} is a serializer of {@link ClusterMembershipDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class ClusterMembershipDeltaSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("38ba5769-6410-40bc-8aef-ea4ff884bd65");
    
    public ClusterMembershipDeltaSerializer()
    {
        super(ID, ClusterMembershipDelta.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        long newMembershipId = deserialization.readLong();
        boolean full = deserialization.readBoolean();
        int count = deserialization.readInt();
        List<IDomainMembershipDelta> domains = new ArrayList<IDomainMembershipDelta>(count);
        for (int i = 0; i < count; i++)
            domains.add(deserialization.<IDomainMembershipDelta>readObject());
        
        return new ClusterMembershipDelta(newMembershipId, full, domains);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        ClusterMembershipDelta delta = (ClusterMembershipDelta)object;

        serialization.writeLong(delta.getId());
        serialization.writeBoolean(delta.isFull());
        serialization.writeInt(delta.getDomains().size());
        for (IDomainMembershipDelta elementDelta : delta.getDomains())
            serialization.writeObject(elementDelta);
    }
}
