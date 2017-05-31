/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.ByteArray;

public final class TestLoadMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("9007e56f-ca9b-4110-b462-fa61de9e2c89");

    public TestLoadMessagePartSerializer()
    {
        super(ID, TestLoadMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        TestLoadMessagePart part = (TestLoadMessagePart)object;

        serialization.writeInt(part.getIndex());
        serialization.writeLong(part.getCount());
        serialization.writeByteArray(part.getValue());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int index = deserialization.readInt();
        long count = deserialization.readLong();
        ByteArray value = deserialization.readByteArray();
        return new TestLoadMessagePart(index, count, value);
    }
}