/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.IStreamReceiveHandler;
import com.exametrika.common.messaging.IStreamSendHandler;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.message.Message;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.protocols.streaming.StreamingMessagePart;
import com.exametrika.common.messaging.impl.protocols.streaming.StreamingProtocol;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Pair;
import com.exametrika.tests.common.messaging.ClientServerChannelTests.TestMessagePart;
import com.exametrika.tests.common.time.TimeServiceMock;

/**
 * The {@link StreamingProtocolTests} are tests for {@link StreamingProtocol}.
 * 
 * @see StreamingProtocol
 * @author Medvedev-A
 */
public class StreamingProtocolTests
{
    private SenderMock sender;
    private ReceiverMock receiver;
    private SerializationRegistry registry;
    private StreamingProtocol protocol;
    private ChannelObserver channelObserver;
    private LiveNodeManager liveNodeManager;
    private IMessageFactory messageFactory;
    private IAddress member1;
    private IAddress member2;
    private IAddress member3;
    private IAddress member4;
    private TimeServiceMock timeService;
    private PullableSenderMock pullableSender;

    @Before
    public void setUp() throws Throwable
    {
        member1 = new TestAddress(UUID.randomUUID(), "member1");
        member2 = new TestAddress(UUID.randomUUID(), "member2");
        member3 = new TestAddress(UUID.randomUUID(), "member3");
        member4 = new TestAddress(UUID.randomUUID(), "member4");
        
        timeService = new TimeServiceMock();
        timeService.useSystemTime = false;
        receiver = new ReceiverMock();
        sender = new SenderMock();
        pullableSender = new PullableSenderMock();
        channelObserver = new ChannelObserver("test");
        liveNodeManager = new LiveNodeManager("test", Arrays.<IFailureObserver>asList(channelObserver), channelObserver);
        liveNodeManager.setLocalNode(member1);
        liveNodeManager.onNodesConnected(Collections.singleton(member2));
        liveNodeManager.onNodesConnected(Collections.singleton(member3));
        liveNodeManager.onNodesConnected(Collections.singleton(member4));
        
        registry = new SerializationRegistry();
        registry.register(new TestStreamMessagePartSerializer());

        messageFactory = new MessageFactory(registry, liveNodeManager);
        
        protocol = new StreamingProtocol("test", messageFactory, 101, true, true);
        protocol.setSender(sender);
        protocol.setReceiver(receiver);
        protocol.setPullableSender(pullableSender);
        protocol.setTimeService(timeService);
        registry.register(protocol);

        liveNodeManager.start();
        channelObserver.start();
        protocol.start();   
        
        Thread.sleep(100);
    }

