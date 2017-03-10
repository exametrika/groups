/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.INode;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.groups.core.membership.Node;

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
        List<INode> joinedCoreNodes = new ArrayList<INode>(count);
        for (int i = 0; i < count; i++)
            joinedCoreNodes.add(deserialization.readTypedObject(Node.class));
        
        count = deserialization.readInt();
        Set<UUID> leftCoreNodes = new LinkedHashSet<UUID>(count);
        for (int i = 0; i < count; i++)
            leftCoreNodes.add(Serializers.readUUID(deserialization));
        
        count = deserialization.readInt();
        Set<UUID> failedCoreNodes = new LinkedHashSet<UUID>(count);
        for (int i = 0; i < count; i++)
            failedCoreNodes.add(Serializers.readUUID(deserialization));
        
        count = deserialization.readInt();
        Map<UUID, UUID> newCoreByWorkerMap = new LinkedHashMap<UUID, UUID>(count);
        for (int i = 0; i < count; i++)
            newCoreByWorkerMap.put(Serializers.readUUID(deserialization), Serializers.readUUID(deserialization));
        
        return new WorkerToCoreMembershipDelta(joinedCoreNodes, leftCoreNodes, failedCoreNodes, newCoreByWorkerMap);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        WorkerToCoreMembershipDelta delta = (WorkerToCoreMembershipDelta)object;

        serialization.writeInt(delta.getJoinedCoreNodes().size());
        for (INode node : delta.getJoinedCoreNodes())
            serialization.writeTypedObject(node);
        
        serialization.writeInt(delta.getLeftCoreNodes().size());
        for (UUID node : delta.getLeftCoreNodes())
            Serializers.writeUUID(serialization, node);
        
        serialization.writeInt(delta.getFailedCoreNodes().size());
        for (UUID node : delta.getFailedCoreNodes())
            Serializers.writeUUID(serialization, node);
        
        serialization.writeInt(delta.getNewCoreByWorkerMap().size());
        for (Map.Entry<UUID, UUID> entry : delta.getNewCoreByWorkerMap().entrySet())
        {
            Serializers.writeUUID(serialization, entry.getKey());
            Serializers.writeUUID(serialization, entry.getValue());
        }
    }
}
