/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.message.Message;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.utils.IVisitor;


/**
 * The {@link MessageTests} are tests for {@link Message}.
 * 
 * @see Message
 * @author Medvedev-A
 */
public class MessageTests
{
    @Test
    public void testMessage()
    {
        IAddress destination = new TestAddress(UUID.randomUUID(), "destination");
        IAddress source = new TestAddress(UUID.randomUUID(), "source");
        
        Message message = new Message(source, destination, 0, new SerializationRegistry());
        assertThat(message.getDestination(), is(destination));
        assertThat(message.getSource(), is(source));
        assertThat(message.getFlags(), is(0));
        assertThat(message.getPartCount(), is(0));
        assertThat(message.getPart(), nullValue());
        
        message = message.addFlags(MessageFlags.NO_COMPRESS);
        assertThat(message.getDestination(), is(destination));
        assertThat(message.getSource(), is(source));
        assertThat(message.getFlags(), is(MessageFlags.NO_COMPRESS));
        assertThat(message.getPartCount(), is(0));
        assertThat(message.getPart(), nullValue());
        
        message = message.addPart(new Part1("test2"));
        assertThat(message.getDestination(), is(destination));
        assertThat(message.getSource(), is(source));
        assertThat(message.getFlags(), is(MessageFlags.NO_COMPRESS));
        assertThat(message.getPartCount(), is(1));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test2")));
        
        message = message.addPart(new Part1("test3"), MessageFlags.NO_COMPRESS);
        assertThat(message.getDestination(), is(destination));
        assertThat(message.getSource(), is(source));
        assertThat(message.getFlags(), is(MessageFlags.NO_COMPRESS));
        assertThat(message.getPartCount(), is(2));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test3")));
        
        message = new Message(source, destination, new Part1("test"), 0, null, new SerializationRegistry());
        assertThat(message.getDestination(), is(destination));
        assertThat(message.getSource(), is(source));
        assertThat(message.getFlags(), is(0));
        assertThat(message.getPartCount(), is(1));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test")));
        
        message = message.addPart(new Part1("test2"), MessageFlags.NO_COMPRESS);
        assertThat(message.getDestination(), is(destination));
        assertThat(message.getSource(), is(source));
        assertThat(message.getFlags(), is(MessageFlags.NO_COMPRESS));
        assertThat(message.getPartCount(), is(2));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test2")));
        
        message = message.removeFlags(MessageFlags.NO_COMPRESS);
        assertThat(message.getDestination(), is(destination));
        assertThat(message.getSource(), is(source));
        assertThat(message.getFlags(), is(0));
        assertThat(message.getPartCount(), is(2));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test2")));
        
        Visitor visitor = new Visitor();
        message.visitParts(visitor);
        assertThat(visitor.parts, is((List)Arrays.asList(new Part1("test2"), new Part1("test"))));
        
        message = message.removePart();
        assertThat(message.getDestination(), is(destination));
        assertThat(message.getSource(), is(source));
        assertThat(message.getFlags(), is(0));
        assertThat(message.getPartCount(), is(1));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test")));
        
        message = message.removePart(MessageFlags.NO_COMPRESS);
        assertThat(message.getDestination(), is(destination));
        assertThat(message.getSource(), is(source));
        assertThat(message.getFlags(), is(0));
        assertThat(message.getPartCount(), is(0));
        assertThat(message.getPart(), nullValue());
        
        message = message.removePart(0);
        assertThat(message, nullValue());
        
        message = new Message(source, destination, MessageFlags.NO_COMPRESS | MessageFlags.PARALLEL | MessageFlags.HIGH_PRIORITY, 
            new SerializationRegistry());
        assertThat(message.hasFlags(MessageFlags.NO_COMPRESS | MessageFlags.PARALLEL), is(true));
        assertThat(message.hasFlags(MessageFlags.NO_COMPRESS | MessageFlags.LOW_PRIORITY), is(false));
        assertThat(message.hasOneOfFlags(MessageFlags.NO_COMPRESS | MessageFlags.LOW_PRIORITY), is(true));
        assertThat(message.hasOneOfFlags(MessageFlags.LOW_PRIORITY), is(false));
    }
    
