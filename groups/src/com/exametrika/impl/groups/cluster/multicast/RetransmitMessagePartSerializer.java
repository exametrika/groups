/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.impl.message.Message;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link RetransmitMessagePartSerializer} is a serializer for {@link RetransmitMessagePart}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RetransmitMessagePartSerializer extends AbstractSerializer
{
    public static final UUID ID = UUID.fromString("444d4821-b0fe-437b-bafb-330b11a612f2");
 
    public RetransmitMessagePartSerializer()
    {
        super(ID, RetransmitMessagePart.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object)
    {
        RetransmitMessagePart part = (RetransmitMessagePart)object;
        Assert.isTrue(!part.getRetransmittedMessages().isEmpty());
        
        Serializers.writeUUID(serialization, part.getFailedNodeId());
        serialization.writeLong(part.getFlushId());
        
        IMessage message = part.getRetransmittedMessages().get(0);
        serialization.writeObject(message.getSource());
        serialization.writeObject(message.getDestination());
        
        Serializers.writeVarInt(serialization, part.getRetransmittedMessages().size());

        ByteOutputStream stream = new ByteOutputStream(0x1000);
        Serialization messageSerialization = new Serialization(serialization.getRegistry(), true, stream);
        for (IMessage retransmittedMessage : part.getRetransmittedMessages())
            MessageSerializers.serialize(messageSerialization, (Message)retransmittedMessage);
        serialization.writeByteArray(new ByteArray(stream.getBuffer(), 0, stream.getLength()));
    }
    
    @Override
    public Object deserialize(IDeserialization deserialization, UUID id)
    {
        UUID failedNodeId = Serializers.readUUID(deserialization);
        long flushId = deserialization.readLong();
        IAddress source = deserialization.readObject();
        IAddress destination = deserialization.readObject();
        int count = Serializers.readVarInt(deserialization);
        
        ByteArray data = deserialization.readByteArray();
        ByteInputStream stream = new ByteInputStream(data.getBuffer(), data.getOffset(), data.getLength());
        Deserialization messageDeserialization = new Deserialization(deserialization.getRegistry(), stream);
        List<IMessage> retransmittedMessages = new ArrayList<IMessage>();
        for (int i = 0; i < count; i++)
            retransmittedMessages.add(MessageSerializers.deserialize(messageDeserialization, source, destination, null));
        
        return new RetransmitMessagePart(failedNodeId, flushId, retransmittedMessages);
    }
}
