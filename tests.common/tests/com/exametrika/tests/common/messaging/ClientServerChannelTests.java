/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.utils.Classes;
import com.exametrika.tests.common.messaging.StreamingProtocolTests.TestStreamReceiveMessagePart;
import com.exametrika.tests.common.messaging.StreamingProtocolTests.TestStreamSendMessagePart;
import com.exametrika.tests.common.net.TcpChannelTests;


/**
 * The {@link ClientServerChannelTests} are tests for client-server {@link Channel}.
 * 
 * @see Channel
 * @author Medvedev-A
 */
@RunWith(Parameterized.class)
public class ClientServerChannelTests
{
    private static final int COUNT = 10;
    private static final int CONNECT_TIMEOUT = 60000;
    private static final int SEND_TIMEOUT = 600000;
    private IChannel[] clients = new IChannel[COUNT];
    private ReceiverMock[] receivers = new ReceiverMock[COUNT];
    private TestFeed[] feeds = new TestFeed[COUNT];
    private ServerReceiver serverReceiver = new ServerReceiver(COUNT * 10);
    private final Sequencer connectionSequencer = new Sequencer();
    private final Sequencer receiveSequencer = new Sequencer();
    private LiveNodeManagerTests.TestChannelListener[] clientListeners = new LiveNodeManagerTests.TestChannelListener[COUNT];
    private LiveNodeManagerTests.TestChannelListener serverListener = new LiveNodeManagerTests.TestChannelListener(connectionSequencer);
    private IChannel server;
    private final boolean secured;
    private final boolean multithreaded;
    
    @Parameters
    public static Collection<?> parameters()
    {
        return Arrays.asList(new Object[]{true, true}, new Object[]{false, true}, new Object[]{true, false}, new Object[]{false, false});
    }
    
    public ClientServerChannelTests(boolean secured, boolean multithreaded)
    {
        this.secured = secured;
        this.multithreaded = multithreaded;
    }
    
    @Before
    public void setUp() throws Throwable
    {
        ChannelFactory factory = new ChannelFactory();
        Parameters parameters = new Parameters();
        
        parameters.channelName = "server";
        parameters.receiver = serverReceiver;
        parameters.serverPart = true;
        parameters.secured = secured;
        parameters.keyStorePassword = "testtest";
        parameters.keyStorePath = "classpath:" + Classes.getResourcePath(TcpChannelTests.class) + "/keystore.jks";
        parameters.serializationRegistrars.add(new StreamingProtocolTests.TestStreamMessagePartSerializer());
        parameters.serializationRegistrars.add(new TestMessagePartSerializer());
        parameters.multiThreaded = multithreaded;
        
        server = factory.createChannel(parameters);
        server.getChannelObserver().addChannelListener(serverListener);
        server.start();
        
        parameters.serverPart = false;
        parameters.clientPart = true;
        for (int i = 0; i < COUNT; i++)
        {
            receivers[i] = new ReceiverMock(10, receiveSequencer);
            feeds[i] = new TestFeed();
            parameters.receiver = receivers[i];
            parameters.channelName = "client" + i;
            clients[i] = factory.createChannel(parameters);
            clientListeners[i] = new LiveNodeManagerTests.TestChannelListener(connectionSequencer);
            clients[i].getChannelObserver().addChannelListener(clientListeners[i]);
            clients[i].start();
            
            feeds[i].sink = clients[i].register(server.getLiveNodeProvider().getLocalNode(), feeds[i]);
            clients[i].connect(server.getLiveNodeProvider().getLocalNode().getConnection(0));
        }
        
        connectionSequencer.waitAll(COUNT * 2, CONNECT_TIMEOUT, 0, "Connection.");
        
        for (int i = 0; i < COUNT; i++)
        {
            assertThat(clients[i].getLiveNodeProvider().isLive(server.getLiveNodeProvider().getLocalNode()), is(true));
            assertThat(server.getLiveNodeProvider().isLive(clients[i].getLiveNodeProvider().getLocalNode()), is(true));
            assertThat(serverListener.connected.contains(clients[i].getLiveNodeProvider().getLocalNode()), is(true));
            assertThat(clientListeners[i].connected.contains(server.getLiveNodeProvider().getLocalNode()), is(true));
        }
    }
    
