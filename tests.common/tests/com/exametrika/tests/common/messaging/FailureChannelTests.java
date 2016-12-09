/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.MessageFlags;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.transports.tcp.ITcpAddress;
import com.exametrika.common.messaging.impl.transports.tcp.TcpConnection;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Classes;
import com.exametrika.tests.common.messaging.ClientServerChannelTests.TestFeed;
import com.exametrika.tests.common.messaging.ClientServerChannelTests.TestMessagePart;
import com.exametrika.tests.common.messaging.ClientServerChannelTests.TestMessagePartSerializer;
import com.exametrika.tests.common.net.TcpChannelTests;


/**
 * The {@link FailureChannelTests} are tests for client-server {@link Channel}.
 * 
 * @see Channel
 * @author Medvedev-A
 */
public class FailureChannelTests
{
    private static final int CONNECT_TIMEOUT = 60000;
    private final Sequencer connectionSequencer = new Sequencer();
    private IChannel client;
    private ReceiverMock receiver;
    private TestFeed feed;
    private ReceiverMock serverReceiver = new ReceiverMock();
    private LiveNodeManagerTests.TestChannelListener clientListener;
    private LiveNodeManagerTests.TestChannelListener serverListener = new LiveNodeManagerTests.TestChannelListener(connectionSequencer);
    private IChannel server;
    
    @After
    public void tearDown()
    {
        if (client != null)
            client.stop();
        if (server != null)
            server.stop();
    }
    
    @Test
    public void testDisconnect() throws Throwable
    {
        ChannelFactory.FactoryParameters parameters = new ChannelFactory.FactoryParameters();
        parameters.cleanupPeriod = 100;
        parameters.transportMinReconnectPeriod = 1000;
        
        createChannels(false, new ChannelFactory(parameters));
        
        server.stop();
        
        Thread.sleep(100);
        
        assertThat(!client.getLiveNodeProvider().isLive(server.getLiveNodeProvider().getLocalNode()), is(true));
        assertThat(clientListener.disconnected.contains(server.getLiveNodeProvider().getLocalNode()), is(true));
        
        Map map = Tests.get(Tests.get(client, "transport"), "connections");
        TcpConnection connection = (TcpConnection)map.get(((ITcpAddress)server.getLiveNodeProvider().getLocalNode()).getAddress());
        assertThat((Boolean)Tests.get(connection, "closed"), is(true));
        List<TcpConnection> closedConnections = Tests.get(Tests.get(client, "transport"), "closedConnections");
        assertThat(closedConnections, is(Arrays.asList(connection)));

        Thread.sleep(3000);
        
        assertThat(map.isEmpty(), is(true));
        assertThat(closedConnections.isEmpty(), is(true));

        client.stop();
    }
    
    @Test
    public void testSecuredDisconnect() throws Throwable
    {
        ChannelFactory.FactoryParameters parameters = new ChannelFactory.FactoryParameters();
        parameters.cleanupPeriod = 100;
        parameters.transportMinReconnectPeriod = 1000;
        
        createChannels(true, new ChannelFactory(parameters));
        
        server.stop();
        
        Thread.sleep(100);
        
        assertThat(!client.getLiveNodeProvider().isLive(server.getLiveNodeProvider().getLocalNode()), is(true));
        assertThat(clientListener.disconnected.contains(server.getLiveNodeProvider().getLocalNode()), is(true));
        
        Map map = Tests.get(Tests.get(client, "transport"), "connections");
        TcpConnection connection = (TcpConnection)map.get(((ITcpAddress)server.getLiveNodeProvider().getLocalNode()).getAddress());
        assertThat((Boolean)Tests.get(connection, "closed"), is(true));
        List<TcpConnection> closedConnections = Tests.get(Tests.get(client, "transport"), "closedConnections");
        assertThat(closedConnections, is(Arrays.asList(connection)));

        Thread.sleep(3000);
        
        assertThat(map.isEmpty(), is(true));
        assertThat(closedConnections.isEmpty(), is(true));

        client.stop();
    }
    
