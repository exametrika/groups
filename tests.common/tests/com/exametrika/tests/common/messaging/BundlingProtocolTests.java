/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.protocols.optimize.BundleMessagePart;
import com.exametrika.common.messaging.impl.protocols.optimize.BundlingProtocol;
import com.exametrika.common.messaging.impl.protocols.streaming.StreamingProtocol;
import com.exametrika.common.tests.Tests;
import com.exametrika.tests.common.messaging.ClientServerChannelTests.TestMessagePart;
import com.exametrika.tests.common.messaging.ClientServerChannelTests.TestMessagePartSerializer;
import com.exametrika.tests.common.time.TimeServiceMock;

/**
 * The {@link BundlingProtocolTests} are tests for {@link BundlingProtocol}.
 * 
 * @see StreamingProtocol
 * @author Medvedev-A
 */
public class BundlingProtocolTests
{
    private SenderMock sender;
    private ReceiverMock receiver;
    private SerializationRegistry registry;
    private BundlingProtocol protocol;
    private ChannelObserver channelObserver;
    private LiveNodeManager liveNodeManager;
    private IMessageFactory messageFactory;
    private IAddress member1;
    private IAddress member2;
    private IAddress member3;
    private IAddress member4;
    private TimeServiceMock timeService;

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
        channelObserver = new ChannelObserver("test");
        liveNodeManager = new LiveNodeManager("test", Arrays.<IFailureObserver>asList(channelObserver), channelObserver);
        liveNodeManager.setLocalNode(member1);
        liveNodeManager.onNodesConnected(Collections.singleton(member2));
        liveNodeManager.onNodesConnected(Collections.singleton(member3));
        liveNodeManager.onNodesConnected(Collections.singleton(member4));
        
        registry = new SerializationRegistry();
        registry.register(new TestMessagePartSerializer());

        messageFactory = new MessageFactory(registry, liveNodeManager);
        
        protocol = new BundlingProtocol("test", messageFactory, registry, 1000, 100, 100000, 600000, false);
        protocol.setSender(sender);
        protocol.setPullableSender(new PullableSenderMock());
        protocol.setReceiver(receiver);
        protocol.setTimeService(timeService);
        registry.register(protocol);

        liveNodeManager.start();
        channelObserver.start();
        protocol.start();   
        
        Thread.sleep(100);
    }

    @Test
    public void testProtocol() throws Exception
    {
        IMessage message1 = messageFactory.create(member2, new TestMessagePart("value"));
        protocol.send(message1);
        assertThat(sender.messages.isEmpty(), is(true));
        
        IMessage message2 = messageFactory.create(member2, MessageFlags.PARALLEL);
        protocol.send(message2);
        
        assertThat(sender.messages.size(), is(1));
        assertThat(sender.messages.get(0).getFlags(), is(MessageFlags.PARALLEL));
        sender.messages.clear();
        
        protocol.onTimer(50);
        assertThat(sender.messages.isEmpty(), is(true));
        
        protocol.onTimer(101); 
        assertThat(sender.messages.size(), is(1));
        assertThat(sender.messages.get(0).getPart() instanceof BundleMessagePart, is(true));
        sender.messages.clear();
        
        protocol.send(message1);
        IMessage message3 = messageFactory.create(member2, MessageFlags.NO_DELAY);
        protocol.send(message3);
        
        assertThat(sender.messages.size(), is(2));
        assertThat(sender.messages.get(0).getPart() instanceof BundleMessagePart, is(true));
        assertThat(sender.messages.get(1).getFlags(), is(MessageFlags.NO_DELAY));
        sender.messages.clear();
        
        protocol.send(message1);
        message3 = messageFactory.create(member2, MessageFlags.HIGH_PRIORITY);
        protocol.send(message3);
        
        assertThat(sender.messages.size(), is(2));
        assertThat(sender.messages.get(0).getPart() instanceof BundleMessagePart, is(true));
        assertThat(sender.messages.get(1).getFlags(), is(MessageFlags.HIGH_PRIORITY));
        sender.messages.clear();
        
        protocol.send(message1);
        message3 = messageFactory.create(member2, new TestMessagePart("value"), 0, Arrays.asList(new File("test")));
        protocol.send(message3);
        
        assertThat(sender.messages.size(), is(2));
        assertThat(sender.messages.get(0).getPart() instanceof BundleMessagePart, is(true));
        assertThat(sender.messages.get(1).getFiles() != null, is(true));
        sender.messages.clear();
        
        protocol.send(message1);
        message3 = messageFactory.create(member2, new TestMessagePart(new String(new char[1001])));
        protocol.send(message3);
        
        assertThat(sender.messages.size(), is(2));
        assertThat(sender.messages.get(0).getPart() instanceof BundleMessagePart, is(true));
        assertThat(sender.messages.get(1).getPart() instanceof TestMessagePart, is(true));
        sender.messages.clear();
        
        timeService.time = 200;
        protocol.send(message1);
        protocol.send(message1);
        assertThat(sender.messages.isEmpty(), is(true));
        
        timeService.time = 300;
        
        protocol.send(message1);
        assertThat(sender.messages.isEmpty(), is(true));
        
        timeService.time = 301;
        protocol.send(message1);
        
        assertThat(sender.messages.size(), is(1));
        assertThat(sender.messages.get(0).getPart() instanceof BundleMessagePart, is(true));
        sender.messages.clear();
        
        for (int i = 0; i <= 10000; i++)
        {
            protocol.send(message1);
            if (i <= 9999)
                assertThat(sender.messages.isEmpty(), is(true));
        }
        
        assertThat(sender.messages.size(), is(1));
        assertThat(sender.messages.get(0).getPart() instanceof BundleMessagePart, is(true));
        
        protocol.receive(sender.messages.get(0));
        
        assertThat(receiver.messages.size(), is(10001));
        for (int i = 0; i <= 10000; i++)
            assertThat((TestMessagePart)receiver.messages.get(i).getPart(), is(new TestMessagePart("value")));
        
        IMessage message4 = messageFactory.create(member3, new TestMessagePart("value"));
        protocol.send(message4);
        
        Map sendQueues = Tests.get(protocol, "sendQueues");
        assertThat(sendQueues.size(), is(2));
        
        liveNodeManager.onNodesFailed(com.exametrika.common.utils.Collections.asSet(member3));
        protocol.cleanup(new CleanupManagerMock(liveNodeManager), liveNodeManager, 200);
        
        assertThat(sendQueues.size(), is(1));
        
        protocol.onTimer(1000);
        assertThat(sendQueues.size(), is(1));
        
        protocol.onTimer(600001);
        assertThat(sendQueues.isEmpty(), is(true));
    }
}
