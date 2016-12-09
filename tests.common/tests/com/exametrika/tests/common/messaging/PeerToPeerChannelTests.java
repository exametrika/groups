/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelFactory.FactoryParameters;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.utils.Classes;
import com.exametrika.tests.common.messaging.StreamingProtocolTests.TestStreamReceiveMessagePart;
import com.exametrika.tests.common.messaging.StreamingProtocolTests.TestStreamSendMessagePart;
import com.exametrika.tests.common.net.TcpChannelTests;


/**
 * The {@link PeerToPeerChannelTests} are tests for peer-to-peer {@link Channel}.
 * 
 * @see Channel
 * @author Medvedev-A
 */
@RunWith(Parameterized.class)
public class PeerToPeerChannelTests
{
    private static final int COUNT = 10;
    private static final int CONNECT_TIMEOUT = 60000;
    private static final int SEND_TIMEOUT = 600000;
    private IChannel[] peers = new IChannel[COUNT];
    private PeerReceiver[] receivers = new PeerReceiver[COUNT];
    private TestFeed[] feeds = new TestFeed[COUNT];
    private LiveNodeManagerTests.TestChannelListener[] listeners = new LiveNodeManagerTests.TestChannelListener[COUNT];
    private final boolean secured;
    private final boolean multithreaded;
    private final Sequencer connectionSequencer = new Sequencer();
    private final Sequencer receiveSequencer = new Sequencer();
    
    @Parameters
    public static Collection<?> parameters()
    {
        return Arrays.asList(new Object[]{true, true}, new Object[]{false, true}, new Object[]{true, false}, new Object[]{false, false});
    }
    
    public PeerToPeerChannelTests(boolean secured, boolean multithreaded)
    {
        this.secured = secured;
        this.multithreaded = multithreaded;
    }
    
    @Before
    public void setUp() throws Throwable
    {
        ChannelFactory factory = new ChannelFactory(new FactoryParameters());
        ChannelFactory.Parameters parameters = new ChannelFactory.Parameters();
        
        parameters.channelName = "server";
        parameters.secured = secured;
        parameters.keyStorePassword = "testtest";
        parameters.keyStorePath = "classpath:" + Classes.getResourcePath(TcpChannelTests.class) + "/keystore.jks";
        parameters.serializationRegistrars.add(new StreamingProtocolTests.TestStreamMessagePartSerializer());
        parameters.serializationRegistrars.add(new TestMessagePartSerializer());
        parameters.serverPart = true;
        parameters.clientPart = true;
        parameters.multiThreaded = multithreaded;
        
        for (int i = 0; i < COUNT; i++)
        {
            receivers[i] = new PeerReceiver(i, 20);
            feeds[i] = new TestFeed();
            parameters.receiver = receivers[i];
            parameters.channelName = "client" + i;
            peers[i] = factory.createChannel(parameters);
            listeners[i] = new LiveNodeManagerTests.TestChannelListener(connectionSequencer);
            peers[i].getChannelObserver().addChannelListener(listeners[i]);
            peers[i].start();
        }
        
        for (int i = 0; i < COUNT; i++)
        {
            int next = (i < COUNT - 1) ? (i + 1) : 0; 
            feeds[i].sink = peers[i].register(peers[next].getLiveNodeProvider().getLocalNode(), feeds[i]);
            peers[i].connect(peers[next].getLiveNodeProvider().getLocalNode().getConnection());
        }
        
        connectionSequencer.waitAll(COUNT * 2, CONNECT_TIMEOUT, 0, "Connection.");
        
        for (int i = 0; i < COUNT; i++)
        {
            int next = (i < COUNT - 1) ? (i + 1) : 0;
            assertThat(peers[i].getLiveNodeProvider().isLive(peers[next].getLiveNodeProvider().getLocalNode()), is(true));
            assertThat(peers[next].getLiveNodeProvider().isLive(peers[i].getLiveNodeProvider().getLocalNode()), is(true));
            assertThat(listeners[next].connected.contains(peers[i].getLiveNodeProvider().getLocalNode()), is(true));
            assertThat(listeners[i].connected.contains(peers[next].getLiveNodeProvider().getLocalNode()), is(true));
        }
    }
    
    @After
    public void tearDown() throws Throwable
    {
        for (int i = 0; i < COUNT; i++)
        {
            if (peers[i] != null)
                peers[i].stop();
        }
    }
    
