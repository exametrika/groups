/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.optimize;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link BundleMessagePartSerializer} is a serializer for {@link BundleMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class BundleMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("12e7b598-c4ac-4a30-87fd-b71f42e48a3c");
 
    public BundleMessagePartSerializer()
    {
        super(ID, BundleMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        BundleMessagePart part = (BundleMessagePart)object;
        
        serialization.writeByteArray(part.getData());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        ByteArray data = deserialization.readByteArray();
        
        return new BundleMessagePart(data);
    }
}