    @Test
    public void testSerializer() throws Throwable
    {
        final IAddress source = new TestAddress(UUID.randomUUID(), "source");
        final IAddress destination = new TestAddress(UUID.randomUUID(), "destination");
        
        final ISerializationRegistry registry = new SerializationRegistry();
        registry.register(new Part1Serializer());
        
        ByteOutputStream outStream = new ByteOutputStream();
        Serialization serialization = new Serialization(registry, true, outStream);
        
        Message message1 = new Message(source, destination, new Part1("test"), 
            MessageFlags.NO_COMPRESS, null, registry);
        message1 = message1.addPart(new Part1("test2"));
        MessageSerializers.serialize(serialization, message1);
        
        Message message5 = new Message(source, destination, 0, registry);
        message5 = message5.addPart(new Part1("test1"), true);
        message5 = message5.addPart(new Part1("test2"), true);
        message5 = message5.addPart(new Part1("test3"));
        message5 = message5.addPart(new Part1("test4"), MessageFlags.NO_COMPRESS, true);
        MessageSerializers.serialize(serialization, message5);
        
        ByteInputStream inStream = new ByteInputStream(outStream.getBuffer(), 0, outStream.getLength());
        Deserialization deserialization = new Deserialization(registry, inStream);
        
        Message message = MessageSerializers.deserialize(deserialization, source, destination, null);
        assertThat(message.getDestination() == destination, is(true));
        assertThat(message.getSource() == source, is(true));
        assertThat(message.getFlags(), is(MessageFlags.NO_COMPRESS));
        assertThat(message.getPartCount(), is(2));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test2")));
        assertThat(message.getSize() >= ("test".length() + "test2".length()), is(true));
        
        message = message.removePart();
        assertThat(message.getPartCount(), is(1));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test")));
        assertThat(message.getSize() > "test".length(), is(true));
        
        message = MessageSerializers.deserialize(deserialization, source, destination, null);
        assertThat(message.getDestination() == destination, is(true));
        assertThat(message.getSource() == source, is(true));
        assertThat(message.getFlags(), is(MessageFlags.NO_COMPRESS));
        assertThat(message.getPartCount(), is(4));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test4")));
        assertThat(message.getSize() >= ("test1".length() + "test2".length() + "test3".length() +
                "test4".length()), is(true));
        
        message = message.removePart();
        assertThat(message.getPartCount(), is(3));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test3")));
        assertThat(message.getSize() >= "test1".length() + "test2".length() + "test3".length(), is(true));
        
        message = message.removePart();
        assertThat(message.getPartCount(), is(2));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test2")));
        assertThat(message.getSize() >= "test1".length() + "test2".length(), is(true));
        
        message = message.removePart();
        assertThat(message.getPartCount(), is(1));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test1")));
        assertThat(message.getSize() >= "test1".length(), is(true));
        
        TcpPacket packet = MessageSerializers.serialize(registry, message1, 10, null);
        message = MessageSerializers.deserialize(registry, source, destination, packet, 10);
        
        assertThat(message.getDestination() == destination, is(true));
        assertThat(message.getSource() == source, is(true));
        assertThat(message.getFlags(), is(MessageFlags.NO_COMPRESS));
        assertThat(message.getPartCount(), is(2));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test2")));
        assertThat(message.getSize() >= ("test".length() + "test2".length()), is(true));
        
        message = message.removePart();
        assertThat(message.getPartCount(), is(1));
        assertThat(message.getPart(), is((IMessagePart)new Part1("test")));
        assertThat(message.getSize() > "test".length(), is(true));
    }
    
    private static class Part1 implements IMessagePart
    {
        private final String value;
        
        public Part1(String value)
        {
            this.value = value;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (!(o instanceof Part1))
                return true;
            
            return value.equals(((Part1)o).value);
        }
        
        @Override
        public int getSize()
        {
            return value.length() * 2;
        }
        
        @Override
        public int hashCode()
        {
            return value.hashCode();
        }

        @Override
        public String toString()
        {
            return value;
        }
    }
    
    private static class Part1Serializer extends AbstractSerializer
    {
        private static final UUID ID = UUID.fromString("390360c1-51bd-42bf-8c07-90567ad9e57d");
     
        public Part1Serializer()
        {
            super(ID, Part1.class);
        }

        @Override
        public void serialize(final ISerialization serialization, Object object)
        {
            Part1 part = (Part1)object;
            serialization.writeString(part.value);
        }
        
        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            String value = deserialization.readString();
            return new Part1(value);
        }
    }
    
    private static class Visitor implements IVisitor<IMessagePart>
    {
        List<IMessagePart> parts =new ArrayList<IMessagePart>();
        
        @Override
        public boolean visit(IMessagePart element)
        {
            parts.add(element);
            return true;
        }
    }
}
