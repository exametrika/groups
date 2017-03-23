/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link GroupMessagePartSerializer} is a serializer for {@link GroupMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("ef9be5c1-6869-4355-a175-7dfc98ecc023");
 
    public GroupMessagePartSerializer()
    {
        super(ID, GroupMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        GroupMessagePart part = (GroupMessagePart)object;

        Serializers.writeUUID(serialization, part.getGroupId());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        UUID groupId = Serializers.readUUID(deserialization);
        
        return new GroupMessagePart(groupId);
    }
}
