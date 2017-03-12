/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.impl.MessageFlags;
import com.exametrika.common.messaging.impl.message.Message;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.FullNodeTrackingStrategy;
import com.exametrika.common.messaging.impl.protocols.failuredetection.HeartbeatProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeAccessTimeProvider;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.tests.Tests;
import com.exametrika.tests.common.time.TimeServiceMock;

/**
 * The {@link HeartbeatProtocolTests} are tests for {@link HeartbeatProtocol}.
 * 
 * @see HeartbeatProtocol
 * @author Medvedev-A
 */
public class HeartbeatProtocolTests
{
    private SerializationRegistry registry;
    private NodeAccessTimeProviderMock accessTimeProvider;
    private HeartbeatProtocol protocol;
    private LiveNodeManager liveNodeManager;
    private ChannelObserver channelObserver;
    private ReceiverMock receiver;
    private SenderMock sender;
    private TimeServiceMock timeService;
    private IAddress member1;
    private IAddress member2;
    private IAddress member3;
    private IAddress member4;
    private IAddress member5;

    @Before
    public void setUp() throws Throwable
    {
        accessTimeProvider = new NodeAccessTimeProviderMock();
        member2 = new TestAddress(UUID.randomUUID(), "member2");
        member3 = new TestAddress(UUID.randomUUID(), "member3");
        member4 = new TestAddress(UUID.randomUUID(), "member4");
        member5 = new TestAddress(UUID.randomUUID(), "member5");
        
        timeService = new TimeServiceMock();
        timeService.useSystemTime = false;
        receiver = new ReceiverMock();
        sender = new SenderMock();
        channelObserver = new ChannelObserver("test");
        liveNodeManager = new LiveNodeManager("test", Arrays.<IFailureObserver>asList(channelObserver), channelObserver);
        member1 = liveNodeManager.getLocalNode();
        liveNodeManager.onNodesConnected(Collections.singleton(member2));
        liveNodeManager.onNodesConnected(Collections.singleton(member3));
        liveNodeManager.onNodesConnected(Collections.singleton(member4));
        
        registry = new SerializationRegistry();
        protocol = new HeartbeatProtocol("test", new FullNodeTrackingStrategy(), 
            new MessageFactory(registry, liveNodeManager), 100, 1000, 200, 2000);
        protocol.setReceiver(receiver);
        protocol.setSender(sender);
        protocol.setPullableSender(new PullableSenderMock());
        protocol.setFailureObserver(liveNodeManager);
        protocol.setTimeService(timeService);
        protocol.setAccessTimeProvider(accessTimeProvider);
        registry.register(protocol);
        
        liveNodeManager.start();
        channelObserver.start();
        protocol.start();
        
        Thread.sleep(100);
    }

    @After
    public void tearDown()
    {
        protocol.stop();
        channelObserver.stop();
        liveNodeManager.stop();
    }

    @Test
    public void testCleanup() throws Throwable
    {
        timeService.time = 1000;
        protocol.cleanup(new CleanupManagerMock(liveNodeManager), liveNodeManager, 1000);
        Map heartbeats = Tests.get(protocol, "heartbeats");
        assertThat(heartbeats.size(), is(3));
        check(heartbeats, member2, 1000);
        check(heartbeats, member3, 1000);
        check(heartbeats, member4, 1000);
        
        liveNodeManager.onNodesFailed(Collections.singleton(member2));
        liveNodeManager.onNodesLeft(Collections.singleton(member3));
        liveNodeManager.onNodesConnected(Collections.singleton(member5));
        
        Thread.sleep(100);
        
        protocol.cleanup(new CleanupManagerMock(liveNodeManager), liveNodeManager, 2000);
        
        heartbeats = Tests.get(protocol, "heartbeats");
        assertThat(heartbeats.size(), is(2));
        check(heartbeats, member4, 1000);
        check(heartbeats, member5, 2000);
    }
    