    @Test
    public void testPushSend() throws Throwable
    {
        for (int i = 0; i < COUNT; i++)
        {
            int next = (i < COUNT - 1) ? (i + 1) : 0;
            
            for (int k = 0; k < 10; k++)            
            {
                TestMessagePart part = new TestMessagePart("request" + k);
                IMessage request = peers[i].getMessageFactory().create(peers[next].getLiveNodeProvider().getLocalNode(), part);
                peers[i].send(request);
            }
        }
        
        receiveSequencer.waitAll(COUNT, SEND_TIMEOUT, 0, "Receive messages.");
        
        for (int i = 0; i < COUNT; i++)
        {
            int next = (i < COUNT - 1) ? (i + 1) : 0;
            assertThat(receivers[i].messages.size(), is(10));
            
            for (int k = 0; k < 10; k++)
            {
                IMessage m = receivers[i].messages.get(k);
                assertThat(m.getDestination(), is(peers[i].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getSource(), is(peers[next].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getFlags(), is(0));
                TestMessagePart part = m.getPart();
                assertThat(part, is(new TestMessagePart("response" + k)));
            }
        }
    }
    
    @Test
    public void testPullSend() throws Throwable
    {
        for (int i = 0; i < COUNT; i++)
        {
            List<IMessage> messages = new ArrayList<IMessage>();
            
            for (int k = 0; k < 10; k++)            
            {
                int next = (i < COUNT - 1) ? (i + 1) : 0;
                TestMessagePart part = new TestMessagePart("request" + k);
                messages.add(peers[i].getMessageFactory().create(peers[next].getLiveNodeProvider().getLocalNode(), part));
            }
            
            feeds[i].setMessages(messages.iterator());
        }
        
        receiveSequencer.waitAll(COUNT, SEND_TIMEOUT, 0, "Receive messages.");
        
        for (int i = 0; i < COUNT; i++)
        {
            int next = (i < COUNT - 1) ? (i + 1) : 0;
            assertThat(receivers[i].messages.size(), is(10));
            
            for (int k = 0; k < 10; k++)
            {
                IMessage m = receivers[i].messages.get(k);
                assertThat(m.getDestination(), is(peers[i].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getSource(), is(peers[next].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getFlags(), is(0));
                TestMessagePart part = m.getPart();
                assertThat(part, is(new TestMessagePart("response" + k)));
            }
        }
    }
    
    @Test
    public void testStreamPushSend() throws Throwable
    {
        for (int i = 0; i < COUNT; i++)
        {
            int next = (i < COUNT - 1) ? (i + 1) : 0;
            
            for (int k = 0; k < 10; k++)            
            {
                TestStreamSendMessagePart part = new TestStreamSendMessagePart("request" + k, StreamingProtocolTests.createBuffers(1, 100000));
                
                IMessage request = peers[i].getMessageFactory().create(peers[next].getLiveNodeProvider().getLocalNode(), 
                    part, MessageFlags.NO_COMPRESS);
                peers[i].send(request);
                
                Thread.sleep(100);
                
                assertThat(part.sendCompleted, is(true));
            }
        }
        
        receiveSequencer.waitAll(COUNT, SEND_TIMEOUT, 0, "Receive messages.");
        
        for (int i = 0; i < COUNT; i++)
        {
            int next = (i < COUNT - 1) ? (i + 1) : 0;
            
            assertThat(receivers[i].messages.size(), is(10));
            
            for (int k = 0; k < 10; k++)
            {
                IMessage m = receivers[i].messages.get(k);
                assertThat(m.getDestination(), is(peers[i].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getSource(), is(peers[next].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getFlags(), is(MessageFlags.NO_COMPRESS | MessageFlags.NO_DELAY));
                TestStreamReceiveMessagePart part = m.getPart();
                assertThat(part, is(new TestStreamReceiveMessagePart("response" + k)));
                assertThat(part.data, is(StreamingProtocolTests.createBuffers(1, 100000)));
                assertThat(part.receiveCompleted, is(true));
            }
        }
    }
    
    @Test
    public void testStreamPullSend() throws Throwable
    {
        for (int i = 0; i < COUNT; i++)
        {
            int next = (i < COUNT - 1) ? (i + 1) : 0;
            List<IMessage> messages = new ArrayList<IMessage>();
            
            for (int k = 0; k < 10; k++)            
            {
                TestStreamSendMessagePart part = new TestStreamSendMessagePart("request" + k, StreamingProtocolTests.createBuffers(1, 100000));
                messages.add(peers[i].getMessageFactory().create(peers[next].getLiveNodeProvider().getLocalNode(), 
                    part, MessageFlags.NO_COMPRESS));
            }
            
            feeds[i].setMessages(messages.iterator());
        }

        receiveSequencer.waitAll(COUNT, SEND_TIMEOUT, 0, "Receive messages."); 
        
        for (int i = 0; i < COUNT; i++)
        {
            int next = (i < COUNT - 1) ? (i + 1) : 0;
            assertThat(receivers[i].messages.size(), is(10));
            
            for (int k = 0; k < 10; k++)
            {
                IMessage m = receivers[i].messages.get(k);
                assertThat(m.getDestination(), is(peers[i].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getSource(), is(peers[next].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getFlags(), is(MessageFlags.NO_COMPRESS | MessageFlags.NO_DELAY));
                TestStreamReceiveMessagePart part = m.getPart();
                assertThat(part, is(new TestStreamReceiveMessagePart("response" + k)));
                assertThat(part.data, is(StreamingProtocolTests.createBuffers(1, 100000)));
                assertThat(part.receiveCompleted, is(true));
            }
        }
    }

    private class PeerReceiver implements IReceiver
    {
        public List<IMessage> messages = new ArrayList<IMessage>();
        private int channel;
        public int index;
        public final int count;
        
        public PeerReceiver(int channel, int count)
        {
            this.channel = channel;
            this.count = count;
        }
        
        @Override
        public synchronized void receive(IMessage message)
        {
            boolean request = false;
            int prev = (channel > 0) ? (channel - 1) : (COUNT - 1);
            if (message.getSource().equals(peers[prev].getLiveNodeProvider().getLocalNode()))
            {
                IMessagePart part = message.getPart();
                if (part instanceof StreamingProtocolTests.TestStreamReceiveMessagePart)
                {
                    TestStreamReceiveMessagePart test = (TestStreamReceiveMessagePart)part;
                    if (test.value.startsWith("request"))
                    {
                        part = new TestStreamSendMessagePart("response" + test.value.substring(7), test.data);
                        request = true;
                    }
                }
                if (part instanceof TestMessagePart)
                {
                    TestMessagePart test = (TestMessagePart)part;
                    if (test.value.startsWith("request"))
                    {
                        part = new TestMessagePart("response" + test.value.substring(7));
                        request = true;
                    }
                }
                
                if (request)
                {
                    IMessage reply = peers[channel].getMessageFactory().create(message.getSource(), part, message.getFlags());
                    peers[channel].send(reply);
                }
            }
            if (!request)
                messages.add(message);
            
            index++;
            
            if (index == count)
                receiveSequencer.allowSingle("Received " + message.getSource());
        }
    }
    
    public static class TestFeed implements IFeed
    {
        private volatile Iterator<IMessage> messages;
        public ISink sink;
        
        public void setMessages(Iterator<IMessage> messages)
        {
            this.messages = messages;
            sink.setReady(true);
        }
        
        @Override
        public void feed(ISink sink)
        {
            if (messages == null)
            {
                sink.setReady(false);
                return;
            }
            
            while (messages.hasNext())
            {
                if (!sink.send(messages.next()))
                    break;
            }
            
            if (!messages.hasNext())
                sink.setReady(false);
        }
    }
    
    public static class TestMessagePartSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("405ea4d2-f79c-4560-8aa6-59a2151c7def");
     
        public TestMessagePartSerializer()
        {
            super(ID, TestMessagePart.class);
        }

        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            TestMessagePart part = (TestMessagePart)object;

            serialization.writeString(part.value);
        }
        
        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            String value = deserialization.readString();
            
            return new TestMessagePart(value);
        }
    }
    
    public static class TestMessagePart implements IMessagePart
    {
        public final String value;
        
        public TestMessagePart(String value)
        {
            this.value = value;
        }
        
        @Override
        public int getSize()
        {
            return value.length() * 2;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            if (!(o instanceof TestMessagePart))
                return true;
            
            return value.equals(((TestMessagePart)o).value);
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
}
