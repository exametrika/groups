/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.impl.groups.cluster.membership.Node;

/**
 * The {@link GroupJoinMessagePartSerializer} is a serializer for {@link GroupJoinMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupJoinMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("ff884aed-e581-42f8-90c6-fa977e7e47f4");
 
    public GroupJoinMessagePartSerializer()
    {
        super(ID, GroupJoinMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        GroupJoinMessagePart part = (GroupJoinMessagePart)object;

        serialization.writeTypedObject(part.getJoiningNode());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        INode joiningNode = deserialization.readTypedObject(Node.class);
        
        return new GroupJoinMessagePart(joiningNode);
    }
}