    @Test
    public void testTcpFailure() throws Throwable
    {
        ChannelFactory.FactoryParameters parameters = new ChannelFactory.FactoryParameters();
        parameters.cleanupPeriod = 100;
        parameters.transportMinReconnectPeriod = 1000;

        createChannels(true, new ChannelFactory(parameters));
        
        Map map = Tests.get(Tests.get(client, "transport"), "connections");
        TcpConnection connection = (TcpConnection)map.get(((ITcpAddress)server.getLiveNodeProvider().getLocalNode()).getAddress());
        connection.close();
        
        Thread.sleep(100);
        
        assertThat(!client.getLiveNodeProvider().isLive(server.getLiveNodeProvider().getLocalNode()), is(true));
        assertThat(!server.getLiveNodeProvider().isLive(client.getLiveNodeProvider().getLocalNode()), is(true));
        assertThat(serverListener.failed.contains(client.getLiveNodeProvider().getLocalNode()), is(true));
        assertThat(clientListener.failed.contains(server.getLiveNodeProvider().getLocalNode()), is(true));
        
        assertThat((Boolean)Tests.get(connection, "closed"), is(true));
        List<TcpConnection> closedConnections = Tests.get(Tests.get(client, "transport"), "closedConnections");
        assertThat(closedConnections, is(Arrays.asList(connection)));

        Thread.sleep(3000);
        
        assertThat(map.isEmpty(), is(true));
        assertThat(closedConnections.isEmpty(), is(true));

        server.stop();
        client.stop();
    }
    
    @Test
    public void testHeartbeatFailure() throws Throwable
    {
        ChannelFactory.FactoryParameters parameters = new ChannelFactory.FactoryParameters();
        parameters.heartbeatFailureDetectionPeriod = 1000;
        parameters.heartbeatPeriod = 100;
        parameters.heartbeatStartPeriod = 200;
        parameters.heartbeatTrackPeriod = 100;
        parameters.cleanupPeriod = 100;
        parameters.transportMinReconnectPeriod = 2000;
        
        createChannels(true, new ChannelFactory(parameters));
        
        AbstractProtocol serverLast = ((ProtocolStack)Tests.get(server, "protocolStack")).getLast();
        AbstractProtocol clientLast = ((ProtocolStack)Tests.get(client, "protocolStack")).getLast();
        
        IReceiver serverHeartbeat = Tests.get(Tests.get(serverLast, "receiver"), "receiver");
        IReceiver clientHeartbeat = Tests.get(Tests.get(clientLast, "receiver"), "receiver");
        FailureSender serverSender = new FailureSender((ISender)Tests.get(serverLast, "sender"));
        Tests.set(serverLast, "sender", serverSender);
        
        FailureSender clientSender = new FailureSender((ISender)Tests.get(clientLast, "sender"));
        Tests.set(clientLast, "sender", clientSender);
        
        Thread.sleep(1500);
        
        Map map = Tests.get(Tests.get(client, "transport"), "connections");
        TcpConnection connection = (TcpConnection)map.get(((ITcpAddress)server.getLiveNodeProvider().getLocalNode()).getAddress());
        assertThat((Boolean)Tests.get(connection, "closed"), is(true));
        List<TcpConnection> closedConnections = Tests.get(Tests.get(client, "transport"), "closedConnections");
        assertThat(closedConnections, is(Arrays.asList(connection)));

        Thread.sleep(3000);
        
        assertThat(map.isEmpty(), is(true));
        assertThat(closedConnections.isEmpty(), is(true));
        
        assertThat(!client.getLiveNodeProvider().isLive(server.getLiveNodeProvider().getLocalNode()), is(true));
        assertThat(!server.getLiveNodeProvider().isLive(client.getLiveNodeProvider().getLocalNode()), is(true));
        assertThat(serverListener.failed.contains(client.getLiveNodeProvider().getLocalNode()), is(true));
        assertThat(clientListener.failed.contains(server.getLiveNodeProvider().getLocalNode()), is(true));
        assertThat(((Map)Tests.get(serverHeartbeat, "heartbeats")).isEmpty(), is(true));
        assertThat(((Map)Tests.get(clientHeartbeat, "heartbeats")).isEmpty(), is(true));
        
        server.stop();
        client.stop();
    }
    
