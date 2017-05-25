/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Test;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Bytes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.cluster.discovery.WellKnownAddressesDiscoveryStrategy;
import com.exametrika.impl.groups.cluster.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.cluster.flush.IFlush;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.multicast.RemoteFlowId;
import com.exametrika.spi.groups.cluster.state.IAsyncStateStore;
import com.exametrika.spi.groups.cluster.state.IAsyncStateTransferClient;
import com.exametrika.spi.groups.cluster.state.IAsyncStateTransferServer;
import com.exametrika.spi.groups.cluster.state.IStateStore;
import com.exametrika.spi.groups.cluster.state.IStateTransferFactory;
import com.exametrika.tests.groups.channel.TestGroupChannel;
import com.exametrika.tests.groups.channel.TestGroupChannelFactory;
import com.exametrika.tests.groups.channel.TestGroupFactoryParameters;
import com.exametrika.tests.groups.channel.TestGroupParameters;

/**
 * The {@link MulticastProtocolTests} are tests for multicast protocol.
 * 
 * @author Medvedev-A
 */
public class MulticastProtocolTests
{
    private static final int COUNT = 10;
    private static final long SEND_COUNT = 10;
    private Map<String, Object> wellKnownAddresses = new ConcurrentHashMap<String, Object>();
    private TestGroupFactoryParameters factoryParameters;
    private List<TestGroupParameters> parameters = new ArrayList<TestGroupParameters>();
    private TestGroupChannel[] channels = new TestGroupChannel[COUNT];
    private TestStateStore stateStore = new TestStateStore();
    private List<TestStateTransferFactory> stateTransferFactories = new ArrayList<TestStateTransferFactory>();
    private List<TestMessageSender> messageSenders = new ArrayList<TestMessageSender>();
    private List<TestFlushParticipant> flushParticipants = new ArrayList<TestFlushParticipant>();
    private Sequencer flushSequencer = new Sequencer();
   
    @After
    public void tearDown()
    {
        for (IChannel channel : channels)
            IOs.close(channel);
    }
    
    @Test
    public void testMulticast() throws Exception
    {
        createParameters();
        messageSenders.get(0).send = true;
        createGroup(Collections.<Integer>asSet());
         
        Threads.sleep(10000);
        
        checkMembership(Collections.<Integer>asSet(), true);
    }

    @Test
    public void testMultipleMulticasts() throws Exception
    {
        createParameters();
        for (TestMessageSender sender : messageSenders)
            sender.send = true;
        createGroup(Collections.<Integer>asSet());
         
        Threads.sleep(10000);
        
        checkMembership(Collections.<Integer>asSet(), true);
    }
    @Test
    public void testPullableSender() throws Exception
    {
        createParameters();
        createGroup(Collections.<Integer>asSet());
         
        TestFeed feed = new TestFeed(0);
        ISink sink = channels[0].register(GroupMemberships.CORE_GROUP_ADDRESS, feed);
        sink.setReady(true);
        
        Threads.sleep(10000);
        
        checkMembership(Collections.<Integer>asSet(), true);
    }
    
    @Test
    public void testFlowControl() throws Exception
    {
        createParameters();
        factoryParameters.minLockQueueCapacity = 1000;
        factoryParameters.maxUnlockQueueCapacity = 100;
        for (TestMessageSender sender : messageSenders)
            sender.send = true;
        createGroup(Collections.<Integer>asSet());
         
        Threads.sleep(10000);
        
        checkMembership(Collections.<Integer>asSet(), true);
        boolean locked = false;
        for (TestMessageSender sender : messageSenders)
        {
            if (sender.flow != null)
                locked = true;
        }
        assertTrue(locked);
    }
    
    @Test
    public void testSendBeforeGroup() throws Exception
    {
        createParameters();
        factoryParameters.minLockQueueCapacity = 1000;
        factoryParameters.maxUnlockQueueCapacity = 100;
        for (TestMessageSender sender : messageSenders)
        {
            sender.send = true;
            sender.sendBeforeGroup = true;
        }
        
        createGroup(Collections.<Integer>asSet());
         
        Threads.sleep(10000);
        
        checkMembership(Collections.<Integer>asSet(), true);
        boolean locked = false;
        for (TestMessageSender sender : messageSenders)
        {
            if (sender.flow != null)
                locked = true;
        }
        assertTrue(locked);
    }
    
