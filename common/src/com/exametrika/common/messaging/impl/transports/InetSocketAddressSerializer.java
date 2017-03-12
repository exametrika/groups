/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.SerializationException;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link InetSocketAddressSerializer} is serializer for {@link InetSocketAddress}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class InetSocketAddressSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("ce8642db-e49f-48e6-b6d2-acab5ab79f3b");

    public InetSocketAddressSerializer()
    {
        super(ID, InetSocketAddress.class);
    }
    
    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        InetSocketAddress address = (InetSocketAddress)object;

        serialization.writeInt(address.getPort());
        serialization.writeByteArray(new ByteArray(address.getAddress().getAddress()));
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int port = deserialization.readInt();
        ByteArray addressData = deserialization.readByteArray();
        
        try
        {
            return new InetSocketAddress(InetAddress.getByAddress(addressData.toByteArray()), port);
        }
        catch (UnknownHostException e)
        {
            throw new SerializationException(e);
        }
    }
}
