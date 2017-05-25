/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelFactoryParameters;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.HeartbeatProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeTrackingStrategy;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ConnectionManager;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Bytes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.cluster.discovery.CoreGroupDiscoveryProtocol;
import com.exametrika.impl.groups.cluster.discovery.WellKnownAddressesDiscoveryStrategy;
import com.exametrika.impl.groups.cluster.failuredetection.CoreGroupFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.GroupNodeTrackingStrategy;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.flush.FlushCoordinatorProtocol;
import com.exametrika.impl.groups.cluster.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.cluster.flush.IFlush;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipTracker;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;
import com.exametrika.impl.groups.cluster.multicast.FlowControlProtocol;
import com.exametrika.impl.groups.cluster.multicast.RemoteFlowId;
import com.exametrika.impl.groups.cluster.state.SimpleStateTransferClientProtocol;
import com.exametrika.impl.groups.cluster.state.SimpleStateTransferServerProtocol;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;
import com.exametrika.spi.groups.cluster.discovery.IDiscoveryStrategy;
import com.exametrika.spi.groups.cluster.state.ISimpleStateStore;
import com.exametrika.spi.groups.cluster.state.ISimpleStateTransferClient;
import com.exametrika.spi.groups.cluster.state.ISimpleStateTransferServer;
import com.exametrika.spi.groups.cluster.state.IStateStore;
import com.exametrika.spi.groups.cluster.state.IStateTransferClient;
import com.exametrika.spi.groups.cluster.state.IStateTransferFactory;
import com.exametrika.spi.groups.cluster.state.IStateTransferServer;
import com.exametrika.tests.common.messaging.ReceiverMock;
import com.exametrika.tests.groups.channel.TestGroupChannel;
import com.exametrika.tests.groups.mocks.PropertyProviderMock;

/**
 * The {@link SimpleStateTransferProtocolTests} are tests for simple state transfer.
 * 
 * @author Medvedev-A
 */
public class SimpleStateTransferProtocolTests
{
    private static final int COUNT = 10;
    private TestGroupChannel[] channels = new TestGroupChannel[COUNT];
    private Sequencer flushSequencer = new Sequencer();
    private Sequencer snapshotSequencer = new Sequencer();
    
    @After
    public void tearDown()
    {
        for (IChannel channel : channels)
            IOs.close(channel);
    }
    