    @Test
    public void testPushProtocol() throws Exception
    {
        Message plainMessage = new Message(member1, member2, 0, registry);
        protocol.send(plainMessage);
        assertThat(sender.messages.size() == 1, is(true));
        assertThat(sender.messages.get(0).getPart(), nullValue());
        sender.messages.clear();
        
        protocol.receive(plainMessage);
        assertThat(receiver.messages.size() == 1, is(true));
        assertThat(receiver.messages.get(0).getPart(), nullValue());
        receiver.messages.clear();
        
        TestStreamSendMessagePart part1 = new TestStreamSendMessagePart("message1", Collections.<ByteArray>emptyList());
        plainMessage = new Message(member1, member2, part1, 
            MessageFlags.NO_COMPRESS, null, registry);
        protocol.send(plainMessage);
        assertThat(sender.messages.size() == 1, is(true));
        sender.messages.clear();
        
        protocol.receive(plainMessage);
        assertThat(receiver.messages.size() == 1, is(true));
        receiver.messages.clear();
        
        part1 = new TestStreamSendMessagePart("message1", createBuffers(1, 10));
        Message smallMessage = new Message(member1, member2, part1, 
            MessageFlags.NO_COMPRESS, null, registry);
        protocol.send(smallMessage);
        assertThat(sender.messages.size() == 1, is(true));
        StreamingMessagePart streamingPart1 = sender.messages.get(0).getPart();
        assertThat(streamingPart1.isFirst(), is(true));
        assertThat(streamingPart1.isLast(), is(true));
        protocol.receive(transmit(sender.messages.get(0)));
        assertThat(receiver.messages.size() == 1, is(true));
        assertThat(receiver.messages.get(0).getSource(), is(member1));
        assertThat(receiver.messages.get(0).getDestination(), is(member2));
        assertThat(receiver.messages.get(0).getFlags(), is(MessageFlags.NO_COMPRESS | MessageFlags.NO_DELAY));
        TestStreamReceiveMessagePart receivedPart1 = receiver.messages.get(0).getPart();
        assertThat(receivedPart1.value, is("message1"));
        assertThat(receivedPart1.data, is(createBuffers(1, 10)));
        sender.messages.clear();
        receiver.messages.clear();
        assertThat(part1.sendStarted, is(true));
        assertThat(part1.sendCompleted, is(true));
        assertThat(receivedPart1.receiveCompleted, is(true));
        
        int[][] params = {{1, 10000}, {10, 10}, {10, 10000}};
        for (int k = 0; k < params.length; k++)
        {
            TestStreamSendMessagePart part2 = new TestStreamSendMessagePart("message2", createBuffers(params[k][0], params[k][1]));
            Message largeMessage = new Message(member1, member2, part2, MessageFlags.NO_COMPRESS, null, registry);
            
            protocol.send(largeMessage);
            assertThat(part2.sendCompleted, is(true));
            assertThat(sender.messages.size() > 1, is(true));
            
            assertThat(sender.messages.size() > 1, is(true));
            for (IMessage fragment : sender.messages)
                protocol.receive(transmit(fragment));
            
            assertThat(receiver.messages.size(), is(1));
            assertThat(receiver.messages.get(0).getSource(), is(member1));
            assertThat(receiver.messages.get(0).getDestination(), is(member2));
            assertThat(receiver.messages.get(0).getFlags(), is(MessageFlags.NO_COMPRESS | MessageFlags.NO_DELAY));
            TestStreamReceiveMessagePart receivedPart2 = receiver.messages.get(0).getPart();
            assertThat(part2.value, is("message2"));
            assertThat(receivedPart2.data, is(createBuffers(params[k][0], params[k][1])));
            assertThat(receivedPart2.receiveStarted, is(true));
            assertThat(receivedPart2.receiveCompleted, is(true));
            sender.messages.clear();
            receiver.messages.clear();
        }
    }

