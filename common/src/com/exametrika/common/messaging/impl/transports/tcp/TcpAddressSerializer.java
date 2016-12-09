/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.SerializationException;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link TcpAddressSerializer} is serializer for {@link TcpAddress}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpAddressSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("d7b80761-b1e7-455a-bbac-de9ce48abc03");

    public TcpAddressSerializer()
    {
        super(ID, TcpAddress.class);
    }
    
    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        TcpAddress nodeAddress = (TcpAddress)object;
        InetSocketAddress address = nodeAddress.getAddress();

        Serializers.writeUUID(serialization, nodeAddress.getId());
        serialization.writeInt(address.getPort());
        serialization.writeByteArray(new ByteArray(address.getAddress().getAddress()));
        serialization.writeString(nodeAddress.getName());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        UUID nodeId = Serializers.readUUID(deserialization);
        int port = deserialization.readInt();
        ByteArray addressData = deserialization.readByteArray();
        String name = deserialization.readString();
        
        try
        {
            return new TcpAddress(nodeId, new InetSocketAddress(InetAddress.getByAddress(addressData.toByteArray()), port), name);
        }
        catch (UnknownHostException e)
        {
            throw new SerializationException(e);
        }
    }
}
