/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Test;

import com.exametrika.api.groups.core.IGroup;
import com.exametrika.api.groups.core.IGroupChannel;
import com.exametrika.api.groups.core.IMembership;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.Channel;
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
import com.exametrika.impl.groups.core.channel.GroupChannel;
import com.exametrika.impl.groups.core.channel.GroupChannelFactory;
import com.exametrika.impl.groups.core.channel.GroupChannelFactory.GroupFactoryParameters;
import com.exametrika.impl.groups.core.channel.GroupChannelFactory.GroupParameters;
import com.exametrika.impl.groups.core.discovery.WellKnownAddressesDiscoveryStrategy;
import com.exametrika.impl.groups.core.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.core.flush.IFlush;
import com.exametrika.impl.groups.core.flush.IFlushParticipant;
import com.exametrika.impl.groups.core.membership.Memberships;
import com.exametrika.impl.groups.core.multicast.RemoteFlowId;
import com.exametrika.spi.groups.IStateStore;
import com.exametrika.spi.groups.IStateTransferClient;
import com.exametrika.spi.groups.IStateTransferFactory;
import com.exametrika.spi.groups.IStateTransferServer;

/**
 * The {@link MulticastProtocolTests} are tests for flush.
 * 
 * @author Medvedev-A
 */
public class MulticastProtocolTests
{
    private static final int COUNT = 2;// TODO:10;
    private static final long SEND_COUNT = Long.MAX_VALUE;
    private Set<String> wellKnownAddresses= new HashSet<String>();
    private GroupFactoryParameters factoryParameters;
    private List<GroupParameters> parameters = new ArrayList<GroupParameters>();
    private IGroupChannel[] channels = new GroupChannel[COUNT];
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
        
