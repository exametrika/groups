/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.compression;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link CompressionMessagePartSerializer} is a serializer for {@link CompressionMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompressionMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("1613ceaa-58a2-41d4-b8f8-6d25ac5b9d87");
 
    public CompressionMessagePartSerializer()
    {
        super(ID, CompressionMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        CompressionMessagePart part = (CompressionMessagePart)object;

        serialization.writeInt(part.getDecompressedSize());
        serialization.writeByteArray(part.getCompressedMessage());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int decompressedSize = deserialization.readInt();
        ByteArray compressedMessage = deserialization.readByteArray();
        
        return new CompressionMessagePart(decompressedSize, compressedMessage, null);
    }
}
