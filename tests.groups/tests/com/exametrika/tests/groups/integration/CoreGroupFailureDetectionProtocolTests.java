/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.IMembershipListener.LeaveReason;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelFactoryParameters;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.composite.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.HeartbeatProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeTrackingStrategy;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.common.net.nio.TcpNioDispatcher;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.failuredetection.CoreGroupFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.GroupNodeTrackingStrategy;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.membership.Group;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.GroupMembership;
import com.exametrika.impl.groups.cluster.membership.GroupMembershipSerializationRegistrar;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.Node;
import com.exametrika.tests.common.messaging.ReceiverMock;
import com.exametrika.tests.groups.mocks.ChannelReconnectorMock;
import com.exametrika.tests.groups.mocks.FailureDetectionListenerMock;

/**
 * The {@link CoreGroupFailureDetectionProtocolTests} are tests for {@link CoreGroupFailureDetectionProtocol}.
 * 
 * @see CoreGroupFailureDetectionProtocol
 * @author Medvedev-A
 */
public class CoreGroupFailureDetectionProtocolTests
{
    private static final int COUNT = 10;
    private IChannel[] channels = new IChannel[COUNT];
    
    @After
    public void tearDown()
    {
        for (IChannel channel : channels)
            IOs.close(channel);
    }
    
    @Test
    public void testExplicitLeaveFailure() throws Exception
    {
        TestChannelFactory channelFactory = new TestChannelFactory(new ChannelFactoryParameters(Debug.isDebug()));
        List<INode> nodes = new ArrayList<INode>();
        for (int i = 0; i < COUNT; i++)
        {
            ChannelParameters parameters = new ChannelParameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            channel.start();
            channels[i] = channel;
            nodes.add(channelFactory.membershipServices.get(i).getLocalNode());
        }
        
        GroupMembership membership = new GroupMembership(1, new Group(new GroupAddress(UUID.randomUUID(), "test"), true, nodes,
            Enums.noneOf(GroupOption.class)));
        for (int i = 0; i < COUNT; i++)
        {
            channelFactory.protocols.get(i).onPreparedMembershipChanged(null, membership, null);
            channelFactory.membershipServices.get(i).membership = membership;
        }
        
        channelFactory.protocols.get(2).addFailedMembers(com.exametrika.common.utils.Collections.asSet(nodes.get(0).getId(), 
            nodes.get(4).getId(), UUID.randomUUID()));
        channelFactory.protocols.get(2).addLeftMembers(com.exametrika.common.utils.Collections.asSet(nodes.get(3).getId(), 
            nodes.get(4).getId(), UUID.randomUUID()));

        failChannel(channels[0]);
        failChannel(channels[3]);
        failChannel(channels[4]);
        
        Threads.sleep(3000);
        
        for (int i = 0; i < COUNT; i++)
        {
            if (i == 0 || i == 3 || i == 4)
                continue;
            
            List<INode> healthy = new ArrayList<INode>(nodes);
            healthy.remove(nodes.get(0));
            healthy.remove(nodes.get(3));
            healthy.remove(nodes.get(4));
            CoreGroupFailureDetectionProtocol protocol = channelFactory.protocols.get(i);
            assertThat(protocol.getCurrentCoordinator(), is(nodes.get(1)));
            assertThat(protocol.getHealthyMembers(), is(healthy));
            assertThat(protocol.getLeftMembers(), is(com.exametrika.common.utils.Collections.asSet(nodes.get(3), nodes.get(4))));
            assertThat(protocol.getFailedMembers(), is(Collections.singleton(nodes.get(0))));
            
            assertTrue(!channels[i].getLiveNodeProvider().isLive(nodes.get(0).getAddress()));
            assertTrue(!channels[i].getLiveNodeProvider().isLive(nodes.get(3).getAddress()));
            assertTrue(!channels[i].getLiveNodeProvider().isLive(nodes.get(4).getAddress()));
            
            FailureDetectionListenerMock listener = channelFactory.failureListeners.get(i);
            assertThat(listener.leftMembers, is(com.exametrika.common.utils.Collections.asSet(nodes.get(3), nodes.get(4))));
            assertThat(listener.failedMembers.contains(nodes.get(0)), is(true));
            assertThat(!listener.failedMembers.contains(nodes.get(3)), is(true));
            assertThat(channelFactory.channelReconnectors.get(i).reconnectRequested, is(false));
        }
        
        List<INode> newNodes = new ArrayList<INode>(nodes);
        newNodes.remove(nodes.get(0));
        newNodes.remove(nodes.get(3));
        
        List<INode> healthy = new ArrayList<INode>(newNodes);
        healthy.remove(nodes.get(4));
        
        GroupMembership membership2 = new GroupMembership(2, new Group(new GroupAddress(UUID.randomUUID(), "test"), true, newNodes,
            Enums.noneOf(GroupOption.class)));
        for (int i = 0; i < COUNT; i++)
        {
            if (i == 0 || i == 3 || i == 4)
                continue;
            channelFactory.membershipServices.get(i).membership = membership2;
            CoreGroupFailureDetectionProtocol protocol = channelFactory.protocols.get(i);
            protocol.onPreparedMembershipChanged(null, membership2, null);
            assertThat(protocol.getCurrentCoordinator(), is(nodes.get(1)));
            assertThat(protocol.getHealthyMembers(), is(healthy));
            assertThat(protocol.getFailedMembers(), is(Collections.<INode>emptySet()));
            assertThat(protocol.getLeftMembers(), is(Collections.singleton(nodes.get(4))));
        }
        
        Threads.sleep(3000);
        
        for (int i = 0; i < COUNT; i++)
        {
            if (i == 0 || i == 3 || i == 4)
                continue;
            CoreGroupFailureDetectionProtocol protocol = channelFactory.protocols.get(i);
            Map history = Tests.get(protocol, "failureHistory");
            assertTrue(history.isEmpty());
        }
    }
    
