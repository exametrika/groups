/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link GroupDefinitionSerializer} is serializer for {@link GroupDefinition}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupDefinitionSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("a849f457-412c-456e-808f-938742253aeb");

    public GroupDefinitionSerializer()
    {
        super(ID, GroupDefinition.class);
    }
    
    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        GroupDefinition group = (GroupDefinition)object;

        serialization.writeString(group.getDomain());
        Serializers.writeUUID(serialization, group.getId());
        serialization.writeString(group.getName());
        serialization.writeString(group.getNodeFilterExpression());
        serialization.writeInt(group.getNodeCount());
        serialization.writeString(group.getType());
        Serializers.writeEnumSet(serialization, group.getOptions());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        String domain = deserialization.readString();
        UUID groupId = Serializers.readUUID(deserialization);
        String name = deserialization.readString();
        String nodeFilterExpression = deserialization.readString();
        int nodeCount = deserialization.readInt();
        String type = deserialization.readString();
        Set<GroupOption> options = Serializers.readEnumSet(deserialization, GroupOption.class);
        
        return new GroupDefinition(domain, groupId, name, options, nodeFilterExpression, nodeCount, type);
    }
}
