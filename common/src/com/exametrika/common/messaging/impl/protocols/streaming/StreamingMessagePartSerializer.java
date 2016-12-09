/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.streaming;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link StreamingMessagePartSerializer} is a serializer for {@link StreamingMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StreamingMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("c6185ce5-aaf3-4d4d-93f0-943ce9763959");
 
    public StreamingMessagePartSerializer()
    {
        super(ID, StreamingMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        StreamingMessagePart part = (StreamingMessagePart)object;

        serialization.writeInt(part.getId());
        serialization.writeInt(part.getStreamIndex());
        serialization.writeInt(part.getStreamCount());
        serialization.writeByte(part.getFlags());
        serialization.writeByteArray(part.getFragment());
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        int streamId = deserialization.readInt();
        int streamIndex = deserialization.readInt();
        int streamCount = deserialization.readInt();
        byte data = deserialization.readByte();
        ByteArray fragment = deserialization.readByteArray();
        
        return new StreamingMessagePart(streamId, streamIndex, streamCount, data, fragment);
    }
}