    @Test @Ignore
    public void testShun() throws Exception
    {
        TestChannelFactory channelFactory = new TestChannelFactory(new ChannelFactoryParameters(Debug.isDebug()));
        channelFactory.failureHistoryPeriod = 20000;
        List<INode> nodes = new ArrayList<INode>();
        for (int i = 0; i < COUNT; i++)
        {
            ChannelParameters parameters = new ChannelParameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            channel.start();
            channels[i] = channel;
            nodes.add(channelFactory.membershipServices.get(i).getLocalNode());
        }
        
        GroupMembership membership = new GroupMembership(1, new Group(new GroupAddress(UUID.randomUUID(), "test"), true, nodes,
            Enums.noneOf(GroupOption.class)));
        for (int i = 0; i < COUNT; i++)
        {
            channelFactory.protocols.get(i).onPreparedMembershipChanged(null, membership, null);
            channelFactory.membershipServices.get(i).membership = membership;
        }
        
        Threads.sleep(1000);
        
        for (int i = 0; i < COUNT; i++)
        {
            if (i == 0 || i == 3 || i == 4)
                Tests.set(channelFactory.protocols.get(i), "membership", null);
        }
        
        channelFactory.protocols.get(2).addFailedMembers(com.exametrika.common.utils.Collections.asSet(nodes.get(0).getId(), 
            nodes.get(4).getId(), UUID.randomUUID()));
        channelFactory.protocols.get(2).addLeftMembers(com.exametrika.common.utils.Collections.asSet(nodes.get(3).getId(), 
            nodes.get(4).getId(), UUID.randomUUID()));
        
        Threads.sleep(1000);
        
        channels[1].connect(nodes.get(0).getAddress());
        channels[1].connect(nodes.get(3).getAddress());
        channels[1].connect(nodes.get(4).getAddress());
        
        Threads.sleep(1000);
        
        for (int i = 0; i < 3; i++)
        {
            channelFactory.protocols.get(1).receive(channelFactory.messageFactories.get(0).create(nodes.get(1).getAddress(), 
                MessageFlags.HIGH_PRIORITY | MessageFlags.PARALLEL));
            channelFactory.protocols.get(1).receive(channelFactory.messageFactories.get(3).create(nodes.get(1).getAddress(), 
                MessageFlags.HIGH_PRIORITY | MessageFlags.PARALLEL));
            channelFactory.protocols.get(1).receive(channelFactory.messageFactories.get(4).create(nodes.get(1).getAddress(),
                MessageFlags.HIGH_PRIORITY | MessageFlags.PARALLEL));
        }
        
        Threads.sleep(3000);
        
        for (int i = 0; i < COUNT; i++)
        {
            assertThat(channelFactory.channelReconnectors.get(i).reconnectRequested, is(i == 0));
        }
    }
    