    @Test
    public void testChangeMembership() throws Exception
    {
        createParameters();
        for (TestMessageSender sender : messageSenders)
            sender.send = true;
        createGroup(Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(Collections.<Integer>asSet(0, 1), true);
        
        channels[0].start();
        channels[1].start();
        IOs.close(channels[COUNT - 2]);
        IOs.close(channels[COUNT - 1]);
        
        Threads.sleep(10000);
        
        checkMembership(Collections.<Integer>asSet(COUNT - 2, COUNT - 1), false);
    }
    
    @Test
    public void testCoordinatorFailureBeforeFlush() throws Exception
    {
        createParameters();
        for (TestMessageSender sender : messageSenders)
            sender.send = true;
        createGroup(Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(Collections.<Integer>asSet(0, 1), true);

        channels[0].start();
        channels[1].start();
        
        IGroup group = flushParticipants.get(2).flush.getNewMembership().getGroup();
        int index = -1;
        for (int i = 0; i < channels.length; i++)
        {
            if (channels[i].getMembershipService().getLocalNode().equals(group.getCoordinator()))
            {
                index = i;
                break;
            }
        }
        
        Threads.sleep(1000);
        IOs.close(channels[index]);
        
        Threads.sleep(10000);
        
        checkMembership(Collections.<Integer>asSet(index), false);
    }
    
    @Test
    public void testCoordinatorFailureAfterFlush() throws Exception
    {
        createParameters();
        for (TestMessageSender sender : messageSenders)
            sender.send = true;
        createGroup(Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(Collections.<Integer>asSet(0, 1), true);

        failOnFlush();
        
        channels[0].start();
        channels[1].start();
        
        flushSequencer.waitAll(COUNT - 2, 5000, 0);
        IGroup group = flushParticipants.get(2).flush.getNewMembership().getGroup();
        int index = -1;
        for (int i = 0; i < channels.length; i++)
        {
            if (channels[i].getMembershipService().getLocalNode().equals(group.getCoordinator()))
            {
                index = i;
                break;
            }
        }
        
        IOs.close(channels[index]);
        
        Threads.sleep(10000000);//TODO:
        
        checkMembership(Collections.<Integer>asSet(index), false);
    }
    
    private void createGroup(Set<Integer> skipIndexes)
    {
        for (int i = 0; i < COUNT; i++)
        {
            TestMulticastGroupChannelFactory channelFactory = new TestMulticastGroupChannelFactory(factoryParameters);
            TestGroupChannel channel = channelFactory.createChannel(parameters.get(i));
            if (!skipIndexes.contains(i))
            {
                channel.start();
                wellKnownAddresses.put(channel.getLiveNodeProvider().getLocalNode().getConnection(0), new Object());
            }

            channel.getCompartment().addTimerProcessor(messageSenders.get(i));
            channels[i] = channel;
        }
    }

    private void checkMembership(Set<Integer> skipIndexes, boolean checkMessages)
    {
        IGroupMembership membership = null;
        ByteArray state = null;
        Map<IAddress, List<Integer>> receivedMap = null;
        for (int i = 0; i < COUNT; i++)
        {
            if (skipIndexes.contains(i))
                continue;
            
            IGroupMembership nodeMembership = channels[i].getMembershipService().getMembership();
            if (membership == null)
                membership = nodeMembership;
            
            assertThat(membership, is(nodeMembership));
            assertThat(membership.getGroup(), is(nodeMembership.getGroup()));
            assertThat(membership.getGroup().getMembers(), is(nodeMembership.getGroup().getMembers()));
            assertThat(membership.getGroup().isPrimary(), is(nodeMembership.getGroup().isPrimary()));
            assertThat(membership.getGroup().getName(), is(nodeMembership.getGroup().getName()));
            assertThat(membership.getGroup().getAddress(), is(nodeMembership.getGroup().getAddress()));
            assertThat(membership.getGroup().getCoordinator(), is(nodeMembership.getGroup().getCoordinator()));
            assertThat(membership.getGroup().findMember(channels[i].getMembershipService().getLocalNode().getId()), 
                is(channels[i].getMembershipService().getLocalNode()));
            
            ByteArray nodeState = stateTransferFactories.get(i).state;
            if (state == null)
                state = nodeState;
            else
                assertThat(nodeState, is(state));
            
            if (checkMessages)
            {
                Map<IAddress, List<Integer>> received = buildReceivedMap(messageSenders.get(i).receivedMessagesMap);
                if (receivedMap == null)
                    receivedMap = received;
                else
                    assertThat(receivedMap, is(received));
                
                assertThat(messageSenders.get(i).deliveredMessages.size(), is((int)messageSenders.get(i).count));
                for (int k = 0; k < messageSenders.size(); k++)
                {
                    TestMessageSender sender = messageSenders.get(k);
                    if (!skipIndexes.contains(k))
                    {
                        int receivedCount = 0;
                        synchronized (sender)
                        {
                            List<IMessage> receivedMessages = sender.receivedMessagesMap.get(channels[i].getLiveNodeProvider().getLocalNode());
                            if (receivedMessages != null)
                                receivedCount = receivedMessages.size();
                        }
                        assertThat(receivedCount, is(messageSenders.get(i).deliveredMessages.size()));
                    }
                }
            }
        }
        
        if (checkMessages)
            assertTrue(!receivedMap.isEmpty());
        
        assertThat(membership.getGroup().getMembers().size(), is(COUNT - skipIndexes.size()));
    }
    
    private Map<IAddress, List<Integer>> buildReceivedMap(Map<IAddress, List<IMessage>> received)
    {
        Map<IAddress, List<Integer>> receivedMap = new HashMap<IAddress, List<Integer>>();
        for (Map.Entry<IAddress, List<IMessage>> entry : received.entrySet())
        {
            if (entry.getValue().isEmpty())
                continue;
            
            List<Integer> receivedMessages = receivedMap.get(entry.getKey());
            if (receivedMessages == null)
            {
                receivedMessages = new ArrayList<Integer>();
                receivedMap.put(entry.getKey(), receivedMessages);
            }
            for (IMessage message : entry.getValue())
            {
                TestBufferMessagePart part = message.getPart();
                receivedMessages.add((int)part.value);
            }
        }
        
        return receivedMap;
    }
    
    private void createFactoryParameters()
    {
        boolean debug = Debug.isDebug();
        factoryParameters = new TestGroupFactoryParameters(debug);
        if (!debug)
        {
            factoryParameters.heartbeatTrackPeriod = 100;
            factoryParameters.heartbeatPeriod = 100;
            factoryParameters.heartbeatStartPeriod = 300;
            factoryParameters.heartbeatFailureDetectionPeriod = 5000;
            factoryParameters.transportChannelTimeout = 5000;
        }
        factoryParameters.discoveryPeriod = 200;
        factoryParameters.groupFormationPeriod = 2000;
        factoryParameters.failureUpdatePeriod = 500;
        factoryParameters.failureHistoryPeriod = 10000;
        factoryParameters.maxShunCount = 3;
        factoryParameters.flushTimeout = 10000;
        factoryParameters.gracefulExitTimeout = 10000;
        factoryParameters.maxStateTransferPeriod = Integer.MAX_VALUE;
        factoryParameters.stateSizeThreshold = 100000;
        factoryParameters.saveSnapshotPeriod = 10000;
        factoryParameters.transferLogRecordPeriod = 1000;
        factoryParameters.transferLogMessagesCount = 2;
        factoryParameters.minLockQueueCapacity = 10000000;
        factoryParameters.dataExchangePeriod = 200;
        factoryParameters.maxBundlingMessageSize = 1000;
        factoryParameters.maxBundlingPeriod = 100;
        factoryParameters.maxBundleSize = 10000;
        factoryParameters.maxTotalOrderBundlingMessageCount = 10;
        factoryParameters.maxUnacknowledgedPeriod = 100;
        factoryParameters.maxUnacknowledgedMessageCount = 100;
        factoryParameters.maxIdleReceiveQueuePeriod = 600000;
        factoryParameters.maxUnlockQueueCapacity = 100000;
    }
    
    private void createParameters()
    {
        createFactoryParameters();
        for (int i = 0; i < COUNT; i++)
        {
            TestMessageSender sender = new TestMessageSender(i);
            messageSenders.add(sender);
            
            TestStateTransferFactory stateTransferFactory = new TestStateTransferFactory(stateStore);
            stateTransferFactories.add(stateTransferFactory);
            
            TestGroupParameters parameters = new TestGroupParameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = sender;
            parameters.discoveryStrategy = new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses.keySet());
            parameters.stateTransferFactory = stateTransferFactory;
            parameters.deliveryHandler = sender;
            parameters.localFlowController = sender;
            parameters.serializationRegistrars.add(new TestBufferMessagePartSerializer());
            
            this.parameters.add(parameters);
        }
    }
    
    private ByteArray createBuffer(int base, int length)
    {
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++)
            buffer[i] = (byte)(base + i);
        
        return new ByteArray(buffer);
    }
    
    private void failOnFlush()
    {
        for (TestFlushParticipant participant : flushParticipants)
            participant.failOnFlush = true;
    }
    
    private class TestStateTransferServer implements IAsyncStateTransferServer
    {
        TestStateTransferFactory factory;
        
        public TestStateTransferServer(TestStateTransferFactory factory)
        {
            this.factory = factory;
        }
        
        @Override
        public MessageType classifyMessage(IMessage message)
        {
            if (message.getPart() instanceof TestBufferMessagePart)
                return MessageType.STATE_WRITE;
            else
                return MessageType.NON_STATE;
        }

        @Override
        public void saveSnapshot(boolean full, File file)
        {
            if (factory.state != null)
                Files.writeBytes(file, factory.state);
        }
    }
    
    private class TestStateTransferClient implements IAsyncStateTransferClient
    {
        TestStateTransferFactory factory;
        
        public TestStateTransferClient(TestStateTransferFactory factory)
        {
            this.factory = factory;
        }
        
        @Override
        public void loadSnapshot(boolean full, File file)
        {
            factory.state = Files.readBytes(file);
        }
    }
    
    private class TestStateTransferFactory implements IStateTransferFactory
    {
        private ByteArray state;
        private IAsyncStateStore stateStore;
        
        public TestStateTransferFactory(IAsyncStateStore stateStore)
        {
            this.stateStore = stateStore;
        }
        
        @Override
        public IAsyncStateTransferServer createServer(UUID groupId)
        {
            return new TestStateTransferServer(this);
        }

        @Override
        public IAsyncStateTransferClient createClient(UUID groupId)
        {
            return new TestStateTransferClient(this);
        }

        @Override
        public IStateStore createStore(UUID groupId)
        {
            return stateStore;
        }
    }
    
    private class TestStateStore implements IAsyncStateStore
    {
        private ByteArray buffer = createBuffer(17, 100000);
        
        @Override
        public boolean load(UUID id, File state)
        {
            if (id.equals(GroupMemberships.CORE_GROUP_ID))
                Files.writeBytes(state, buffer);
            else
                Assert.error();
            
            return true;
        }

        @Override
        public void save(UUID id, File state)
        {
        }
    }
    
    private static final class TestBufferMessagePart implements IMessagePart
    {
        private final int index;
        private final long value;

        public TestBufferMessagePart(int index, long value)
        {
            this.index = index;
            this.value = value;
        }
        
        @Override
        public int getSize()
        {
            return 12;
        }
        
        @Override
        public String toString()
        {
            return Integer.toString(index) + ":" + Long.toString(value);
        }
    }
    
    private static final class TestBufferMessagePartSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("b9ca8da1-e56d-475f-b59e-220baa7d2d19");
     
        public TestBufferMessagePartSerializer()
        {
            super(ID, TestBufferMessagePart.class);
        }

        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            TestBufferMessagePart part = (TestBufferMessagePart)object;

            serialization.writeInt(part.index);
            serialization.writeLong(part.value);
        }
        
        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            int index = deserialization.readInt();
            long value = deserialization.readLong();
            return new TestBufferMessagePart(index, value);
        }
    }
    
    private class TestFlushParticipant implements IFlushParticipant
    {
        public boolean failOnFlush;
        private IFlush flush;
        
        @Override
        public boolean isFlushProcessingRequired()
        {
            return false;
        }

        @Override
        public void setCoordinator()
        {
        }

        @Override
        public void startFlush(IFlush flush)
        {
            this.flush = flush;
            flush.grantFlush(this);
            if (failOnFlush)
                flushSequencer.allowSingle();
        }

        @Override
        public void beforeProcessFlush()
        {
        }

        @Override
        public void processFlush()
        {
            Assert.error();
        }

        @Override
        public void endFlush()
        {
        }
    }
    
    private class TestMessageSender implements IReceiver, IDeliveryHandler, IFlowController<RemoteFlowId>, ICompartmentTimerProcessor
    {
        public boolean sendBeforeGroup;
        public boolean send;
        private int index;
        private long count;
        private boolean flowLocked;
        private RemoteFlowId flow;
        private Map<IAddress, List<IMessage>> receivedMessagesMap = new HashMap<IAddress, List<IMessage>>();
        private List<IMessage> deliveredMessages = new ArrayList<IMessage>();

        public TestMessageSender(int index)
        {
            this.index = index;
        }
        
        @Override
        public synchronized void onTimer(long currentTime)
        {
            if (!send)
                return;
            
            TestGroupChannel channel = channels[index];
            if (sendBeforeGroup)
            {
                if (!flowLocked && count < SEND_COUNT)
                    channel.send(channel.getMessageFactory().create(GroupMemberships.CORE_GROUP_ADDRESS, 
                        new TestBufferMessagePart(index, count++)));
            }
            else if (!flowLocked && channel.getMembershipService().getMembership() != null && count < SEND_COUNT)
                channel.send(channel.getMessageFactory().create(GroupMemberships.CORE_GROUP_ADDRESS, 
                    new TestBufferMessagePart(index, count++)));
        }
        
        @Override
        public synchronized void receive(IMessage message)
        {
            if (message.getPart() instanceof TestBufferMessagePart)
            {
                TestBufferMessagePart part = message.getPart();
                List<IMessage> receivedMessages = receivedMessagesMap.get(message.getSource());
                if (receivedMessages == null)
                {
                    receivedMessages = new ArrayList<IMessage>();
                    receivedMessagesMap.put(message.getSource(), receivedMessages);
                }
                
                assertThat(part.value, is((long)receivedMessages.size()));
                ByteArray buffer = stateTransferFactories.get(index).state;
                long counter = Bytes.readLong(buffer.getBuffer(), buffer.getOffset());
                Bytes.writeLong(buffer.getBuffer(), buffer.getOffset(), counter + (part.index + 1) * part.value);
                receivedMessages.add(message);
            }
        }

        @Override
        public void lockFlow(RemoteFlowId flow)
        {
            flowLocked = true;
            this.flow = flow;
        }

        @Override
        public void unlockFlow(RemoteFlowId flow)
        {
            flowLocked = false;
        }

        @Override
        public void onDelivered(IMessage message)
        {
            TestBufferMessagePart part = message.getPart();
            assertThat(part.value, is((long)deliveredMessages.size()));
            deliveredMessages.add(message);
        }
    }
    
    private class TestFeed implements IFeed
    {
        private final int index;
        
        public TestFeed(int index)
        {
            this.index = index;
        }
        
        @Override
        public void feed(ISink sink)
        {
            TestMessageSender sender = messageSenders.get(index);
            if (sender.count < SEND_COUNT)
            {
                IMessage message = sink.getMessageFactory().create(sink.getDestination(), new TestBufferMessagePart(0, sender.count++));
                sink.send(message);
            }
        }
    }
    
    private class TestMulticastGroupChannelFactory extends TestGroupChannelFactory
    {
        public TestMulticastGroupChannelFactory(TestGroupFactoryParameters factoryParameters)
        {
            super(factoryParameters);
        }
        
        @Override
        protected void wireProtocols(IChannel channel, TcpTransport transport, ProtocolStack protocolStack)
        {
            super.wireProtocols(channel, transport, protocolStack);
            
            FlushParticipantProtocol flushParticipantProtocol = protocolStack.find(FlushParticipantProtocol.class);
            TestFlushParticipant flushParticipant = new TestFlushParticipant();
            flushParticipants.add(flushParticipant);
            flushParticipantProtocol.getParticipants().add(flushParticipant);
        }
    }
}
