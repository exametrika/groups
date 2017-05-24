/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.message;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.IVisitor;

/**
 * The {@link MessageSerializers} is utility class for serializing {@link Message}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MessageSerializers
{
    public static TcpPacket serialize(ISerializationRegistry registry, Message message, int headerOverhead, Object digest)
    {
        //MultiByteOutputStream stream = new MultiByteOutputStream(0x1000);
        ByteOutputStream stream = new ByteOutputStream(0x1000);
        stream.grow(headerOverhead);
        final ISerialization serialization = new Serialization(registry, true, stream);
        
        serialize(serialization, message);
        //stream.close();

        //return new TcpPacket(stream.getBuffers(), message.getFiles());
        return new TcpPacket(Collections.singletonList(new ByteArray(stream.getBuffer(), 0, stream.getLength())), 
            message.getFiles(), digest);
    }

    public static void serialize(final ISerialization serialization, Message message)
    {
        serialization.writeInt(message.getFlags());
        serialization.writeInt(message.getPartCount());
        
        message.visitParts(new IVisitor<IMessagePart>()
        {
            @Override
            public boolean visit(IMessagePart element)
            {
                serialization.beginWriteRegion();
                
                if (element instanceof SerializedMessagePart)
                {
                    SerializedMessagePart part = (SerializedMessagePart)element;
                    for (ByteArray buffer : part.getBuffers())
                        serialization.writeRegion(buffer);
                }
                else
                    serialization.writeObject(element);
                
                serialization.endWriteRegion();
                
                return true;
            }
        });
    }
    
    public static Message deserialize(ISerializationRegistry registry, IAddress source, IAddress destination, 
        TcpPacket packet, int headerOverhead)
    {
        Assert.isTrue(packet.getBuffers().size() == 1);
        ByteArray buffer = packet.getBuffers().get(0);
        ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset() + headerOverhead, 
            buffer.getLength() - headerOverhead);
        Deserialization deserialization = new Deserialization(registry, stream);

        return deserialize(deserialization, source, destination, packet.getFiles());
    }

    public static Message deserialize(IDeserialization deserialization, IAddress source, IAddress destination, List<File> files)
    {
        int flags = deserialization.readInt();
        
        int count = deserialization.readInt();
        List<IMessagePart> parts = new ArrayList<IMessagePart>(count);
        
        for (int i = 0; i < count; i++)
        {
            ByteArray buffer = deserialization.readRegion();
            IMessagePart part = new SerializedMessagePart(deserialization.getRegistry(), Collections.singletonList(buffer), buffer.getLength(), null);
            parts.add(part);
        }
        
        return new Message(source, destination, parts, flags, files, deserialization.getRegistry());
    }

    public static void serializeFully(ISerialization serialization, Message message)
    {
        serialization.writeObject(message.getSource());
        serialization.writeObject(message.getDestination());
        Assert.isTrue(message.getFiles() == null);
        
        serialize(serialization, message);
    }
    
    public static Message deserializeFully(IDeserialization deserialization)
    {
        IAddress source = deserialization.readObject();
        IAddress destination = deserialization.readObject();
        
        return deserialize(deserialization, source, destination, null);
    }
    
    private MessageSerializers()
    {
    }
}
