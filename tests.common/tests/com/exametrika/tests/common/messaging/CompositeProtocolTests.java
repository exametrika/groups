/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.impl.MessageFlags;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.composite.AbstractCompositeProtocol;
import com.exametrika.common.messaging.impl.protocols.composite.ProtocolSubStack;
import com.exametrika.common.messaging.impl.protocols.error.UnhandledMessageProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.protocols.optimize.LocalSendOptimizationProtocol;
import com.exametrika.common.messaging.impl.protocols.routing.AllMessageFlagsRoutingCondition;
import com.exametrika.common.messaging.impl.protocols.routing.MessageRouter;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.Pair;


/**
 * The {@link CompositeProtocolTests} are tests for composite protocol implementations.
 * 
 * @see AbstractCompositeProtocol
 * @see ProtocolSubStack
 * @see MessageRouter
 * @author Medvedev-A
 */
public class CompositeProtocolTests
{
    @Test
    public void testProtocol()
    {
        IAddress member1 = new TestAddress(UUID.randomUUID(), "member1");
        IAddress member2 = new TestAddress(UUID.randomUUID(), "member2");
        
        ChannelObserver channelObserver = new ChannelObserver("test");
        LiveNodeManager liveNodeManager = new LiveNodeManager("test", Arrays.<IFailureObserver>asList(channelObserver), channelObserver);
        liveNodeManager.setLocalNode(member1);
        liveNodeManager.onNodesConnected(Collections.singleton(member2));
        
        SerializationRegistry registry = new SerializationRegistry();
        
        IMessageFactory messageFactory = new MessageFactory(registry, liveNodeManager);
        
        TestLeafProtocol leaf1 = new TestLeafProtocol("test", messageFactory);
        TestProtocol parent1 = new TestProtocol("test", messageFactory);
        TestLeafProtocol leaf2 = new TestLeafProtocol("test", messageFactory);
        TestProtocol parent2 = new TestProtocol("test", messageFactory);
        TestRootProtocol root = new TestRootProtocol("test", messageFactory);
        LocalSendOptimizationProtocol local = new LocalSendOptimizationProtocol("test", null, messageFactory, liveNodeManager);
        UnhandledMessageProtocol error = new UnhandledMessageProtocol("test", messageFactory);
        
        TestProtocol[] protocols = new TestProtocol[]{leaf1, leaf2, parent1, parent2, root};
        
        ProtocolSubStack subStack1 = new ProtocolSubStack("test", messageFactory, Arrays.asList(leaf1, parent1));
        ProtocolSubStack subStack2 = new ProtocolSubStack("test", messageFactory, Arrays.asList(leaf2, parent2));
        
        MessageRouter router = new MessageRouter("test", messageFactory, Arrays.asList(subStack1, subStack2), 
            Arrays.asList(new Pair<ICondition<IMessage>, IReceiver>(new AllMessageFlagsRoutingCondition(MessageFlags.HIGH_PRIORITY), subStack1), 
                new Pair<ICondition<IMessage>, IReceiver>(new AllMessageFlagsRoutingCondition(MessageFlags.LOW_PRIORITY), subStack2)));
        
        ProtocolStack stack = new ProtocolStack("test", Arrays.asList(error, router, local, root), liveNodeManager, 100);
        stack.setTimeService(new SystemTimeService());
        
        stack.register(registry);
        stack.unregister(registry);
        
        stack.start();
        
        stack.onTimer(100);
        
        leaf1.send(messageFactory.create(member2, MessageFlags.LOW_PRIORITY));
        leaf1.send(messageFactory.create(member1, MessageFlags.LOW_PRIORITY));
        root.receive(messageFactory.create(member1, MessageFlags.HIGH_PRIORITY));
        root.receive(messageFactory.create(member1, MessageFlags.LOW_PRIORITY));
        root.receive(messageFactory.create(member1, MessageFlags.PARALLEL));
        
        stack.stop();
        
        for (TestProtocol protocol : protocols)
        {
            assertTrue(protocol.started);
            assertTrue(protocol.stopped);
            assertTrue(protocol.registered);
            assertTrue(protocol.unregistered);
            assertTrue(protocol.timered);
            assertTrue(protocol.cleanedup);
            assertTrue(protocol.getTimeService() != null);
        }
        
        assertTrue(leaf1.messages.size() == 1);
        assertEquals(leaf1.messages.get(0).getDestination(), member1);
        assertEquals(leaf1.messages.get(0).getFlags(), MessageFlags.HIGH_PRIORITY);
        
        assertTrue(leaf2.messages.size() == 2);
        assertEquals(leaf2.messages.get(0).getDestination(), member1);
        assertEquals(leaf2.messages.get(0).getFlags(), MessageFlags.LOW_PRIORITY);
        assertEquals(leaf2.messages.get(1).getDestination(), member1);
        assertEquals(leaf2.messages.get(1).getFlags(), MessageFlags.LOW_PRIORITY);
        
        assertTrue(root.messages.size() == 1);
        assertEquals(root.messages.get(0).getDestination(), member2);
        assertEquals(root.messages.get(0).getFlags(), MessageFlags.LOW_PRIORITY);
    }
    
    private static class TestProtocol extends AbstractProtocol
    {
        private boolean started;
        private boolean stopped;
        private boolean registered;
        private boolean unregistered;
        private boolean timered;
        private boolean cleanedup;

        public TestProtocol(String channelName, IMessageFactory messageFactory)
        {
            super(channelName, messageFactory);
        }
        
        @Override
        public void start()
        {
            super.start();
            started = true;
        }
        
        @Override
        public void stop()
        {
            stopped = true;
            super.stop();
        }
        
        @Override
        public void register(ISerializationRegistry registry)
        {
            registered = true;
        }
        
        @Override
        public void unregister(ISerializationRegistry registry)
        {
            unregistered = true;
        }
        
        @Override
        public void onTimer(long currentTime)
        {
            timered = true;
        }
        
        @Override
        public void cleanup(ILiveNodeProvider liveNodeProvider, long currentTime)
        {
            cleanedup = true;
        }
    }
    
    private static class TestLeafProtocol extends TestProtocol
    {
        private List<IMessage> messages = new ArrayList<IMessage>();
        
        public TestLeafProtocol(String channelName, IMessageFactory messageFactory)
        {
            super(channelName, messageFactory);
        }
        
        @Override
        public void start()
        {
            blankOffReceiver();
            super.start();
        }
        
        @Override
        protected void doReceive(IReceiver receiver, IMessage message)
        {
            messages.add(message);
        }
    }
    
    private static class TestRootProtocol extends TestProtocol
    {
        private List<IMessage> messages = new ArrayList<IMessage>();
        
        public TestRootProtocol(String channelName, IMessageFactory messageFactory)
        {
            super(channelName, messageFactory);
        }
        
        @Override
        public void start()
        {
            blankOffSender();
            super.start();
        }
        
        @Override
        protected void doSend(ISender sender, IMessage message)
        {
            messages.add(message);
        }
    }
}
