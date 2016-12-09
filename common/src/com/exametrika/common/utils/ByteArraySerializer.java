/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;


/**
 * The {@link ByteArraySerializer} is serializer for {@link ByteArray}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ByteArraySerializer extends AbstractSerializer
{
    private static final UUID ID = UUID.fromString("1fb878fb-5eaa-437b-91e7-472264a86ff2");
    
    public ByteArraySerializer()
    {
        super(ID, ByteArray.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        ByteArray buffer = (ByteArray)object;
        serialization.writeByteArray(buffer);
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        return deserialization.readByteArray();
    }
}