    @After
    public void tearDown() throws Throwable
    {
        server.stop();
        
        for (int i = 0; i < COUNT; i++)
        {
            if (clients[i] != null)
                clients[i].stop();
        }
    }
    
    @Test
    public void testPushSend() throws Throwable
    {
        for (int i = 0; i < COUNT; i++)
        {
            for (int k = 0; k < 10; k++)            
            {
                TestMessagePart part = new TestMessagePart("message" + k);
                IMessage request = clients[i].getMessageFactory().create(server.getLiveNodeProvider().getLocalNode(), part);
                clients[i].send(request);
            }
        }
        
        receiveSequencer.waitAll(COUNT + 1, SEND_TIMEOUT, 0, "Receive messages.");
        
        assertThat(serverReceiver.messages.size(), is(COUNT));
        
        for (int i = 0; i < COUNT; i++)
        {
            List<IMessage> list = serverReceiver.messages.get(clients[i].getLiveNodeProvider().getLocalNode());
            assertThat(list.size(), is(10));
            assertThat(receivers[i].messages.size(), is(10));
            
            for (int k = 0; k < 10; k++)
            {
                IMessage m = list.get(k);
                assertThat(m.getDestination(), is(server.getLiveNodeProvider().getLocalNode()));
                assertThat(m.getSource(), is(clients[i].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getFlags(), is(0));
                TestMessagePart part = m.getPart();
                assertThat(part, is(new TestMessagePart("message" + k)));
                
                m = receivers[i].messages.get(k);
                assertThat(m.getDestination(), is(clients[i].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getSource(), is(server.getLiveNodeProvider().getLocalNode()));
                assertThat(m.getFlags(), is(0));
                part = m.getPart();
                assertThat(part, is(new TestMessagePart("message" + k)));
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
                TestMessagePart part = new TestMessagePart("message" + k);
                messages.add(clients[i].getMessageFactory().create(server.getLiveNodeProvider().getLocalNode(), part));
            }
            
            feeds[i].setMessages(messages.iterator());
        }
        
        receiveSequencer.waitAll(COUNT + 1, SEND_TIMEOUT, 0, "Receive messages.");
        
        assertThat(serverReceiver.messages.size(), is(COUNT));
        
        for (int i = 0; i < COUNT; i++)
        {
            List<IMessage> list = serverReceiver.messages.get(clients[i].getLiveNodeProvider().getLocalNode());
            assertThat(list.size(), is(10));
            assertThat(receivers[i].messages.size(), is(10));
            
            for (int k = 0; k < 10; k++)
            {
                IMessage m = list.get(k);
                assertThat(m.getDestination(), is(server.getLiveNodeProvider().getLocalNode()));
                assertThat(m.getSource(), is(clients[i].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getFlags(), is(0));
                TestMessagePart part = m.getPart();
                assertThat(part, is(new TestMessagePart("message" + k)));
                
                m = receivers[i].messages.get(k);
                assertThat(m.getDestination(), is(clients[i].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getSource(), is(server.getLiveNodeProvider().getLocalNode()));
                assertThat(m.getFlags(), is(0));
                part = m.getPart();
                assertThat(part, is(new TestMessagePart("message" + k)));
            }
        }
    }
    
    @Test
    public void testStreamPushSend() throws Throwable
    {
        for (int i = 0; i < COUNT; i++)
        {
            for (int k = 0; k < 10; k++)            
            {
                TestStreamSendMessagePart part = new TestStreamSendMessagePart("message" + k, StreamingProtocolTests.createBuffers(1, 100000));
                
                IMessage request = clients[i].getMessageFactory().create(server.getLiveNodeProvider().getLocalNode(), 
                    part, MessageFlags.NO_COMPRESS);
                clients[i].send(request);
                
                Thread.sleep(100);
                
                assertThat(part.sendCompleted, is(true));
            }
        }
        
        receiveSequencer.waitAll(COUNT + 1, SEND_TIMEOUT, 0, "Receive messages.");
        
        assertThat(serverReceiver.messages.size(), is(COUNT));
        
        for (int i = 0; i < COUNT; i++)
        {
            List<IMessage> list = serverReceiver.messages.get(clients[i].getLiveNodeProvider().getLocalNode());
            assertThat(list.size(), is(10));
            assertThat(receivers[i].messages.size(), is(10));
            
            for (int k = 0; k < 10; k++)
            {
                IMessage m = list.get(k);
                assertThat(m.getDestination(), is(server.getLiveNodeProvider().getLocalNode()));
                assertThat(m.getSource(), is(clients[i].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getFlags(), is(MessageFlags.NO_COMPRESS | MessageFlags.NO_DELAY));
                TestStreamReceiveMessagePart part = m.getPart();
                assertThat(part, is(new TestStreamReceiveMessagePart("message" + k)));
                assertThat(part.data, is(StreamingProtocolTests.createBuffers(1, 100000)));
                
                m = receivers[i].messages.get(k);
                assertThat(m.getDestination(), is(clients[i].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getSource(), is(server.getLiveNodeProvider().getLocalNode()));
                assertThat(m.getFlags(), is(MessageFlags.NO_COMPRESS | MessageFlags.NO_DELAY));
                part = m.getPart();
                assertThat(part, is(new TestStreamReceiveMessagePart("message" + k)));
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
            List<IMessage> messages = new ArrayList<IMessage>();
            
            for (int k = 0; k < 10; k++)            
            {
                TestStreamSendMessagePart part = new TestStreamSendMessagePart("message" + k, StreamingProtocolTests.createBuffers(1, 100000));
                messages.add(clients[i].getMessageFactory().create(server.getLiveNodeProvider().getLocalNode(), 
                    part, MessageFlags.NO_COMPRESS));
            }
            
            feeds[i].setMessages(messages.iterator());
        }

        receiveSequencer.waitAll(COUNT + 1, SEND_TIMEOUT, 0, "Receive messages.");
        
        assertThat(serverReceiver.messages.size(), is(COUNT));
        
        for (int i = 0; i < COUNT; i++)
        {
            List<IMessage> list = serverReceiver.messages.get(clients[i].getLiveNodeProvider().getLocalNode());
            assertThat(list.size(), is(10));
            assertThat(receivers[i].messages.size(), is(10));
            
            for (int k = 0; k < 10; k++)
            {
                IMessage m = list.get(k);
                assertThat(m.getDestination(), is(server.getLiveNodeProvider().getLocalNode()));
                assertThat(m.getSource(), is(clients[i].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getFlags(), is(MessageFlags.NO_COMPRESS | MessageFlags.NO_DELAY));
                TestStreamReceiveMessagePart part = m.getPart();
                assertThat(part, is(new TestStreamReceiveMessagePart("message" + k)));
                assertThat(part.data, is(StreamingProtocolTests.createBuffers(1, 100000)));
                
                m = receivers[i].messages.get(k);
                assertThat(m.getDestination(), is(clients[i].getLiveNodeProvider().getLocalNode()));
                assertThat(m.getSource(), is(server.getLiveNodeProvider().getLocalNode()));
                assertThat(m.getFlags(), is(MessageFlags.NO_COMPRESS | MessageFlags.NO_DELAY));
                part = m.getPart();
                assertThat(part, is(new TestStreamReceiveMessagePart("message" + k)));
                assertThat(part.data, is(StreamingProtocolTests.createBuffers(1, 100000)));
                assertThat(part.receiveCompleted, is(true));
            }
        }
    }

    private class ServerReceiver implements IReceiver
    {
        public Map<IAddress, List<IMessage> > messages = new HashMap<IAddress, List<IMessage>>();
        public int index;
        public final int count;
        
        public ServerReceiver(int count)
        {
            this.count = count;
        }
        
        @Override
        public synchronized void receive(IMessage message)
        {
            List<IMessage> list = messages.get(message.getSource());
            if (list == null)
            {
                list = new ArrayList<IMessage>();
                messages.put(message.getSource(), list);
            }
            list.add(message);

            IMessagePart part = message.getPart();
            if (part instanceof StreamingProtocolTests.TestStreamReceiveMessagePart)
            {
                TestStreamReceiveMessagePart test = (TestStreamReceiveMessagePart)part;
                part = new TestStreamSendMessagePart(test.value, test.data);
            }
            
            IMessage reply = server.getMessageFactory().create(message.getSource(), part, message.getFlags());
            server.send(reply);
            
            index++;
            
            if (index == count)
                receiveSequencer.allowSingle("Received " + message.getSource());
        }
    }
    
    public static class TestFeed implements IFeed
    {
        private Iterator<IMessage> messages;
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
