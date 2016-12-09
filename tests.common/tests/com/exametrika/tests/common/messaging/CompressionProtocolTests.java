/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.message.Message;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.messaging.impl.protocols.compression.CompressionMessagePart;
import com.exametrika.common.messaging.impl.protocols.compression.CompressionProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link CompressionProtocolTests} are tests for {@link CompressionProtocol}.
 * 
 * @see CompressionProtocol
 * @author Medvedev-A
 */
public class CompressionProtocolTests
{
    private SenderMock sender;
    private ReceiverMock receiver;
    private CompressionProtocol protocol;
    private IMessageFactory messageFactory;
    private SerializationRegistry registry;
    private IAddress node;
    private IAddress node2;
    private IAddress localNode;

    @Before
    public void setUp() throws Throwable
    {
        sender = new SenderMock();
        receiver = new ReceiverMock();
        node = new TestAddress(UUID.randomUUID(), "node");
        node2 = new TestAddress(UUID.randomUUID(), "node2");
        localNode = new TestAddress(UUID.randomUUID(), "local");
        
        
        LiveNodeManager manager = new LiveNodeManager("test", Arrays.<IFailureObserver>asList(new ChannelObserver("test")), new ChannelObserver("test"));
        manager.setLocalNode(localNode);
        
        registry = new SerializationRegistry();

        messageFactory = new MessageFactory(registry, manager);
        
        protocol = new CompressionProtocol("test", 5, messageFactory, registry);
        protocol.setSender(sender);
        protocol.setReceiver(receiver);
        protocol.setPullableSender(new PullableSenderMock());
        protocol.setTimeService(new SystemTimeService());

        registry.register(protocol);
        registry.register(new ByteArrayPartSerializer());

        protocol.start();
    }

    @Test
    public void testProtocol() throws Exception
    {
        byte[] buffer = new byte[10000];
        for (int i = 0; i < buffer.length; i++)
            buffer[i] = (byte)i;
        ByteArray value = new ByteArray(buffer, 0, buffer.length);
        protocol.send(messageFactory.create(node2, new ByteArrayPart(value), MessageFlags.NO_COMPRESS));
        protocol.send(messageFactory.create(node, new ByteArrayPart(value), 0));
        assertThat(sender.messages.size(), is(2));
        assertThat(sender.messages.get(0).getPart() instanceof CompressionMessagePart, is(false));
        assertThat(sender.messages.get(1).getPart() instanceof CompressionMessagePart, is(true));
        
        protocol.receive(sender.messages.get(0));
        protocol.receive(sender.messages.get(1));
        assertThat(receiver.messages.size(), is(2));
        assertThat(receiver.messages.get(0).getDestination(), is(node2));
        assertThat(receiver.messages.get(0).getSource(), is(localNode));
        assertThat(receiver.messages.get(0).getFlags(), is(MessageFlags.NO_COMPRESS));
        assertThat(((ByteArrayPart)receiver.messages.get(0).getPart()).value, is(value));
        assertThat(receiver.messages.get(1).getDestination(), is(node));
        assertThat(receiver.messages.get(1).getSource(), is(localNode));
        assertThat(receiver.messages.get(1).getFlags(), is(0));
        assertThat(((ByteArrayPart)receiver.messages.get(1).getPart()).value, is(value));
        sender.messages.clear();
        receiver.messages.clear();
        
        protocol.send(messageFactory.create(node2, new ByteArrayPart(new ByteArray(buffer, 0, buffer.length)), 0));
        
        assertThat(sender.messages.size(), is(1));
        assertThat(sender.messages.get(0).getPart() instanceof CompressionMessagePart, is(true));
        
        ByteOutputStream outStream = new ByteOutputStream();
        Serialization serialization = new Serialization(registry, true, outStream);
        
        MessageSerializers.serialize(serialization, (Message)sender.messages.get(0));

        ByteInputStream inStream = new ByteInputStream(outStream.getBuffer(), 0, outStream.getLength());
        Deserialization deserialization = new Deserialization(registry, inStream);
        
        IMessage message2 = MessageSerializers.deserialize(deserialization, localNode, node2, null); 

        protocol.receive(message2);
        
        assertThat(receiver.messages.size(), is(1));
        assertThat(receiver.messages.get(0).getDestination(), is(node2));
        assertThat(receiver.messages.get(0).getSource(), is(localNode));
        assertThat(receiver.messages.get(0).getFlags(), is(0));
        assertThat(((ByteArrayPart)receiver.messages.get(0).getPart()).value, is(value));
    }
    
    private static class ByteArrayPart implements IMessagePart
    {
        private final ByteArray value;
        
        public ByteArrayPart(ByteArray value)
        {
            this.value = value;
        }
        
        @Override
        public int getSize()
        {
            return value.getLength();
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (!(o instanceof ByteArrayPart))
                return true;
            
            return value.equals(((ByteArrayPart)o).value);
        }
        
        @Override
        public int hashCode()
        {
            return value.hashCode();
        }
    }

    public static class ByteArrayPartSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("568b5ef9-cd25-4c33-9bd0-33e39b82207a");
     
        public ByteArrayPartSerializer()
        {
            super(ID, ByteArrayPart.class);
        }

        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            ByteArrayPart part = (ByteArrayPart)object;

            serialization.writeByteArray(part.value);
        }
        
        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            ByteArray value = deserialization.readByteArray();
            
            return new ByteArrayPart(value);
        }
    }
}
