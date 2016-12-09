/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.net.nio.TcpNioDispatcher;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Classes;
import com.exametrika.tests.common.messaging.ClientServerChannelTests.TestFeed;
import com.exametrika.tests.common.messaging.ClientServerChannelTests.TestMessagePart;
import com.exametrika.tests.common.messaging.ClientServerChannelTests.TestMessagePartSerializer;
import com.exametrika.tests.common.net.TcpChannelTests;


/**
 * The {@link SpecialChannelTests} are tests for client-server {@link Channel}.
 * 
 * @see Channel
 * @author Medvedev-A
 */
@RunWith(Parameterized.class)
public class SpecialChannelTests
{
    private static final int CONNECT_TIMEOUT = 60000;
    private static final int SEND_TIMEOUT = 600000;
    private IChannel client;
    private ReceiverMock receiver;
    private TestFeed feed;
    private final Sequencer connectionSequencer = new Sequencer();
    private final Sequencer receiveSequencer = new Sequencer();
    private ReceiverMock serverReceiver = new ReceiverMock(30, receiveSequencer);
    private LiveNodeManagerTests.TestChannelListener clientListener;
    private LiveNodeManagerTests.TestChannelListener serverListener = new LiveNodeManagerTests.TestChannelListener(connectionSequencer);
    private IChannel server;
    private final boolean secured;
    private final boolean multithreaded;
    
    @Parameters
    public static Collection<?> parameters()
    {
        return Arrays.asList(new Object[]{true, true}, new Object[]{false, true}, new Object[]{true, false}, new Object[]{false, false});
    }
    
    public SpecialChannelTests(boolean secured, boolean multithreaded)
    {
        this.secured = secured;
        this.multithreaded = multithreaded;
    }
    
    @Before
    public void setUp() throws Throwable
    {
        ChannelFactory factory = new ChannelFactory();
        ChannelFactory.Parameters parameters = new ChannelFactory.Parameters();
        
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

        receiver = new ReceiverMock();
        feed = new TestFeed();
        parameters.receiver = receiver;
        parameters.channelName = "client";
        client = factory.createChannel(parameters);
        clientListener = new LiveNodeManagerTests.TestChannelListener(connectionSequencer);
        client.getChannelObserver().addChannelListener(clientListener);
        client.start();
        
        feed.sink = client.register(server.getLiveNodeProvider().getLocalNode(), feed);
        client.connect(server.getLiveNodeProvider().getLocalNode().getConnection());
        
        connectionSequencer.waitAll(2, CONNECT_TIMEOUT, 0, "Connection.");
        
        assertThat(client.getLiveNodeProvider().isLive(server.getLiveNodeProvider().getLocalNode()), is(true));
        assertThat(server.getLiveNodeProvider().isLive(client.getLiveNodeProvider().getLocalNode()), is(true));
        assertThat(serverListener.connected.contains(client.getLiveNodeProvider().getLocalNode()), is(true));
        assertThat(clientListener.connected.contains(server.getLiveNodeProvider().getLocalNode()), is(true));
    }
    
    @After
    public void tearDown() throws Throwable
    {
        server.stop();
        client.stop();
    }
    
    @Test
    public void testPrioritySend() throws Throwable
    {
        TcpNioDispatcher dispatcher = Tests.get(Tests.get(client, "transport"), "dispatcher");
        dispatcher.suspend();
        for (int k = 0; k < 10; k++)            
        {
            TestMessagePart part = new TestMessagePart("message" + (k + 20));
            IMessage request = client.getMessageFactory().create(server.getLiveNodeProvider().getLocalNode(), part, 
                MessageFlags.LOW_PRIORITY | MessageFlags.NO_DELAY);
            client.send(request);
        }
        
        for (int k = 0; k < 10; k++)            
        {
            TestMessagePart part = new TestMessagePart("message" + (k + 10));
            IMessage request = client.getMessageFactory().create(server.getLiveNodeProvider().getLocalNode(), part, MessageFlags.NO_DELAY);
            client.send(request);
        }
        
        for (int k = 0; k < 10; k++)            
        {
            TestMessagePart part = new TestMessagePart("message" + k);
            IMessage request = client.getMessageFactory().create(server.getLiveNodeProvider().getLocalNode(), part, 
                MessageFlags.HIGH_PRIORITY | MessageFlags.NO_DELAY);
            client.send(request);
        }
        
        Thread.sleep(500);
        dispatcher.resume();
        
        receiveSequencer.waitAll(1, SEND_TIMEOUT, 0, "Receive messages.");
        
        assertThat(serverReceiver.messages.size(), is(30));
        
        for (int k = 0; k < 30; k++)
        {
            IMessage m = serverReceiver.messages.get(k);
            assertThat(m.getDestination(), is(server.getLiveNodeProvider().getLocalNode()));
            assertThat(m.getSource(), is(client.getLiveNodeProvider().getLocalNode()));
            TestMessagePart part = m.getPart();
            assertThat(part, is(new TestMessagePart("message" + k)));
            
            if (k < 10)
                assertThat(m.getFlags(), is(MessageFlags.HIGH_PRIORITY | MessageFlags.NO_DELAY));
            else if (k < 10)
                assertThat(m.getFlags(), is(MessageFlags.NO_DELAY));
            else if (k < 10)
                assertThat(m.getFlags(), is(MessageFlags.LOW_PRIORITY | MessageFlags.NO_DELAY));
        }
    }
    
    @Test
    public void testParallelSend() throws Throwable
    {
        for (int k = 0; k < 30; k++)            
        {
            TestMessagePart part = new TestMessagePart("message" + k);
            IMessage request = client.getMessageFactory().create(server.getLiveNodeProvider().getLocalNode(), part, 
                MessageFlags.PARALLEL);
            client.send(request);
        }
        
        receiveSequencer.waitAll(1, SEND_TIMEOUT, 0, "Receive messages.");
        
        assertThat(serverReceiver.messages.size(), is(30));
        
        //boolean unordered = false;
        for (int k = 0; k < 30; k++)
        {
            IMessage m = serverReceiver.messages.get(k);
            assertThat(m.getDestination(), is(server.getLiveNodeProvider().getLocalNode()));
            assertThat(m.getSource(), is(client.getLiveNodeProvider().getLocalNode()));
            assertThat(m.getFlags(), is(MessageFlags.PARALLEL));
//            TestMessagePart part = m.getPart();
//            if (!part.equals(new TestMessagePart("message" + k)))
//                unordered = true;
        }
        //assertThat(unordered, is(true));
    }
}