        checkMembership(Collections.<Integer>asSet());
    }

    @Test
    public void testMultipleMulticasts() throws Exception
    {
        createParameters();
        for (TestMessageSender sender : messageSenders)
            sender.send = true;
        createGroup(Collections.<Integer>asSet());
         
        Threads.sleep(10000);
        
        checkMembership(Collections.<Integer>asSet());
    }
    @Test
    public void testPullableSender() throws Exception
    {
        createParameters();
        createGroup(Collections.<Integer>asSet());
         
        TestFeed feed = new TestFeed();
        ISink sink = channels[0].register(Memberships.CORE_GROUP_ADDRESS, feed);
        sink.setReady(true);
        
        Threads.sleep(10000);
        
        checkMembership(Collections.<Integer>asSet());
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
        
        checkMembership(Collections.<Integer>asSet());
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
        
        checkMembership(Collections.<Integer>asSet());
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
         
        checkMembership(Collections.<Integer>asSet(0, 1));
        
        channels[0].start();
        channels[1].start();
        IOs.close(channels[COUNT - 2]);
        IOs.close(channels[COUNT - 1]);
        
        Threads.sleep(10000);
        
        checkMembership(Collections.<Integer>asSet(COUNT - 2, COUNT - 1));
    }
    
    @Test
    public void testCoordinatorFailureBeforeFlush() throws Exception
    {
        createParameters();
        for (TestMessageSender sender : messageSenders)
            sender.send = true;
        createGroup(Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(Collections.<Integer>asSet(0, 1));

        channels[0].start();
        channels[1].start();
        
        IGroup group = flushParticipants.get(2).flush.getNewMembership().getGroup();
        int index = group.getMembers().indexOf(group.getCoordinator());
        Threads.sleep(1000);
        IOs.close(channels[index]);
        
        Threads.sleep(10000);
        
        checkMembership(Collections.<Integer>asSet(index));
    }
    
    @Test
    public void testCoordinatorFailureAfterFlush() throws Exception
    {
        createParameters();
        for (TestMessageSender sender : messageSenders)
            sender.send = true;
        createGroup(Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(Collections.<Integer>asSet(0, 1));

        failOnFlush();
        
        channels[0].start();
        channels[1].start();
        
        flushSequencer.waitAll(COUNT - 2, 5000, 0);
        IGroup group = flushParticipants.get(2).flush.getNewMembership().getGroup();
        int index = group.getMembers().indexOf(group.getCoordinator());
        
        IOs.close(channels[index]);
        
        Threads.sleep(10000);
        
        checkMembership(Collections.<Integer>asSet(index));
    }
    
    private void createGroup(Set<Integer> skipIndexes)
    {
        for (int i = 0; i < COUNT; i++)
        {
            TestGroupChannelFactory channelFactory = new TestGroupChannelFactory(factoryParameters);
            IGroupChannel channel = channelFactory.createChannel(parameters.get(i));
            if (!skipIndexes.contains(i))
            {
                channel.start();
                wellKnownAddresses.add(channel.getLiveNodeProvider().getLocalNode().getConnection(0));
            }
            channel.getCompartment().execute(messageSenders.get(i));
            channels[i] = channel;
        }
    }

    private void checkMembership(Set<Integer> skipIndexes)
    {
        IMembership membership = null;
        ByteArray state = null;
        Integer receivedCount = null;
        for (int i = 0; i < COUNT; i++)
        {
            if (skipIndexes.contains(i))
                continue;
            
            IMembership nodeMembership = channels[i].getMembershipService().getMembership();
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
            
            int received = messageSenders.get(i).receivedCount;
            if (receivedCount == null)
                receivedCount = received;
            else
                assertThat(receivedCount, is(received));
        }
        
        assertTrue(receivedCount > 0);
        assertThat(membership.getGroup().getMembers().size(), is(COUNT - skipIndexes.size()));
    }
    
    private void createFactoryParameters()
    {
        boolean debug = Debug.isDebug();
        factoryParameters = new GroupFactoryParameters(debug);
        if (!debug)
        {
            factoryParameters.heartbeatTrackPeriod = 100;
            factoryParameters.heartbeatPeriod = 100;
            factoryParameters.heartbeatStartPeriod = 300;
            factoryParameters.heartbeatFailureDetectionPeriod = 1000;
            factoryParameters.transportChannelTimeout = 1000;
        }
        factoryParameters.discoveryPeriod = 200;
        factoryParameters.groupFormationPeriod = 2000;
        factoryParameters.failureUpdatePeriod = 500;
        factoryParameters.failureHistoryPeriod = 10000;
        factoryParameters.maxShunCount = 3;
        factoryParameters.flushTimeout = 10000;
        factoryParameters.gracefulCloseTimeout = 10000;
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
            
            TestStateTransferFactory stateTransferFactory = new TestStateTransferFactory();
            stateTransferFactories.add(stateTransferFactory);
            
            GroupParameters parameters = new GroupParameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = sender;
            parameters.discoveryStrategy = new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses);
            parameters.stateStore = stateStore;
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
    
    private class TestStateTransferServer implements IStateTransferServer
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
        public void saveSnapshot(File file)
        {
            if (factory.state != null)
                Files.writeBytes(file, factory.state);
        }
    }
    
    private class TestStateTransferClient implements IStateTransferClient
    {
        TestStateTransferFactory factory;
        
        public TestStateTransferClient(TestStateTransferFactory factory)
        {
            this.factory = factory;
        }
        
        @Override
        public void loadSnapshot(File file)
        {
            factory.state = Files.readBytes(file);
        }
    }
    
    private class TestStateTransferFactory implements IStateTransferFactory
    {
        private ByteArray state;
        
        @Override
        public IStateTransferServer createServer()
        {
            return new TestStateTransferServer(this);
        }

        @Override
        public IStateTransferClient createClient()
        {
            return new TestStateTransferClient(this);
        }
    }
    
    private class TestStateStore implements IStateStore
    {
        private ByteArray buffer = createBuffer(17, 100000);
        
        @Override
        public void load(UUID id, File state)
        {
            if (id.equals(Memberships.CORE_GROUP_ID))
                Files.writeBytes(state, buffer);
            else
                Assert.error();
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
    
    private class TestMessageSender implements IReceiver, IDeliveryHandler, IFlowController<RemoteFlowId>, Runnable
    {
        public boolean sendBeforeGroup;
        public boolean send;
        private int index;
        private long count;
        private boolean flowLocked;
        private RemoteFlowId flow;
        private int receivedCount;

        public TestMessageSender(int index)
        {
            this.index = index;
        }
        
        @Override
        public void run()
        {
            if (!send)
                return;
            
            IGroupChannel channel = channels[index];
            if (sendBeforeGroup)
            {
                if (!flowLocked && count < SEND_COUNT)
                    channel.send(channel.getMessageFactory().create(Memberships.CORE_GROUP_ADDRESS, 
                        new TestBufferMessagePart(index, count++)));
            }
            else if (!flowLocked && channel.getMembershipService().getMembership() != null && count < SEND_COUNT)
                channel.send(channel.getMessageFactory().create(Memberships.CORE_GROUP_ADDRESS, 
                    new TestBufferMessagePart(index, count++)));
        }
        
        @Override
        public void receive(IMessage message)
        {
            if (message.getPart() instanceof TestBufferMessagePart)
            {
                TestBufferMessagePart part = message.getPart();
                ByteArray buffer = stateTransferFactories.get(index).state;
                long counter = Bytes.readLong(buffer.getBuffer(), buffer.getOffset());
                Bytes.writeLong(buffer.getBuffer(), buffer.getOffset(), counter + (part.index + 1) * part.value);
                receivedCount++;
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
        }
    }
    
    private static class TestFeed implements IFeed
    {
        private long count;
        
        @Override
        public void feed(ISink sink)
        {
            while (true)
            {
                IMessage message = sink.getMessageFactory().create(sink.getDestination(), new TestBufferMessagePart(0, count++));
                if (!sink.send(message))
                    break;
            }
        }
    }
    
    private class TestGroupChannelFactory extends GroupChannelFactory
    {
        public TestGroupChannelFactory(GroupFactoryParameters factoryParameters)
        {
            super(factoryParameters);
        }
        
        @Override
        protected void wireProtocols(Channel channel, TcpTransport transport, ProtocolStack protocolStack)
        {
            super.wireProtocols(channel, transport, protocolStack);
            
            FlushParticipantProtocol flushParticipantProtocol = protocolStack.find(FlushParticipantProtocol.class);
            TestFlushParticipant flushParticipant = new TestFlushParticipant();
            flushParticipants.add(flushParticipant);
            flushParticipantProtocol.getParticipants().add(flushParticipant);
        }
    }
}
