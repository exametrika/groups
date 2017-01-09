/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.SerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IPullableSender;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.message.Message;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ICleanupManager;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.protocols.trace.TracingProtocol;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Pair;


/**
 * The {@link AbstractProtocolTests} are tests for {@link AbstractProtocol} implementations.
 * 
 * @see AbstractProtocol
 * @see TracingProtocol
 * @author Medvedev-A
 */
public class AbstractProtocolTests
{
    @Test
    public void testProtocol()
    {
        SenderMock sender = new SenderMock();
        ReceiverMock receiver = new ReceiverMock();
        PullableSenderMock pullableSender = new PullableSenderMock();
        
        final ProtocolMock protocol = new ProtocolMock("test");
        protocol.setSender(sender);
        protocol.setReceiver(receiver);
        protocol.setPullableSender(pullableSender);
        
        IAddress node1 = new TestAddress(UUID.randomUUID(), "node1");
        IAddress node2 = new TestAddress(UUID.randomUUID(), "node2");
        IAddress destination1 = node1;
        IAddress destination2 = node2;
        IMessage message1 = new Message(destination1, node1, new MessagePart("value1"), new SerializationRegistry());
        final IMessage message2 = new Message(destination1, node1, new MessagePart("value2"), new SerializationRegistry());
        final IMessage message3 = new Message(destination2, node2, new MessagePart("value3"), new SerializationRegistry());
        
        protocol.send(message1);
        
        assertThat(sender.messages.size(), is(1));
        assertThat(sender.messages.get(0), is(message1));
        
        protocol.receive(message1);
        
        assertThat(receiver.messages.size(), is(1));
        assertThat(receiver.messages.get(0), is(message1));
        
        ISink sink1 = protocol.register(destination1, new IFeed()
        {
            @Override
            public void feed(ISink sink)
            {
                sink.setReady(true);
                assertThat(sink.send(message2), is(!protocol.sendPending));
                
                if (protocol.sendPending)
                    sink.setReady(false);
            }
        });
        ISink sink2 = protocol.register(destination2, new IFeed()
        {
            @Override
            public void feed(ISink sink)
            {
                sink.setReady(false);
                assertThat(sink.send(message3), is(true));
            }
        });
        pullableSender.send(destination1);
        pullableSender.send(destination2);
        
        assertThat(pullableSender.messages.size(), is(2));
        assertThat(pullableSender.readinesses.size(), is(2));
        assertThat(pullableSender.messages.get(0), is(new Pair<IAddress, IMessage>(destination1, message2)));
        assertThat(pullableSender.readinesses.get(0), is(new Pair<IAddress, Boolean>(destination1, true)));
        assertThat(pullableSender.messages.get(1), is(new Pair<IAddress, IMessage>(destination2, message3)));
        assertThat(pullableSender.readinesses.get(1), is(new Pair<IAddress, Boolean>(destination2, false)));
        assertThat(pullableSender.sinks.size(), is(2));
        
        pullableSender.messages.clear();
        pullableSender.readinesses.clear();
        
        protocol.sendPending = true;
        pullableSender.setFull(sink1, true);
        pullableSender.send(destination1);
        protocol.sendPending = false;
        pullableSender.setFull(sink1, false);
        pullableSender.send(destination1);
        
        assertThat(pullableSender.messages.size(), is(2));
        assertThat(pullableSender.messages.get(0), is(new Pair<IAddress, IMessage>(destination1, message2)));
        assertThat(pullableSender.messages.get(1), is(new Pair<IAddress, IMessage>(destination1, message2)));
        assertThat(pullableSender.readinesses.size(), is(4));
        assertThat(pullableSender.readinesses.get(0), is(new Pair<IAddress, Boolean>(destination1, true)));
        assertThat(pullableSender.readinesses.get(1), is(new Pair<IAddress, Boolean>(destination1, true)));
        assertThat(pullableSender.readinesses.get(2), is(new Pair<IAddress, Boolean>(destination1, true)));
        assertThat(pullableSender.readinesses.get(3), is(new Pair<IAddress, Boolean>(destination1, false)));
        
        pullableSender.messages.clear();
        pullableSender.readinesses.clear();
        
        protocol.unregister(sink1);
        protocol.unregister(sink2);
        
        assertThat(pullableSender.sinks.isEmpty(), is(true));
        
        pullableSender.send(destination1);
        pullableSender.send(destination2);
        
        assertThat(pullableSender.messages.isEmpty(), is(true));
        assertThat(pullableSender.readinesses.isEmpty(), is(true));
    }
    
