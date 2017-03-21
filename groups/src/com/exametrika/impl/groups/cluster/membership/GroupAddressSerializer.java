/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link GroupAddressSerializer} is serializer for {@link GroupAddress}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupAddressSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("db69d584-03ac-4849-b99a-a88c9878df75");

    public GroupAddressSerializer()
    {
        super(ID, GroupAddress.class);
    }
    
    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        GroupAddress nodeAddress = (GroupAddress)object;

        Serializers.writeUUID(serialization, nodeAddress.getId());
        serialization.writeString(nodeAddress.getName());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        UUID nodeId = Serializers.readUUID(deserialization);
        String name = deserialization.readString();
        
        return new GroupAddress(nodeId, name);
    }
}
