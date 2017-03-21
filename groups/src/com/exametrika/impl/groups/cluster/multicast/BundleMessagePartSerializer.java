/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link BundleMessagePartSerializer} is a serializer for {@link BundleMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class BundleMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("34c548ab-36b8-480e-a861-2bafae90a6c2");
 
    public BundleMessagePartSerializer()
    {
        super(ID, BundleMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        BundleMessagePart part = (BundleMessagePart)object;
        
        Serializers.writeVarLong(serialization, part.getMembershipId());
        Serializers.writeVarLong(serialization, part.getCompletedMessageId());
        serialization.writeByteArray(part.getData());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        long membershipId = Serializers.readVarLong(deserialization);
        long completedMessageId = Serializers.readVarLong(deserialization);
        
        ByteArray data = deserialization.readByteArray();
        
        return new BundleMessagePart(membershipId, completedMessageId, data);
    }
}
