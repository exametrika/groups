/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Test;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipListener;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentProcessor;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelFactory.FactoryParameters;
import com.exametrika.common.messaging.impl.ChannelFactory.Parameters;
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
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Threads;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.groups.core.channel.GroupChannel;
import com.exametrika.impl.groups.core.channel.IChannelReconnector;
import com.exametrika.impl.groups.core.channel.IGracefulCloseStrategy;
import com.exametrika.impl.groups.core.discovery.DiscoveryProtocol;
import com.exametrika.impl.groups.core.discovery.WellKnownAddressesDiscoveryStrategy;
import com.exametrika.impl.groups.core.failuredetection.FailureDetectionProtocol;
import com.exametrika.impl.groups.core.failuredetection.GroupNodeTrackingStrategy;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.core.flush.FlushCoordinatorProtocol;
import com.exametrika.impl.groups.core.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.core.flush.IFlush;
import com.exametrika.impl.groups.core.flush.IFlushParticipant;
import com.exametrika.impl.groups.core.membership.IMembershipManager;
import com.exametrika.impl.groups.core.membership.IPreparedMembershipListener;
import com.exametrika.impl.groups.core.membership.MembershipManager;
import com.exametrika.impl.groups.core.membership.MembershipTracker;
import com.exametrika.impl.groups.core.membership.Memberships;
import com.exametrika.impl.groups.core.state.StateTransferClientProtocol;
import com.exametrika.impl.groups.core.state.StateTransferServerProtocol;
import com.exametrika.spi.groups.IDiscoveryStrategy;
import com.exametrika.spi.groups.IStateStore;
import com.exametrika.spi.groups.IStateTransferClient;
import com.exametrika.spi.groups.IStateTransferFactory;
import com.exametrika.spi.groups.IStateTransferServer;
import com.exametrika.tests.common.messaging.ReceiverMock;
import com.exametrika.tests.groups.DiscoveryProtocolTests.GroupJoinStrategyMock;
import com.exametrika.tests.groups.MembershipManagerTests.PropertyProviderMock;

/**
 * The {@link StateTransferProtocolTests} are tests for flush.
 * 
 * @author Medvedev-A
 */