    @Test
    public void testPullProtocol() throws Exception
    {
        final Message plainMessage = new Message(member1, member2, 0, registry);
        
        protocol.register(member2, new IFeed()
        {
            @Override
            public void feed(ISink sink)
            {
                sink.setReady(true);
                assertThat(sink.send(plainMessage), is(true));
            }
        });
        
        pullableSender.send(member2);
        assertThat(pullableSender.messages.size() == 1, is(true));
        assertThat(pullableSender.messages.get(0).getValue().getPart(), nullValue());
        pullableSender.messages.clear();
        
        protocol.receive(plainMessage);
        assertThat(receiver.messages.size() == 1, is(true));
        assertThat(receiver.messages.get(0).getPart(), nullValue());
        receiver.messages.clear();
        
        final Message plainMessage2 = new Message(member1, member2, new TestStreamSendMessagePart("message1", 
            Collections.<ByteArray>emptyList()), 0, null, registry);
        
        protocol.register(member2, new IFeed()
        {
            @Override
            public void feed(ISink sink)
            {
                sink.setReady(true);
                assertThat(sink.send(plainMessage2), is(true));
            }
        });
        
        pullableSender.send(member2);
        assertThat(pullableSender.messages.size() == 1, is(true));
        pullableSender.messages.clear();
        
        protocol.receive(plainMessage2);
        assertThat(receiver.messages.size() == 1, is(true));
        receiver.messages.clear();
        
        TestStreamSendMessagePart part1 = new TestStreamSendMessagePart("message1", createBuffers(1, 10));
        final Message smallMessage = new Message(member1, member2, part1, 
            MessageFlags.NO_COMPRESS, null, registry);
        
        protocol.register(member2, new IFeed()
        {
            @Override
            public void feed(ISink sink)
            {
                sink.setReady(true);
                assertThat(sink.send(smallMessage), is(true));
            }
        });
        
        pullableSender.send(member2);
        assertThat(pullableSender.messages.size() == 1, is(true));
        StreamingMessagePart streamingPart1 = pullableSender.messages.get(0).getValue().getPart();
        assertThat(streamingPart1.isFirst(), is(true));
        assertThat(streamingPart1.isLast(), is(true));
        protocol.receive(transmit(pullableSender.messages.get(0).getValue()));
        assertThat(receiver.messages.size() == 1, is(true));
        assertThat(receiver.messages.get(0).getSource(), is(member1));
        assertThat(receiver.messages.get(0).getDestination(), is(member2));
        assertThat(receiver.messages.get(0).getFlags(), is(MessageFlags.NO_COMPRESS | MessageFlags.NO_DELAY));
        TestStreamReceiveMessagePart receivedPart1 = receiver.messages.get(0).getPart();
        assertThat(receivedPart1.value, is("message1"));
        assertThat(receivedPart1.data, is(createBuffers(1, 10)));
        pullableSender.messages.clear();
        receiver.messages.clear();
        assertThat(part1.sendCompleted, is(true));
        assertThat(receivedPart1.receiveCompleted, is(true));
        
        int[][] params = {{1, 1000}, {10, 10}, {10, 1000}};
        for (int k = 0; k < params.length; k++)
        {
            TestStreamSendMessagePart part2 = new TestStreamSendMessagePart("message2", createBuffers(params[k][0], params[k][1]));
            final Message largeMessage = new Message(member1, member2, part2, MessageFlags.NO_COMPRESS, null, registry);
            
            pullableSender.blockCount = 5;
            protocol.register(member2, new IFeed()
            {
                @Override
                public void feed(ISink sink)
                {
                    sink.setReady(true);
                    assertThat(sink.send(largeMessage), is(false));
                    sink.setReady(false);
                }
            });
            
            pullableSender.send(member2);
            assertThat(part2.sendCompleted, is(false));
            assertThat(pullableSender.messages.size() > 1, is(true));
            
            assertThat(pullableSender.messages.size() > 1, is(true));
            for (Pair<IAddress, IMessage> pair : pullableSender.messages)
            {
                IMessage fragment = pair.getValue();
                protocol.receive(transmit(fragment));
            }
            pullableSender.messages.clear();
            assertThat(receiver.messages.isEmpty(), is(true));
            pullableSender.blockCount = 0;
            
            pullableSender.send(member2);
            assertThat(part2.sendCompleted, is(true));
            assertThat(pullableSender.messages.size() > 1, is(true));
            
            assertThat(pullableSender.messages.size() > 1, is(true));
            for (Pair<IAddress, IMessage> pair : pullableSender.messages)
            {
                IMessage fragment = pair.getValue();
                protocol.receive(transmit(fragment));
            }

            assertThat(receiver.messages.size(), is(1));
            assertThat(receiver.messages.get(0).getSource(), is(member1));
            assertThat(receiver.messages.get(0).getDestination(), is(member2));
            assertThat(receiver.messages.get(0).getFlags(), is(MessageFlags.NO_COMPRESS | MessageFlags.NO_DELAY));
            TestStreamReceiveMessagePart receivedPart2 = receiver.messages.get(0).getPart();
            assertThat(part2.value, is("message2"));
            assertThat(receivedPart2.data, is(createBuffers(params[k][0], params[k][1])));
            assertThat(receivedPart2.receiveCompleted, is(true));
            pullableSender.messages.clear();
            receiver.messages.clear();
        }
    }

    @Test
    public void testCleanup() throws Exception
    {
        timeService.time = 1000;
        TestStreamSendMessagePart part2 = new TestStreamSendMessagePart("message2", createBuffers(1, 1000));
        final Message largeMessage = new Message(member1, member2, part2, MessageFlags.NO_COMPRESS, null, registry);
        
        pullableSender.blockCount = 5;
        protocol.register(member2, new IFeed()
        {
            @Override
            public void feed(ISink sink)
            {
                sink.setReady(true);
                assertThat(sink.send(largeMessage), is(false));
                sink.setReady(false);
            }
        });
        
        pullableSender.send(member2);
        assertThat(part2.sendCompleted, is(false));
        assertThat(pullableSender.messages.size() > 1, is(true));
        
        assertThat(pullableSender.messages.size() > 1, is(true));
        int i = 0;
        for (Pair<IAddress, IMessage> pair : pullableSender.messages)
        {
            IMessage fragment = pair.getValue();
            StreamingMessagePart part = fragment.getPart();
            assertThat(part.isFirst(), is(i == 0));
            
            ByteOutputStream outStream = new ByteOutputStream();
            Serialization serialization = new Serialization(registry, true, outStream);
            
            MessageSerializers.serialize(serialization, (Message)fragment);

            ByteInputStream inStream = new ByteInputStream(outStream.getBuffer(), 0, outStream.getLength());
            Deserialization deserialization = new Deserialization(registry, inStream);
            
            IMessage fragment2 = MessageSerializers.deserialize(deserialization, member2, member1, null);

            protocol.receive(fragment2);
            i++;
        }

        Map outStreams = Tests.get(protocol, "outgoingStreams");
        Map inStreams = Tests.get(protocol, "incomingStreams");

        TestStreamSendMessagePart outHandler = ((IMessage)Tests.get(outStreams.values().iterator().next(), "message")).getPart();
        TestStreamReceiveMessagePart inHandler = ((IMessage)Tests.get(inStreams.values().iterator().next(), "message")).getPart();
        
        liveNodeManager.onNodesFailed(Collections.singleton(member2));
        
        Thread.sleep(100);
        protocol.cleanup(liveNodeManager, 1500);
        
        assertThat(outStreams.isEmpty(), is(true));
        assertThat(outHandler.sendCanceled, is(true));
        assertThat(inStreams.isEmpty(), is(true));
        assertThat(inHandler.receiveCanceled, is(true));
    }
    
