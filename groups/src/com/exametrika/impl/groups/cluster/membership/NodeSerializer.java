/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;

/**
 * The {@link NodeSerializer} is a serializer of {@link Node}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class NodeSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("df246f79-9a45-4fd8-8ac6-50dd25e51df7");
    
    public NodeSerializer()
    {
        super(ID, Node.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        IAddress address = deserialization.readTypedObject(UnicastAddress.class);
        
        int count = deserialization.readInt();
        Map<String, Object> properties = new LinkedHashMap<String, Object>(count);
        for (int i = 0; i < count; i++)
        {
            String key = deserialization.readString();
            Object value = deserialization.readObject();
            
            properties.put(key, value);
        }
        
        String domain = deserialization.readString();
        
        return new Node(address, properties, domain);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        Node node = (Node)object;
        serialization.writeTypedObject(node.getAddress());
        
        Map<String, Object> properties = node.getProperties();
        serialization.writeInt(properties.size());
        for (Map.Entry<String, Object> entry : properties.entrySet())
        {
            serialization.writeString(entry.getKey());
            serialization.writeObject(entry.getValue());
        }
        
        serialization.writeString(node.getDomain());
    }
}