public class StateTransferProtocolTests
{
    private static final int COUNT = 10;
    private GroupChannel[] channels = new GroupChannel[COUNT];
    private Sequencer sequencer = new Sequencer();
    
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
        failOnFlush(channelFactory);
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet());
         
        sequencer.waitAll(COUNT, 5000, 0);
        int coordinatorNodeIndex = findNodeIndex(0, channelFactory.flushParticipants.get(0).flush.getNewMembership().getGroup().getCoordinator());
        int[] nodes = selectNodes(0, COUNT, 2, coordinatorNodeIndex);
        FailureDetectionProtocolTests.failChannel(channels[nodes[0]]);
        IOs.close(channels[nodes[1]]);
        
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.asSet(nodes[0], nodes[1]));
    }
    
    @Test
    public void testGroupFormationCoordinatorFailure() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        failOnFlush(channelFactory);
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet());
         
        sequencer.waitAll(COUNT, 5000, 0);
        int coordinatorNodeIndex = findNodeIndex(0, channelFactory.flushParticipants.get(0).flush.getNewMembership().getGroup().getCoordinator());
        IOs.close(channels[coordinatorNodeIndex]);
        
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.asSet(coordinatorNodeIndex));
    }

    @Test
    public void testChangeMembership() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet(0, 1));
        
        channels[0].start();
        channels[1].start();
        
        Threads.sleep(3000);
        
        FailureDetectionProtocolTests.failChannel(channels[COUNT - 1]);
        IOs.close(channels[COUNT - 2]);
        
        Threads.sleep(10000);
        
        checkMembership(channelFactory, Collections.<Integer>asSet(COUNT - 1, COUNT - 2));
    }
    
    @Test
    public void testChangeMembershipNonCoordinatorFailureOnFlush() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet(0, 1));

        failOnFlush(channelFactory);
        int coordinatorNodeIndex = findNodeIndex(2, channels[2].getMembershipService().getMembership().getGroup().getCoordinator());
        int[] nodes = selectNodes(2, COUNT, 4, coordinatorNodeIndex);
        
        channels[0].start();
        channels[1].start();
        
        Threads.sleep(3000);
        
        FailureDetectionProtocolTests.failChannel(channels[nodes[0]]);
        IOs.close(channels[nodes[1]]);
        
        sequencer.waitAll(COUNT - 6, 5000, 0);
        
        FailureDetectionProtocolTests.failChannel(channels[nodes[2]]);
        IOs.close(channels[nodes[3]]);
        
        Threads.sleep(10000);
        
        checkMembership(channelFactory, Collections.<Integer>asSet(nodes[0], nodes[1], nodes[2], nodes[3]));
    }
    
    @Test
    public void testChangeMembershipCoordinatorFailureOnFlush() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet(0, 1));

        failOnFlush(channelFactory);
        int coordinatorNodeIndex = findNodeIndex(2, channels[2].getMembershipService().getMembership().getGroup().getCoordinator());
        int[] nodes = selectNodes(2, COUNT, 2, coordinatorNodeIndex);
        
        channels[0].start();
        channels[1].start();
        
        Threads.sleep(3000);
        
        FailureDetectionProtocolTests.failChannel(channels[nodes[0]]);
        IOs.close(channels[nodes[1]]);
        
        sequencer.waitAll(COUNT - 3, 5000, 0);
        FailureDetectionProtocolTests.failChannel(channels[coordinatorNodeIndex]);
        
        Threads.sleep(10000);
        
        checkMembership(channelFactory, Collections.<Integer>asSet(nodes[0], nodes[1], coordinatorNodeIndex));
    }
    
    private void createGroup(Set<String> wellKnownAddresses, TestChannelFactory channelFactory, Set<Integer> skipIndexes)
    {
        for (int i = 0; i < COUNT; i++)
        {
            Parameters parameters = new Parameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            if (!skipIndexes.contains(i))
            {
                channel.start();
                wellKnownAddresses.add(channel.getLiveNodeProvider().getLocalNode().getConnection());
            }
            channels[i] = (GroupChannel)channel;
        }
    }

    private void checkMembership(TestChannelFactory channelFactory, Set<Integer> skipIndexes)
    {
        IMembership membership = null;
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
            
            FlushParticipantMock participant = channelFactory.flushParticipants.get(i);
            assertThat(participant.beforeProcessingFlush, is(true));
            assertThat(participant.processFlush, is(true));
            assertThat(participant.endFlush, is(true));
            assertThat(participant.isCoordinator, is(channels[i].getMembershipService().getLocalNode().equals(
                membership.getGroup().getCoordinator())));
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

    private class TestStateTransferServer implements IStateTransferServer
    {
        TestStateTransferFactory factory;
        
        public TestStateTransferServer(TestStateTransferFactory factory)
        {
            this.factory = factory;
        }
        
        @Override
        public boolean isModifyingMessage(IMessagePart part)
        {
            return true;
        }

        @Override
        public void saveSnapshot(File file)
        {
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
        ByteArray state;
        
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
        private ByteArray buffer = createBuffer(17, 1000000);
        private ByteArray savedBuffer;
        
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
            if (id.equals(Memberships.CORE_GROUP_ID))
                savedBuffer = Files.readBytes(state);
            else
                Assert.error();
        }
    }
    
    private static FactoryParameters getFactoryParameters()
    {
        FactoryParameters factoryParameters = new FactoryParameters(Debug.isDebug());
        factoryParameters.heartbeatTrackPeriod = 100;
        factoryParameters.heartbeatPeriod = 100;
        factoryParameters.heartbeatStartPeriod = 300;
        factoryParameters.heartbeatFailureDetectionPeriod = 1000;
        factoryParameters.transportChannelTimeout = 1000;
        
        return factoryParameters;
    }
    
    private class TestChannelFactory extends ChannelFactory
    {
        private final IDiscoveryStrategy discoveryStrategy;
        private final long discoveryPeriod = 200;
        private final long groupFormationPeriod = 2000;
        private long failureUpdatePeriod = 500;
        private  long failureHistoryPeriod = 10000;
        private  int maxShunCount = 3;
        private long flushTimeout = 10000;
        private long gracefulCloseTimeout = 10000;
        private long maxStateTransferPeriod = Integer.MAX_VALUE;
        private long stateSizeThreshold = 1000000;
        private long saveSnapshotPeriod = 1000;
        private long transferLogRecordPeriod = 1000;
        private int transferLogMessagesCount = 100;
        private int minLockQueueCapacity = 10000000;
        private List<TestStateTransferFactory> stateTransferFactories = new ArrayList<TestStateTransferFactory>();
        private MembershipTracker membershipTracker;
        private MembershipManager membershipManager;
        private List<IGracefulCloseStrategy> gracefulCloseStrategies = new ArrayList<IGracefulCloseStrategy>();
        private TestStateStore stateStore = new TestStateStore();
        
        public TestChannelFactory(IDiscoveryStrategy discoveryStrategy)
        {
            super(getFactoryParameters());
            this.discoveryStrategy = discoveryStrategy;
        }

        @Override
        protected INodeTrackingStrategy createNodeTrackingStrategy()
        {
            return new GroupNodeTrackingStrategy();
        }
        
        @Override
        protected void createProtocols(Parameters parameters, String channelName, IMessageFactory messageFactory, 
            ISerializationRegistry serializationRegistry, ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers, 
            List<AbstractProtocol> protocols)
        {
            Set<IPreparedMembershipListener> preparedMembershipListeners = new HashSet<IPreparedMembershipListener>();
            Set<IMembershipListener> membershipListeners = new HashSet<IMembershipListener>();
            membershipManager = new MembershipManager("test", liveNodeProvider, new PropertyProviderMock(), 
                preparedMembershipListeners, membershipListeners);

            Set<IFailureDetectionListener> failureDetectionListeners = new HashSet<IFailureDetectionListener>();
            FailureDetectionProtocol failureDetectionProtocol = new FailureDetectionProtocol(channelName, messageFactory, membershipManager, 
                failureDetectionListeners, failureUpdatePeriod, failureHistoryPeriod, maxShunCount);
            preparedMembershipListeners.add(failureDetectionProtocol);
            failureObservers.add(failureDetectionProtocol);
            
            DiscoveryProtocol discoveryProtocol = new DiscoveryProtocol(channelName, messageFactory, membershipManager, 
                failureDetectionProtocol, discoveryStrategy, liveNodeProvider, discoveryPeriod, 
                groupFormationPeriod);
            preparedMembershipListeners.add(discoveryProtocol);
            membershipManager.setNodeDiscoverer(discoveryProtocol);
            
            TestStateTransferFactory stateTransferFactory = new TestStateTransferFactory();
            stateTransferFactories.add(stateTransferFactory);
            StateTransferClientProtocol stateTransferClientProtocol = new StateTransferClientProtocol(channelName,
                messageFactory, membershipManager, stateTransferFactory, stateStore, serializationRegistry, 
                maxStateTransferPeriod, stateSizeThreshold);
            protocols.add(stateTransferClientProtocol);
            discoveryProtocol.setGroupJoinStrategy(stateTransferClientProtocol);
            failureDetectionListeners.add(stateTransferClientProtocol);
            
            StateTransferServerProtocol stateTransferServerProtocol = new StateTransferServerProtocol(channelName, 
                messageFactory, membershipManager, failureDetectionProtocol, stateTransferFactory, stateStore, serializationRegistry, 
                saveSnapshotPeriod, transferLogRecordPeriod, transferLogMessagesCount, minLockQueueCapacity);
            protocols.add(stateTransferServerProtocol);
            
            FlushParticipantProtocol flushParticipantProtocol = new FlushParticipantProtocol(channelName, messageFactory, 
                Arrays.<IFlushParticipant>asList(stateTransferClientProtocol, stateTransferServerProtocol), membershipManager, failureDetectionProtocol);
            protocols.add(flushParticipantProtocol);
            FlushCoordinatorProtocol flushCoordinatorProtocol = new FlushCoordinatorProtocol(channelName, messageFactory, 
                membershipManager, failureDetectionProtocol, flushTimeout, flushParticipantProtocol);
            failureDetectionListeners.add(flushCoordinatorProtocol);
            protocols.add(flushCoordinatorProtocol);

            protocols.add(discoveryProtocol);
            protocols.add(failureDetectionProtocol);
            
            membershipTracker = new MembershipTracker(1000, membershipManager, discoveryProtocol, 
                failureDetectionProtocol, flushCoordinatorProtocol);
            
            gracefulCloseStrategies.add(flushCoordinatorProtocol);
            gracefulCloseStrategies.add(flushParticipantProtocol);
            gracefulCloseStrategies.add(membershipTracker);
        }
        
        @Override
        protected void wireProtocols(Channel channel, TcpTransport transport, ProtocolStack protocolStack)
        {
            FailureDetectionProtocol failureDetectionProtocol = protocolStack.find(FailureDetectionProtocol.class);
            failureDetectionProtocol.setFailureObserver(transport);
            failureDetectionProtocol.setChannelReconnector((IChannelReconnector)channel);
            channel.getCompartment().addProcessor(membershipTracker);
            
            GroupNodeTrackingStrategy strategy = (GroupNodeTrackingStrategy)protocolStack.find(HeartbeatProtocol.class).getNodeTrackingStrategy();
            strategy.setFailureDetector(failureDetectionProtocol);
            strategy.setMembershipManager((IMembershipManager)failureDetectionProtocol.getMembersipService());
            
            StateTransferClientProtocol stateTransferClientProtocol = protocolStack.find(StateTransferClientProtocol.class);
            stateTransferClientProtocol.setChannelReconnector((IChannelReconnector)channel);
            stateTransferClientProtocol.setCompartment(channel.getCompartment());
            
            StateTransferServerProtocol stateTransferServerProtocol = protocolStack.find(StateTransferServerProtocol.class);
            stateTransferServerProtocol.setCompartment(channel.getCompartment());
            stateTransferServerProtocol.setFlowController(transport);
        }
        
        @Override
        protected Channel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
            MessageFactory messageFactory, ProtocolStack protocolStack, TcpTransport transport,
            ConnectionManager connectionManager, ICompartment compartment)
        {
            return new GroupChannel(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, 
                connectionManager, compartment, membershipManager, gracefulCloseStrategies, gracefulCloseTimeout);
        }
    }
}