    private IMessage transmit(IMessage message)
    {
        ByteOutputStream outStream = new ByteOutputStream();
        Serialization serialization = new Serialization(registry, true, outStream);
        
        MessageSerializers.serialize(serialization, (Message)message);
   
        ByteInputStream inStream = new ByteInputStream(outStream.getBuffer(), 0, outStream.getLength());
        Deserialization deserialization = new Deserialization(registry, inStream);
        
        IMessage fragment2 = MessageSerializers.deserialize(deserialization, member1, member2, null);
        return fragment2;
    }

    public static List<ByteArray> createBuffers(int count, int size)
    {
        List<ByteArray> buffers = new ArrayList<ByteArray>();
        for (int k = 0; k < count; k++)
        {
            byte[] buffer = new byte[size];
            for (int i = 0; i < buffer.length; i++)
                buffer[i] = (byte)(i + k);
            
            buffers.add(new ByteArray(buffer));
        }
        
        return buffers;
    }
    
    public static class TestStreamMessagePartSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("568b5ef9-cd25-4c33-9bd0-33e39b82207a");
     
        public TestStreamMessagePartSerializer()
        {
            super(ID, TestStreamSendMessagePart.class);
        }

        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            TestStreamSendMessagePart part = (TestStreamSendMessagePart)object;

            serialization.writeString(part.value);
        }
        
        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            String value = deserialization.readString();
            
            return new TestStreamReceiveMessagePart(value);
        }
    }
    
    public static class TestStreamSendMessagePart extends TestMessagePart implements IStreamSendHandler
    {
        public boolean sendStarted;
        public boolean sendCompleted;
        public boolean sendCanceled;
        public final List<ByteArray> data;
        public ByteInputStream in;
        
        public TestStreamSendMessagePart(String value, List<ByteArray> data)
        {
            super(value);
            this.data = data;
        }

        @Override
        public int getStreamCount()
        {
            return data.size();
        }

        @Override
        public boolean hasData()
        {
            return in.available() != 0;
        }
        
        @Override
        public int read(byte[] sendBuffer)
        {
            return in.read(sendBuffer);
        }

        @Override
        public void sendStreamStarted(int streamIndex)
        {
            ByteArray buffer = data.get(streamIndex);
            in = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
        }

        @Override
        public void sendStreamCompleted()
        {
            in = null;
        }

        @Override
        public void sendStarted()
        {
            sendStarted = true;
        }
        
        @Override
        public void sendCompleted()
        {
            sendCompleted = true;
        }

        @Override
        public void sendCanceled()
        {
            sendCanceled = true;
        }
    }
    
    public static class TestStreamReceiveMessagePart extends TestMessagePart implements IStreamReceiveHandler
    {
        public boolean receiveStarted;
        public boolean receiveCompleted;
        public boolean receiveCanceled;
        public List<ByteArray> data;
        public ByteOutputStream out;
        
        public TestStreamReceiveMessagePart(String value)
        {
            super(value);
        }
        
        @Override
        public void write(byte[] buffer, int offset, int length)
        {
            out.write(buffer, offset, length);
        }

        @Override
        public void receiveStreamStarted(int streamIndex)
        {
            out = new ByteOutputStream();
        }
        
        @Override
        public void receiveStreamCompleted()
        {
            data.add(new ByteArray(out.getBuffer(), 0, out.getLength()));
            out = null;
        }
        
        @Override
        public void receiveStarted(int streamCount)
        {
            receiveStarted = true;
            data = new ArrayList<ByteArray>();
        }
        
        @Override
        public void receiveCompleted()
        {
            receiveCompleted = true;
        }

        @Override
        public void receiveCanceled()
        {
            receiveCanceled = true;
        }
    }
}