    @Test
    public void testBlockReconnect() throws Throwable
    {
        ChannelFactory.FactoryParameters factoryParameters = new ChannelFactory.FactoryParameters();
        factoryParameters.cleanupPeriod = 100;
        factoryParameters.transportMinReconnectPeriod = 1000;
        
        ChannelFactory factory = new ChannelFactory(factoryParameters);
        
        createChannels(true, factory); 
        server.stop();
        
        Thread.sleep(100);
        
        assertThat(!client.getLiveNodeProvider().isLive(server.getLiveNodeProvider().getLocalNode()), is(true));
        assertThat(clientListener.disconnected.contains(server.getLiveNodeProvider().getLocalNode()), is(true));
        
        ChannelFactory.Parameters parameters = new ChannelFactory.Parameters();
        
        parameters.channelName = "server";
        parameters.receiver = serverReceiver;
        parameters.serverPart = true;
        parameters.secured = true;
        parameters.portRangeStart = ((ITcpAddress)server.getLiveNodeProvider().getLocalNode()).getAddress().getPort();
        parameters.portRangeEnd = parameters.portRangeStart;
        parameters.keyStorePassword = "testtest";
        parameters.keyStorePath = "classpath:" + Classes.getResourcePath(TcpChannelTests.class) + "/keystore.jks";
        parameters.serializationRegistrars.add(new StreamingProtocolTests.TestStreamMessagePartSerializer());
        parameters.serializationRegistrars.add(new TestMessagePartSerializer());
        
        clientListener.connected.clear();
        
        server = factory.createChannel(parameters);
        server.getChannelObserver().addChannelListener(serverListener);
        server.start();
        
        client.connect(server.getLiveNodeProvider().getLocalNode().getConnection());
        
        Thread.sleep(1000);
        
        assertThat(clientListener.connected.isEmpty(), is(true));
        
        Thread.sleep(2000);
        
        client.connect(server.getLiveNodeProvider().getLocalNode().getConnection());
        
        Thread.sleep(1000);
        
        assertThat(clientListener.connected.get(0), is(server.getLiveNodeProvider().getLocalNode()));

        client.stop();
    }

    @Test
    public void testQueueOverflow() throws Throwable
    {
        ChannelFactory.FactoryParameters factoryParameters = new ChannelFactory.FactoryParameters(true);
        factoryParameters.heartbeatStartPeriod = 1000000;
        factoryParameters.transportMaxUnlockSendQueueCapacity = 0;
        factoryParameters.transportMinLockSendQueueCapacity = 100;
        
        ChannelFactory factory = new ChannelFactory(factoryParameters);
        
        createChannels(true, factory);
        
        for (int k = 0; k < 100; k++)            
        {
            TestMessagePart part = new TestMessagePart("message" + k);
            IMessage request = client.getMessageFactory().create(server.getLiveNodeProvider().getLocalNode(), part);
            client.send(request);
        }
        
        Thread.sleep(500);
        
        assertThat(serverReceiver.messages.size(), is(100));
        
        for (int k = 0; k < 100; k++)
        {
            IMessage m = serverReceiver.messages.get(k);
            assertThat(m.getDestination(), is(server.getLiveNodeProvider().getLocalNode()));
            assertThat(m.getSource(), is(client.getLiveNodeProvider().getLocalNode()));
            TestMessagePart part = m.getPart();
            assertThat(part, is(new TestMessagePart("message" + k)));
            assertThat(m.getFlags(), is(0));
        }
    }

    private void createChannels(boolean secured, ChannelFactory factory) throws Throwable
    {
        ChannelFactory.Parameters parameters = new ChannelFactory.Parameters();
        
        parameters.channelName = "server";
        parameters.receiver = serverReceiver;
        parameters.serverPart = true;
        parameters.secured = secured;
        parameters.keyStorePassword = "testtest";
        parameters.keyStorePath = "classpath:" + Classes.getResourcePath(TcpChannelTests.class) + "/keystore.jks";
        parameters.serializationRegistrars.add(new StreamingProtocolTests.TestStreamMessagePartSerializer());
        parameters.serializationRegistrars.add(new TestMessagePartSerializer());
        
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
    
    private static class FailureSender implements ISender
    {
        private final ISender sender;

        public FailureSender(ISender sender)
        {
            this.sender = sender;
        }
        
        @Override
        public void send(IMessage message)
        {
            if (message.hasOneOfFlags(MessageFlags.HEARTBEAT_REQUEST | MessageFlags.HEARTBEAT_RESPONSE))
                return;
            
            sender.send(message);
        }
        
    }
}