    @Test
    public void testDetectedLeaveFailure() throws Exception
    {
        ChannelFactoryParameters factoryParameters = new ChannelFactoryParameters(Debug.isDebug());
        factoryParameters.heartbeatTrackPeriod = 100;
        factoryParameters.heartbeatPeriod = 100;
        factoryParameters.heartbeatStartPeriod = 300;
        factoryParameters.heartbeatFailureDetectionPeriod = 1000;
        factoryParameters.transportChannelTimeout = 1000;
        TestChannelFactory channelFactory = new TestChannelFactory(factoryParameters);
        List<INode> nodes = new ArrayList<INode>();
        for (int i = 0; i < COUNT; i++)
        {
            ChannelParameters parameters = new ChannelParameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            channel.start();
            channels[i] = channel;
            nodes.add(channelFactory.membershipServices.get(i).getLocalNode());
        }
        
        GroupMembership membership = new GroupMembership(1, new Group(new GroupAddress(UUID.randomUUID(), "test"), true, nodes,
            Enums.noneOf(GroupOption.class)));
        for (int i = 0; i < COUNT; i++)
        {
            channelFactory.protocols.get(i).onPreparedMembershipChanged(null, membership, null);
            channelFactory.membershipServices.get(i).membership = membership;
        }
        
        IOs.close(channels[4]);
        Threads.sleep(1000);
        failChannel(channels[0]);
        failChannel(channels[3]);
        
        Threads.sleep(3000);
        
        for (int i = 0; i < COUNT; i++)
        {
            if (i == 0 || i == 3 || i == 4)
                continue;
            
            List<INode> healthy = new ArrayList<INode>(nodes);
            healthy.remove(nodes.get(0));
            healthy.remove(nodes.get(3));
            healthy.remove(nodes.get(4));
            CoreGroupFailureDetectionProtocol protocol = channelFactory.protocols.get(i);
            assertThat(protocol.getCurrentCoordinator(), is(nodes.get(1)));
            assertThat(protocol.getHealthyMembers(), is(healthy));
            assertThat(protocol.getFailedMembers(), is(com.exametrika.common.utils.Collections.asSet(nodes.get(0), nodes.get(3))));
            assertThat(protocol.getLeftMembers(), is(Collections.singleton(nodes.get(4))));
            
            assertTrue(!channels[i].getLiveNodeProvider().isLive(nodes.get(0).getAddress()));
            assertTrue(!channels[i].getLiveNodeProvider().isLive(nodes.get(3).getAddress()));
            assertTrue(!channels[i].getLiveNodeProvider().isLive(nodes.get(4).getAddress()));
            
            FailureDetectionListenerMock listener = channelFactory.failureListeners.get(i);
            assertThat(listener.failedMembers, is(com.exametrika.common.utils.Collections.asSet(nodes.get(0), nodes.get(3))));
            assertThat(listener.leftMembers, is(Collections.singleton(nodes.get(4))));
            assertThat(channelFactory.channelReconnectors.get(i).reconnectRequested, is(false));
        }
        
        List<INode> newNodes = new ArrayList<INode>(nodes);
        newNodes.remove(nodes.get(0));
        newNodes.remove(nodes.get(3));
        List<INode> newHealthy = new ArrayList<INode>(newNodes);
        newHealthy.remove(nodes.get(4));
        
        GroupMembership membership2 = new GroupMembership(2, new Group(new GroupAddress(UUID.randomUUID(), "test"), true, newNodes,
            Enums.noneOf(GroupOption.class)));
        for (int i = 0; i < COUNT; i++)
        {
            if (i == 0 || i == 3 || i == 4)
                continue;
            
            channelFactory.membershipServices.get(i).membership = membership2;
            CoreGroupFailureDetectionProtocol protocol = channelFactory.protocols.get(i);
            protocol.onPreparedMembershipChanged(null, membership2, null);
            assertThat(protocol.getCurrentCoordinator(), is(nodes.get(1)));
            assertThat(protocol.getHealthyMembers(), is(newHealthy));
            assertThat(protocol.getFailedMembers(), is(Collections.<INode>emptySet()));
            assertThat(protocol.getLeftMembers(), is(Collections.singleton(nodes.get(4))));
        }
        
        Threads.sleep(2000);
        
        for (int i = 0; i < COUNT; i++)
        {
            if (i == 0 || i == 3 || i == 4)
                continue;
            
            CoreGroupFailureDetectionProtocol protocol = channelFactory.protocols.get(i);
            Map history = Tests.get(protocol, "failureHistory");
            assertTrue(history.isEmpty());
        }
    }
    