    @Test
    public void testReceive() throws Throwable
    {
        protocol.cleanup(new CleanupManagerMock(liveNodeManager), liveNodeManager, 1000);
        timeService.time = 2000;
        
        protocol.receive(new Message(member5, member1, 0, registry));
        Map heartbeats = Tests.get(protocol, "heartbeats");
        assertThat(heartbeats.size(), is(3));
        assertThat(sender.messages.isEmpty(), is(true));
        assertThat(receiver.messages.size(), is(1));
        assertThat(receiver.messages.get(0).getSource(), is(member5));
        receiver.messages.clear();
        
        protocol.receive(new Message(member2, member1, 0, registry));
        check(heartbeats, member2, 1000);
        assertThat(sender.messages.isEmpty(), is(true));
        assertThat(receiver.messages.size(), is(1));
        assertThat(receiver.messages.get(0).getSource(), is(member2));
        receiver.messages.clear();
        
        timeService.time = 3000;
        protocol.receive(new Message(member2, member1, MessageFlags.HEARTBEAT_REQUEST, registry));
        check(heartbeats, member2, 1000);
        assertThat(receiver.messages.isEmpty(), is(true));
        assertThat(sender.messages.size(), is(1));
        assertThat(sender.messages.get(0).getDestination(), is(member2));
        assertThat(sender.messages.get(0).getSource(), is(member1));
        assertThat(sender.messages.get(0).getFlags(), is(MessageFlags.PARALLEL | MessageFlags.HIGH_PRIORITY | 
            MessageFlags.HEARTBEAT_RESPONSE));
        sender.messages.clear();
        
        timeService.time = 4000;
        protocol.receive(new Message(member2, member1, MessageFlags.PARALLEL | MessageFlags.HIGH_PRIORITY | 
            MessageFlags.HEARTBEAT_RESPONSE, registry));
        check(heartbeats, member2, 1000);
        assertThat(receiver.messages.isEmpty(), is(true));
        assertThat(sender.messages.isEmpty(), is(true));
    }
    
    @Test
    public void testTrack() throws Throwable
    {
        accessTimeProvider.lastReadTimes.put(member2, 2000l);
        
        protocol.cleanup(new CleanupManagerMock(liveNodeManager), liveNodeManager, 2000);

        timeService.time = 2000;
        protocol.onTimer(timeService.time);
        assertThat(sender.messages.isEmpty(), is(true));
        
        timeService.time = 2050;
        protocol.onTimer(timeService.time);
        assertThat(sender.messages.isEmpty(), is(true));
        
        timeService.time = 2150;
        protocol.onTimer(timeService.time);
        assertThat(sender.messages.isEmpty(), is(true));
        
        timeService.time = 3000;
        protocol.receive(new Message(member3, member1, MessageFlags.NO_COMPRESS, registry));
        protocol.receive(new Message(member4, member1, MessageFlags.NO_COMPRESS, registry));
        
        timeService.time = 3100;
        protocol.onTimer(timeService.time);
        assertThat(sender.messages.size(), is(1));
        assertThat(sender.messages.get(0).getDestination(), is(member2));
        assertThat(sender.messages.get(0).getSource(), is(member1));
        assertThat(sender.messages.get(0).getFlags(), is(MessageFlags.PARALLEL | MessageFlags.HIGH_PRIORITY | 
            MessageFlags.HEARTBEAT_REQUEST));
        sender.messages.clear();
        
        timeService.time = 3250;
        protocol.onTimer(timeService.time);
        assertThat(sender.messages.isEmpty(), is(true));
        
        timeService.time = 3350;
        protocol.onTimer(timeService.time);
        assertThat(sender.messages.size(), is(1));
        assertThat(sender.messages.get(0).getDestination(), is(member2));
        assertThat(sender.messages.get(0).getSource(), is(member1));
        assertThat(sender.messages.get(0).getFlags(), is(MessageFlags.PARALLEL | MessageFlags.HIGH_PRIORITY | 
            MessageFlags.HEARTBEAT_REQUEST));
        sender.messages.clear();
        
        timeService.time = 3900;
        protocol.receive(new Message(member3, member1, MessageFlags.NO_COMPRESS, registry));
        protocol.receive(new Message(member4, member1, MessageFlags.NO_COMPRESS, registry));
        
        timeService.time = 4000;
        protocol.onTimer(timeService.time);
        
        Thread.sleep(100);
        assertThat(sender.messages.isEmpty(), is(true));
        
        assertThat(liveNodeManager.getId(), is(4l));
        assertThat(new TreeSet(liveNodeManager.getLiveNodes()), is(new TreeSet(Arrays.asList(member1, member3, member4))));
    }
    
    private void check(Map heartbeats, IAddress node, long requestTime) throws Throwable
    {
        Object info = heartbeats.get(node);
        assertThat((Long)Tests.get(info, "lastRequestTime"), is(requestTime));
    }
    
    private static class NodeAccessTimeProviderMock implements INodeAccessTimeProvider
    {
        private Map<IAddress, Long> lastReadTimes = new HashMap<IAddress, Long>();
        
        @Override
        public long getLastReadTime(IAddress node)
        {
            Long t = lastReadTimes.get(node);
            if (t == null)
                return 0;
            else
                return t;
        }

        @Override
        public long getLastWriteTime(IAddress node)
        {
            return 0;
        }
    }
}