    @Test
    public void testGroupFormation()
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet());
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet());
    }

    @Test
    public void testGroupFormationNonCoordinatorFailure() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet());
         
        failOnFlush(channelFactory);
        flushSequencer.waitAll(COUNT, 5000, 0);
        int coordinatorNodeIndex = findNodeIndex(0, channelFactory.messageSenders.get(0).flush.getNewMembership().getGroup().getCoordinator());
        int[] nodes = selectNodes(0, COUNT, 2, coordinatorNodeIndex);
        CoreGroupFailureDetectionProtocolTests.failChannel(channels[nodes[0]]);
        IOs.close(channels[nodes[1]]);
        
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.asSet(nodes[0], nodes[1]));
    }
    
    @Test
    public void testGroupFormationCoordinatorFailure() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet());
         
        failOnFlush(channelFactory);
        flushSequencer.waitAll(COUNT, 5000, 0);
        int coordinatorNodeIndex = findNodeIndex(0, channelFactory.messageSenders.get(0).flush.getNewMembership().getGroup().getCoordinator());
        IOs.close(channels[coordinatorNodeIndex]);
        
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.asSet(coordinatorNodeIndex));
    }

    @Test
    public void testStateTransfer() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet(0, 1));
        
        channels[0].start();
        channels[1].start();
        
        Threads.sleep(10000);
        
        checkMembership(channelFactory, Collections.<Integer>asSet());
    }
    
    @Test
    public void testClientFailureBeforeFlush() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet(0, 1));

        trackSnapshot(channelFactory);
        channels[0].start();
        channels[1].start();
        
        snapshotSequencer.waitAll(2, 5000, 0);
        
        Threads.sleep(1000);
        IOs.close(channels[0]);
        
        Threads.sleep(10000);
        
        checkMembership(channelFactory, Collections.<Integer>asSet(0));
    }
    
    @Test
    public void testClientFailureAfterFlush() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet(0, 1));

        failOnFlush(channelFactory);
        
        channels[0].start();
        channels[1].start();
        
        flushSequencer.waitAll(COUNT - 2, 5000, 0);
        
        IOs.close(channels[0]);
        
        Threads.sleep(10000);
        
        checkMembership(channelFactory, Collections.<Integer>asSet(0));
    }
    
    @Test
    public void testCoordinatorFailureBeforeFlush() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet(0, 1));

        trackSnapshot(channelFactory);
        channels[0].start();
        channels[1].start();
        
        snapshotSequencer.waitAll(2, 5000, 0);
        IGroup group = channelFactory.messageSenders.get(2).membership.getGroup();
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
        
        checkMembership(channelFactory, Collections.<Integer>asSet(index));
    }
    
    @Test
    public void testCoordinatorFailureAfterFlush() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet(0, 1));

        failOnFlush(channelFactory);
        
        channels[0].start();
        channels[1].start();
        
        flushSequencer.waitAll(COUNT - 2, 5000, 0);
        IGroup group = channelFactory.messageSenders.get(2).membership.getGroup();
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
        
        Threads.sleep(10000);
        
        checkMembership(channelFactory, Collections.<Integer>asSet(index));
    }
    
    @Test @Ignore
    public void testGroupFailureBeforeFlush() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet(0, 1));

        trackSnapshot(channelFactory);
        channels[0].start();
        channels[1].start();
        
        snapshotSequencer.waitAll(2, 5000, 0);
        Set<Integer> skipIndexes = new HashSet<Integer>();
        for (int i = 2; i < COUNT; i++)
        {
            IOs.close(channels[i]);
            skipIndexes.add(i);
        }
        
        Threads.sleep(10000);
        
        checkMembership(channelFactory, skipIndexes);
    }
    
    @Test @Ignore
    public void testGroupFailureAfterFlush() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet(0, 1));

        failOnFlush(channelFactory);
        
        channels[0].start();
        channels[1].start();
        
        flushSequencer.waitAll(COUNT - 2, 5000, 0);
        Set<Integer> skipIndexes = new HashSet<Integer>();
        for (int i = 2; i < COUNT; i++)
        {
            IOs.close(channels[i]);
            skipIndexes.add(i);
        }
        
        Threads.sleep(10000);
        
        checkMembership(channelFactory, skipIndexes);
    }
    
    @Test
    public void testSaveStateInExternalStore() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet());
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet());
        
        Threads.sleep(1000);
        disableSend(channelFactory);
        
        Threads.sleep(5000);
        
        assertThat(channelFactory.stateStore.savedBuffer, is(channelFactory.stateTransferFactories.get(0).state));
    }
    
    private void createGroup(Set<String> wellKnownAddresses, TestChannelFactory channelFactory, Set<Integer> skipIndexes)
    {
        for (int i = 0; i < COUNT; i++)
        {
            ChannelParameters parameters = new ChannelParameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            if (!skipIndexes.contains(i))
            {
                channel.start();
                wellKnownAddresses.add(channel.getLiveNodeProvider().getLocalNode().getConnection(0));
            }
            channels[i] = (TestGroupChannel)channel;
        }
    }

    private void checkMembership(TestChannelFactory channelFactory, Set<Integer> skipIndexes)
    {
        IGroupMembership membership = null;
        ByteArray state = null;
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
            
            ByteArray nodeState = channelFactory.stateTransferFactories.get(i).state;
            if (state == null)
                state = nodeState;
            else
                assertThat(nodeState, is(state));
        }
        
        assertThat(membership.getGroup().getMembers().size(), is(COUNT - skipIndexes.size()));
    }
    
    private int findNodeIndex(int startWith, INode node)
    {
        for (int i = startWith; i < COUNT; i++)
        {
            if (channels[i].getMembershipService().getLocalNode().equals(node))
                return i;
        }
        Assert.error();
        return 0;
    }
    
    private int[] selectNodes(int start, int count, int selectCount, int coordinator)
    {
        int[] indexes = new int[selectCount];
        for (int i = start; i < count; i++)
        {
            if (selectCount == 0)
                break;
            
            if (i != coordinator)
            {
                indexes[selectCount - 1] = i;
                selectCount--;
            }
        }
        
        return indexes;
    }
    
    private ByteArray createBuffer(int base, int length)
    {
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++)
            buffer[i] = (byte)(base + i);
        
        return new ByteArray(buffer);
    }
    
    private void failOnFlush(TestChannelFactory channelFactory)
    {
        for (TestMessageSender sender : channelFactory.messageSenders)
            sender.failOnFlush = true;
    }
    
    private void disableSend(TestChannelFactory channelFactory)
    {
        for (TestMessageSender sender : channelFactory.messageSenders)
            sender.disableSend = true;
    }
    
    private void trackSnapshot(TestChannelFactory channelFactory)
    {
        for (TestStateTransferFactory factory : channelFactory.stateTransferFactories)
            factory.trackSnapshot = true;    
    }
    
    private class TestStateTransferServer implements ISimpleStateTransferServer
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
        public ByteArray saveSnapshot(boolean full)
        {
            if (factory.state != null)
                return factory.state.clone();
            else 
                return new ByteArray(0);
        }
    }
    
    private class TestStateTransferClient implements ISimpleStateTransferClient
    {
        TestStateTransferFactory factory;
        
        public TestStateTransferClient(TestStateTransferFactory factory)
        {
            this.factory = factory;
        }
        
        @Override
        public void loadSnapshot(boolean full, ByteArray state)
        {
            factory.state = state.clone();
            if (factory.trackSnapshot)
                snapshotSequencer.allowSingle();
        }
    }
    
    private class TestStateTransferFactory implements IStateTransferFactory
    {
        private boolean trackSnapshot;
        private ByteArray state;
        private TestStateStore stateStore;
        
        public TestStateTransferFactory(TestStateStore stateStore)
        {
            this.stateStore = stateStore;
        }
        
        @Override
        public IStateTransferServer createServer(UUID groupId)
        {
            return new TestStateTransferServer(this);
        }

        @Override
        public IStateTransferClient createClient(UUID groupId)
        {
            return new TestStateTransferClient(this);
        }

        @Override
        public IStateStore createStore(UUID groupId)
        {
            return stateStore;
        }
    }
    
    private class TestStateStore implements ISimpleStateStore
    {
        private ByteArray buffer = createBuffer(17, 100000);
        private ByteArray savedBuffer;
        
        @Override
        public ByteArray load(UUID id)
        {
            return buffer.clone();
        }

        @Override
        public void save(UUID id, ByteArray state)
        {
            savedBuffer = state.clone();
        }
    }
    
    private static final class TestBufferMessagePart implements IMessagePart
    {
        private final ByteArray buffer;

        public TestBufferMessagePart(ByteArray buffer)
        {
            this.buffer = buffer;
        }
        
        public ByteArray getBuffer()
        {
            return buffer;
        }
        
        @Override
        public int getSize()
        {
            return 3;
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

            serialization.writeByteArray(part.getBuffer());
        }
        
        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            ByteArray buffer = deserialization.readByteArray();
            return new TestBufferMessagePart(buffer);
        }
    }
    
    private class TestMessageSender extends AbstractProtocol implements IFlushParticipant, IFlowController<RemoteFlowId>
    {
        public boolean failOnFlush;
        private final TestStateTransferFactory stateTransferFactory;
        private boolean coordinator;
        private IFlush flush;
        private IGroupMembership membership;
        private boolean disableSend;
        private boolean flowLocked;

        public TestMessageSender(String channelName, IMessageFactory messageFactory, TestStateTransferFactory stateTransferFactory)
        {
            super(channelName, messageFactory);
            
            this.stateTransferFactory = stateTransferFactory;
        }
        
        @Override
        public boolean isFlushProcessingRequired()
        {
            return false;
        }

        @Override
        public void setCoordinator()
        {
            this.coordinator = true;
        }

        @Override
        public void startFlush(IFlush flush)
        {
            this.flush = flush;
            this.membership = flush.getNewMembership();
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
            flush = null;
        }
        
        @Override
        public void onTimer(long currentTime)
        {
            if (!flowLocked && coordinator && flush == null && membership != null && !disableSend)
            {
                ByteArray buffer = stateTransferFactory.state.clone();
                int counter = Bytes.readInt(buffer.getBuffer(), buffer.getOffset());
                Bytes.writeInt(buffer.getBuffer(), buffer.getOffset(), counter + 1);
                
                for (INode member : membership.getGroup().getMembers())
                    getSender().send(getMessageFactory().create(member.getAddress(), new TestBufferMessagePart(buffer)));
            }
        }
        
        @Override
        public void register(ISerializationRegistry registry)
        {
            registry.register(new TestBufferMessagePartSerializer());
        }

        @Override
        public void unregister(ISerializationRegistry registry)
        {
            registry.unregister(TestBufferMessagePartSerializer.ID);
        }
        
        @Override
        protected void doReceive(IReceiver receiver, IMessage message)
        {
            receiver.receive(message);
        }

        @Override
        public void lockFlow(RemoteFlowId flow)
        {
            flowLocked = true;
        }

        @Override
        public void unlockFlow(RemoteFlowId flow)
        {
            flowLocked = false;
        }
    }
    
    private static ChannelFactoryParameters getFactoryParameters()
    {
        boolean debug = Debug.isDebug();
        
        ChannelFactoryParameters factoryParameters = new ChannelFactoryParameters(debug);
        if (!debug)
        {
            factoryParameters.heartbeatTrackPeriod = 100;
            factoryParameters.heartbeatPeriod = 100;
            factoryParameters.heartbeatStartPeriod = 300;
            factoryParameters.heartbeatFailureDetectionPeriod = 1000;
            factoryParameters.transportChannelTimeout = 1000;
        }
        
        return factoryParameters;
    }
    
    private class TestChannelFactory extends ChannelFactory
    {
        private final IDiscoveryStrategy discoveryStrategy;
        private final long discoveryPeriod = 200;
        private final long groupFormationPeriod = 2000;
        private long failureUpdatePeriod = 500;
        private long failureHistoryPeriod = 10000;
        private int maxShunCount = 3;
        private long flushTimeout = 10000;
        private long saveSnapshotPeriod = 10000;
        private List<TestStateTransferFactory> stateTransferFactories = new ArrayList<TestStateTransferFactory>();
        private CoreGroupMembershipTracker membershipTracker;
        private CoreGroupMembershipManager membershipManager;
        private TestStateStore stateStore = new TestStateStore();
        private List<TestMessageSender> messageSenders = new ArrayList<TestMessageSender>();
        private List<SimpleStateTransferClientProtocol> clientProtocols = new ArrayList<SimpleStateTransferClientProtocol>();
        
        public TestChannelFactory(IDiscoveryStrategy discoveryStrategy)
        {
            super(getFactoryParameters());
            this.discoveryStrategy = discoveryStrategy;
            
            boolean debug = Debug.isDebug();
            int timeMultiplier = !debug ? 1 : 1000;
            flushTimeout *= timeMultiplier;
        }

        @Override
        protected INodeTrackingStrategy createNodeTrackingStrategy()
        {
            return new GroupNodeTrackingStrategy();
        }
        
        @Override
        protected void createProtocols(ChannelParameters parameters, String channelName, IMessageFactory messageFactory, 
            ISerializationRegistry serializationRegistry, ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers, 
            List<AbstractProtocol> protocols)
        {
            Set<IPreparedGroupMembershipListener> preparedMembershipListeners = new HashSet<IPreparedGroupMembershipListener>();
            Set<IGroupMembershipListener> membershipListeners = new HashSet<IGroupMembershipListener>();
            LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeProvider, new PropertyProviderMock(), 
                GroupMemberships.CORE_DOMAIN);
            membershipManager = new CoreGroupMembershipManager("test", localNodeProvider, 
                preparedMembershipListeners, membershipListeners);

            Set<IFailureDetectionListener> failureDetectionListeners = new HashSet<IFailureDetectionListener>();
            CoreGroupFailureDetectionProtocol failureDetectionProtocol = new CoreGroupFailureDetectionProtocol(channelName, messageFactory, membershipManager, 
                failureDetectionListeners, failureUpdatePeriod, failureHistoryPeriod, maxShunCount);
            preparedMembershipListeners.add(failureDetectionProtocol);
            failureObservers.add(failureDetectionProtocol);
            
            CoreGroupDiscoveryProtocol discoveryProtocol = new CoreGroupDiscoveryProtocol(channelName, messageFactory, membershipManager, 
                failureDetectionProtocol, discoveryStrategy, liveNodeProvider, discoveryPeriod, 
                groupFormationPeriod);
            preparedMembershipListeners.add(discoveryProtocol);
            membershipListeners.add(discoveryProtocol);
            membershipManager.setNodeDiscoverer(discoveryProtocol);
            
            TestStateTransferFactory stateTransferFactory = new TestStateTransferFactory(stateStore);
            stateTransferFactories.add(stateTransferFactory);
            
            SimpleStateTransferClientProtocol stateTransferClientProtocol = new SimpleStateTransferClientProtocol(channelName,
                messageFactory, membershipManager, stateTransferFactory, GroupMemberships.CORE_GROUP_ID);
            protocols.add(stateTransferClientProtocol);
            discoveryProtocol.setGroupJoinStrategy(stateTransferClientProtocol);
            failureDetectionListeners.add(stateTransferClientProtocol);
            clientProtocols.add(stateTransferClientProtocol);
            
            SimpleStateTransferServerProtocol stateTransferServerProtocol = new SimpleStateTransferServerProtocol(channelName, 
                messageFactory, membershipManager, failureDetectionProtocol, stateTransferFactory, 
                GroupMemberships.CORE_GROUP_ID, saveSnapshotPeriod);
            protocols.add(stateTransferServerProtocol);
            
            TestMessageSender testSender = new TestMessageSender(channelName, messageFactory, stateTransferFactory);
            protocols.add(testSender);
            messageSenders.add(testSender);
            
            FlowControlProtocol flowControlProtocol = new FlowControlProtocol(channelName, messageFactory, 
                membershipManager);
            flowControlProtocol.setFlowController(testSender);
            protocols.add(flowControlProtocol);
            failureDetectionListeners.add(flowControlProtocol);
            flowControlProtocol.setFailureDetector(failureDetectionProtocol);
            
            FlushParticipantProtocol flushParticipantProtocol = new FlushParticipantProtocol(channelName, messageFactory, 
                Arrays.<IFlushParticipant>asList(stateTransferClientProtocol, stateTransferServerProtocol, testSender), membershipManager, failureDetectionProtocol);
            protocols.add(flushParticipantProtocol);
            FlushCoordinatorProtocol flushCoordinatorProtocol = new FlushCoordinatorProtocol(channelName, messageFactory, 
                membershipManager, failureDetectionProtocol, flushTimeout, flushParticipantProtocol);
            failureDetectionListeners.add(flushCoordinatorProtocol);
            protocols.add(flushCoordinatorProtocol);

            protocols.add(discoveryProtocol);
            protocols.add(failureDetectionProtocol);
            
            membershipTracker = new CoreGroupMembershipTracker(1000, membershipManager, discoveryProtocol, 
                failureDetectionProtocol, flushCoordinatorProtocol, null);
        }
        
        @Override
        protected void wireProtocols(IChannel channel, TcpTransport transport, ProtocolStack protocolStack)
        {
            CoreGroupFailureDetectionProtocol failureDetectionProtocol = protocolStack.find(CoreGroupFailureDetectionProtocol.class);
            failureDetectionProtocol.setFailureObserver(transport);
            failureDetectionProtocol.setChannelReconnector((IChannelReconnector)channel);
            channel.getCompartment().addTimerProcessor(membershipTracker);
            
            GroupNodeTrackingStrategy strategy = (GroupNodeTrackingStrategy)protocolStack.find(HeartbeatProtocol.class).getNodeTrackingStrategy();
            strategy.setFailureDetector(failureDetectionProtocol);
            strategy.setMembershipManager((IGroupMembershipManager)failureDetectionProtocol.getMembersipService());
        }
        
        @Override
        protected IChannel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
            MessageFactory messageFactory, ProtocolStack protocolStack, TcpTransport transport,
            ConnectionManager connectionManager, ICompartment compartment)
        {
            return new TestGroupChannel(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, 
                connectionManager, compartment, membershipManager, null, membershipTracker);
        }
    }
}
