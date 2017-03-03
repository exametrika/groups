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
 * The {@link DomainMembershipDeltaSerializer} is a serializer of {@link DomainMembershipDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class DomainMembershipDeltaSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("0c069989-86a5-4fe2-9d0a-96bc710f32bd");
    
    public DomainMembershipDeltaSerializer()
    {
        super(ID, DomainMembershipDelta.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        String name = deserialization.readString();
        int count = deserialization.readInt();
        List<IClusterMembershipElementDelta> deltas = new ArrayList<IClusterMembershipElementDelta>(count);
        for (int i = 0; i < count; i++)
            deltas.add(deserialization.<IClusterMembershipElementDelta>readObject());
        
        return new DomainMembershipDelta(name, deltas);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        DomainMembershipDelta delta = (DomainMembershipDelta)object;

        serialization.writeString(delta.getName());
        serialization.writeInt(delta.getDeltas().size());
        for (IClusterMembershipElementDelta elementDelta : delta.getDeltas())
            serialization.writeObject(elementDelta);
    }
}
