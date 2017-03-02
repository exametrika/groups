/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link WorkerToCoreMembershipDeltaSerializer} is a serializer of {@link WorkerToCoreMembershipDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class WorkerToCoreMembershipDeltaSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("4b08b799-f03c-43da-a55a-5fd9c1af14aa");
    
    public WorkerToCoreMembershipDeltaSerializer()
    {
        super(ID, WorkerToCoreMembershipDelta.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int count = deserialization.readInt();
        Map<UUID, UUID> newCoreByWorkerMap = new LinkedHashMap<UUID, UUID>(count);
        for (int i = 0; i < count; i++)
            newCoreByWorkerMap.put(Serializers.readUUID(deserialization), Serializers.readUUID(deserialization));
        
        return new WorkerToCoreMembershipDelta(newCoreByWorkerMap);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        WorkerToCoreMembershipDelta delta = (WorkerToCoreMembershipDelta)object;

        serialization.writeInt(delta.getNewCoreByWorkerMap().size());
        for (Map.Entry<UUID, UUID> entry : delta.getNewCoreByWorkerMap().entrySet())
        {
            Serializers.writeUUID(serialization, entry.getKey());
            Serializers.writeUUID(serialization, entry.getValue());
        }
    }
}
