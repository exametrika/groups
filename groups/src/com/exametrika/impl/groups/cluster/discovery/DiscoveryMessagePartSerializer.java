/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.impl.groups.cluster.membership.Node;

/**
 * The {@link DiscoveryMessagePartSerializer} is a serializer for {@link DiscoveryMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DiscoveryMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("adeabda1-d031-4bb2-9958-20270240f7c7");
 
    public DiscoveryMessagePartSerializer()
    {
        super(ID, DiscoveryMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        DiscoveryMessagePart part = (DiscoveryMessagePart)object;

        serialization.writeBoolean(part.isCore());
        serialization.writeInt(part.getDiscoveredNodes().size());
        
        for (INode node : part.getDiscoveredNodes())
            serialization.writeTypedObject(node);
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        boolean core = deserialization.readBoolean();
        int count = deserialization.readInt();
        
        Set<INode> discoveredNodes = new HashSet<INode>();
        for (int i = 0; i < count; i++)
            discoveredNodes.add(deserialization.readTypedObject(Node.class));
        
        return new DiscoveryMessagePart(discoveredNodes, core);
    }
}