    public static void failChannel(IChannel channel) throws Exception
    {
        ((TcpNioDispatcher)Tests.get(((Channel)channel).getCompartment(), "dispatcher")).getSelector().close();
        IOs.close(channel);
    }
    
    private static class MembershipServiceMock implements IGroupMembershipManager
    {
        private Node localNode;
        private GroupMembership membership;
        private ILiveNodeProvider provider;
        
        public MembershipServiceMock(ILiveNodeProvider provider)
        {
            this.provider = provider;
        }
        
        @Override
        public synchronized INode getLocalNode()
        {
            if (localNode == null)
                localNode = new Node(provider.getLocalNode(), Collections.<String, Object>emptyMap(), "core");
            
            return localNode;
        }

        @Override
        public synchronized IGroupMembership getMembership()
        {
            return membership;
        }

        @Override
        public void addMembershipListener(IGroupMembershipListener listener)
        {
        }

        @Override
        public void removeMembershipListener(IGroupMembershipListener listener)
        {
        }

        @Override
        public void removeAllMembershipListeners()
        {
        }

        @Override
        public IGroupMembership getPreparedMembership()
        {
            return membership;
        }

        @Override
        public void prepareInstallMembership(IGroupMembership membership)
        {
        }

        @Override
        public void prepareChangeMembership(IGroupMembership membership, IGroupMembershipChange membershipChange)
        {
        }

        @Override
        public void commitMembership()
        {
        }

        @Override
        public void uninstallMembership(LeaveReason reason)
        {
        }
    }
    
    private class TestChannelFactory extends ChannelFactory
    {
        private long failureUpdatePeriod = 500;
        private  long failureHistoryPeriod = 2000;
        private  int maxShunCount = 3;
        private List<CoreGroupFailureDetectionProtocol> protocols = new ArrayList<CoreGroupFailureDetectionProtocol>();
        private List<MembershipServiceMock> membershipServices = new ArrayList<MembershipServiceMock>();
        private List<FailureDetectionListenerMock> failureListeners = new ArrayList<FailureDetectionListenerMock>();
        private List<ChannelReconnectorMock> channelReconnectors = new ArrayList<ChannelReconnectorMock>();
        private List<IMessageFactory> messageFactories = new ArrayList<IMessageFactory>();
        
        public TestChannelFactory(ChannelFactoryParameters factoryParameters)
        {
            super(factoryParameters);
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
            messageFactories.add(messageFactory);
            MembershipServiceMock membershipService = new MembershipServiceMock(liveNodeProvider);
            membershipServices.add(membershipService);
            FailureDetectionListenerMock failureListener = new FailureDetectionListenerMock();
            failureListeners.add(failureListener);
            ChannelReconnectorMock channelReconnector = new ChannelReconnectorMock();
            channelReconnectors.add(channelReconnector);
            
            CoreGroupFailureDetectionProtocol failureDetectionProtocol = new CoreGroupFailureDetectionProtocol(channelName, messageFactory, membershipService, 
                Collections.<IFailureDetectionListener>singleton(failureListener), failureUpdatePeriod,
                failureHistoryPeriod, maxShunCount);
            failureDetectionProtocol.setChannelReconnector(channelReconnector);
            protocols.add(failureDetectionProtocol);
            failureObservers.add(failureDetectionProtocol);
            
            this.protocols.add(failureDetectionProtocol);
            
            serializationRegistry.register(new GroupMembershipSerializationRegistrar());
        }
        
        @Override
        protected void wireProtocols(IChannel channel, TcpTransport transport, ProtocolStack protocolStack)
        {
            GroupNodeTrackingStrategy strategy = (GroupNodeTrackingStrategy)protocolStack.find(HeartbeatProtocol.class).getNodeTrackingStrategy();
            CoreGroupFailureDetectionProtocol failureDetectionProtocol = protocolStack.find(CoreGroupFailureDetectionProtocol.class);
            failureDetectionProtocol.setFailureObserver(transport);
            strategy.setFailureDetector(failureDetectionProtocol);
            strategy.setMembershipManager((IGroupMembershipManager)failureDetectionProtocol.getMembersipService());
        }
    }
}
