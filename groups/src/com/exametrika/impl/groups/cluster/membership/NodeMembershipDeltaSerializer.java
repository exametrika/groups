/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.core.INode;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.groups.core.membership.Node;

/**
 * The {@link NodeMembershipDeltaSerializer} is a serializer of {@link NodeMembershipDelta}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class NodeMembershipDeltaSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("fcf475f2-20f6-47f9-a636-9ac82dd50095");
    
    public NodeMembershipDeltaSerializer()
    {
        super(ID, NodeMembershipDelta.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int count = deserialization.readInt();
        List<INode> joinedNodes = new ArrayList<INode>(count);
        for (int i = 0; i < count; i++)
            joinedNodes.add(deserialization.readTypedObject(Node.class));
        
        count = deserialization.readInt();
        Set<UUID> leftNodes = new LinkedHashSet<UUID>(count);
        for (int i = 0; i < count; i++)
            leftNodes.add(Serializers.readUUID(deserialization));
        
        count = deserialization.readInt();
        Set<UUID> failedNodes = new LinkedHashSet<UUID>(count);
        for (int i = 0; i < count; i++)
            failedNodes.add(Serializers.readUUID(deserialization));
        
        return new NodeMembershipDelta(joinedNodes, leftNodes, failedNodes);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        NodeMembershipDelta delta = (NodeMembershipDelta)object;

        serialization.writeInt(delta.getJoinedNodes().size());
        for (INode member : delta.getJoinedNodes())
            serialization.writeTypedObject(member);
        
        serialization.writeInt(delta.getLeftNodes().size());
        for (UUID member : delta.getLeftNodes())
            Serializers.writeUUID(serialization, member);
        
        serialization.writeInt(delta.getFailedNodes().size());
        for (UUID member : delta.getFailedNodes())
            Serializers.writeUUID(serialization, member);
    }
}