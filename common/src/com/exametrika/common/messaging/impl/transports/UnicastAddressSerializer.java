/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link UnicastAddressSerializer} is serializer for {@link UnicastAddress}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class UnicastAddressSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("d7b80761-b1e7-455a-bbac-de9ce48abc03");

    public UnicastAddressSerializer()
    {
        super(ID, UnicastAddress.class);
    }
    
    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        UnicastAddress address = (UnicastAddress)object;

        Serializers.writeUUID(serialization, address.getId());
        serialization.writeString(address.getName());
        serialization.writeInt(address.getCount());
        for (int i = 0; i < address.getCount(); i++)
        {
            serialization.writeObject(address.getAddress(i));
            serialization.writeString(address.getConnection(i));
        }
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        UUID nodeId = Serializers.readUUID(deserialization);
        String name = deserialization.readString();
        UnicastAddress address = new UnicastAddress(nodeId, name);
        int count = deserialization.readInt();
        for (int i = 0; i < count; i++)
            address.setAddress(i, deserialization.readObject(), deserialization.readString());
        
        return address;
    }
}