    @Test
    public void testProtocolStack()
    {
        TestLiveNodeProvider liveNodeProvider = new TestLiveNodeProvider();
        TestTimeService timeService = new TestTimeService();
        timeService.time = 1000;
        
        ReceiverMock receiver = new ReceiverMock();
        SenderMock sender = new SenderMock();
        ProtocolMock protocol1 = new ProtocolMock("protocol1");
        ProtocolMock protocol2 = new ProtocolMock("protocol2");
        ProtocolStack stack = new ProtocolStack("test", Arrays.<AbstractProtocol>asList(protocol1, protocol2), liveNodeProvider, 100, 1000);
        stack.setTimeService(timeService);
        
        assertThat(stack.getFirst() == protocol1, is(true));
        assertThat(stack.getLast() == protocol2, is(true));
        assertThat(protocol1.getTimeService() == timeService, is(true));
        assertThat(protocol2.getTimeService() == timeService, is(true));
        
        stack.getFirst().setReceiver(receiver);
        stack.getLast().setSender(sender);
        
        stack.start();
        
        assertThat(protocol1.getProtocolSender() == protocol2, is(true));
        assertThat(protocol1.getProtocolPullableSender() == protocol2, is(true));
        assertThat(protocol2.getProtocolReceiver() == protocol1, is(true));
        assertThat(protocol1.started, is(true));
        assertThat(protocol2.started, is(true));
        
        stack.onTimer(timeService.time);
        assertThat(protocol1.timer, is(true));
        assertThat(protocol2.timer, is(true));
        
        ISerializationRegistry registry = new SerializationRegistry();
        stack.register(registry);
        stack.unregister(registry);
        
        assertThat(protocol1.registered, is(true));
        assertThat(protocol2.registered, is(true));
        assertThat(protocol1.unregistered, is(true));
        assertThat(protocol2.unregistered, is(true));
        
        assertThat(protocol1.cleanup, is(true));
        assertThat(protocol2.cleanup, is(true));
        protocol1.cleanup = false;
        protocol2.cleanup = false;
        
        timeService.time = 1050;
        
        stack.onTimer(timeService.time);
        
        assertThat(protocol1.cleanup, is(false));
        assertThat(protocol2.cleanup, is(false));
        
        timeService.time = 3000;
        
        stack.onTimer(timeService.time);
        
        assertThat(protocol1.cleanup, is(true));
        assertThat(protocol2.cleanup, is(true));
        protocol1.cleanup = false;
        protocol2.cleanup = false;
        
        stack.stop();
            
        assertThat(protocol1.stopped, is(true));
        assertThat(protocol2.stopped, is(true));
    }
    
    private static class TestTimeService implements ITimeService
    {
        private long time;
        
        @Override
        public long getCurrentTime()
        {
            return time;
        }
    }
    
    public static class TestLiveNodeProvider implements ILiveNodeProvider
    {
        long id;
        public List<IAddress> liveNodes;
        
        @Override
        public long getId()
        {
            return id;
        }

        @Override
        public IAddress getLocalNode()
        {
            return null;
        }

        @Override
        public List<IAddress> getLiveNodes()
        {
            return liveNodes;
        }

        @Override
        public boolean isLive(IAddress node)
        {
            if (liveNodes == null)
                return false;
            else
                return liveNodes.contains(node);
        }

        @Override
        public IAddress findById(UUID id)
        {
            return null;
        }

        @Override
        public IAddress findByName(String name)
        {
            return null;
        }
        
        @Override
        public IAddress findByConnection(String connection)
        {
            return null;
        }
    }
    
    private static class MessagePart implements IMessagePart
    {
        private final String value;

        public MessagePart(String value)
        {
            this.value = value;
        }
        
        @Override
        public String toString()
        {
            return value;
        }
        
        @Override
        public int getSize()
        {
            return value.length() * 2;
        }
    }
    
    public static class ProtocolMock extends AbstractProtocol
    {
        List<IMessage> messages = new ArrayList<IMessage>();
        private boolean timer;
        private boolean registered;
        private boolean unregistered;
        public boolean cleanup;
        private boolean stopped;
        private boolean started;
        private boolean sendPending;
        private IMessage pendingMessage;
        
        public ProtocolMock(String channelName)
        {
            super(channelName, new MessageFactory(new SerializationRegistry(), new LiveNodeManager(channelName, 
                Arrays.<IFailureObserver>asList(new ChannelObserver(channelName)), new ChannelObserver(channelName))));
        }
        
        public ISender getProtocolSender()
        {
            return getSender();
        }
        
        public IReceiver getProtocolReceiver()
        {
            return getReceiver();
        }
        
        public IPullableSender getProtocolPullableSender()
        {
            return getPullableSender();
        }
        
        @Override
        public void start()
        {
            started = true;
        }
        
        @Override
        public void stop()
        {
            stopped = true;
        }
        
        @Override
        public void onTimer(long currentTime)
        {
            timer = true;
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
        public void cleanup(ICleanupManager cleanupManager, ILiveNodeProvider liveNodeProvider, long currentTime)
        {
            cleanup = true;
        }

        @Override
        protected boolean supportsPullSendModel()
        {
            return true;
        }
        
        @Override
        protected void doSend(ISender sender, IMessage message)
        {
            messages.add(message);
            super.doSend(sender, message);
        }
        
        @Override
        protected boolean doSend(IFeed feed, ISink sink, IMessage message)
        {
            messages.add(message);
            
            if (sendPending)
            {
                assertThat(super.doSend(feed, sink, message), is(false));
                pendingMessage = message;
                return false;
            }
            else
                assertThat(super.doSend(feed, sink, message), is(true));
            return true;
        }
        
        @Override
        protected boolean doSendPending(IFeed feed, ISink sink)
        {
            assertThat(sink.send(pendingMessage), is(true));
            pendingMessage = null;
            return true;
        }
        
        @Override
        protected void doReceive(IReceiver receiver, IMessage message)
        {
            messages.add(message);
            super.doReceive(receiver, message);
        }
    }
}
