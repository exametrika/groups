/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipChange;
import com.exametrika.api.groups.core.IMembershipListener;
import com.exametrika.api.groups.core.IMembershipListener.LeaveReason;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelFactory.FactoryParameters;
import com.exametrika.common.messaging.impl.ChannelFactory.Parameters;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.HeartbeatProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeTrackingStrategy;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.common.net.nio.TcpNioDispatcher;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.core.channel.IChannelReconnector;
import com.exametrika.impl.groups.core.failuredetection.FailureDetectionProtocol;
import com.exametrika.impl.groups.core.failuredetection.GroupNodeTrackingStrategy;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.core.membership.Group;
import com.exametrika.impl.groups.core.membership.GroupAddress;
import com.exametrika.impl.groups.core.membership.IMembershipManager;
import com.exametrika.impl.groups.core.membership.Membership;
import com.exametrika.impl.groups.core.membership.MembershipSerializationRegistrar;
import com.exametrika.impl.groups.core.membership.Node;
import com.exametrika.tests.common.messaging.ReceiverMock;

/**
 * The {@link FailureDetectionProtocolTests} are tests for {@link FailureDetectionProtocol}.
 * 
 * @see FailureDetectionProtocol
 * @author Medvedev-A
 */
public class FailureDetectionProtocolTests
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
        TestChannelFactory channelFactory = new TestChannelFactory(new FactoryParameters(Debug.isDebug()));
        List<INode> nodes = new ArrayList<INode>();
        for (int i = 0; i < COUNT; i++)
        {
            Parameters parameters = new Parameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            channel.start();
            channels[i] = channel;
            nodes.add(channelFactory.membershipServices.get(i).getLocalNode());
        }
        
        Membership membership = new Membership(1, new Group(new GroupAddress(UUID.randomUUID(), "test"), true, nodes));
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
            FailureDetectionProtocol protocol = channelFactory.protocols.get(i);
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
        
        Membership membership2 = new Membership(2, new Group(new GroupAddress(UUID.randomUUID(), "test"), true, newNodes));
        for (int i = 0; i < COUNT; i++)
        {
            if (i == 0 || i == 3 || i == 4)
                continue;
            channelFactory.membershipServices.get(i).membership = membership2;
            FailureDetectionProtocol protocol = channelFactory.protocols.get(i);
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
            FailureDetectionProtocol protocol = channelFactory.protocols.get(i);
            Map history = Tests.get(protocol, "failureHistory");
            assertTrue(history.isEmpty());
        }
    }
    
    @Test @Ignore
    public void testShun() throws Exception
    {
        TestChannelFactory channelFactory = new TestChannelFactory(new FactoryParameters(Debug.isDebug()));
        channelFactory.failureHistoryPeriod = 20000;
        List<INode> nodes = new ArrayList<INode>();
        for (int i = 0; i < COUNT; i++)
        {
            Parameters parameters = new Parameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            channel.start();
            channels[i] = channel;
            nodes.add(channelFactory.membershipServices.get(i).getLocalNode());
        }
        
        Membership membership = new Membership(1, new Group(new GroupAddress(UUID.randomUUID(), "test"), true, nodes));
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
        FactoryParameters factoryParameters = new FactoryParameters(Debug.isDebug());
        factoryParameters.heartbeatTrackPeriod = 100;
        factoryParameters.heartbeatPeriod = 100;
        factoryParameters.heartbeatStartPeriod = 300;
        factoryParameters.heartbeatFailureDetectionPeriod = 1000;
        factoryParameters.transportChannelTimeout = 1000;
        TestChannelFactory channelFactory = new TestChannelFactory(factoryParameters);
        List<INode> nodes = new ArrayList<INode>();
        for (int i = 0; i < COUNT; i++)
        {
            Parameters parameters = new Parameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            channel.start();
            channels[i] = channel;
            nodes.add(channelFactory.membershipServices.get(i).getLocalNode());
        }
        
        Membership membership = new Membership(1, new Group(new GroupAddress(UUID.randomUUID(), "test"), true, nodes));
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
            FailureDetectionProtocol protocol = channelFactory.protocols.get(i);
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
        
        Membership membership2 = new Membership(2, new Group(new GroupAddress(UUID.randomUUID(), "test"), true, newNodes));
        for (int i = 0; i < COUNT; i++)
        {
            if (i == 0 || i == 3 || i == 4)
                continue;
            
            channelFactory.membershipServices.get(i).membership = membership2;
            FailureDetectionProtocol protocol = channelFactory.protocols.get(i);
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
            
            FailureDetectionProtocol protocol = channelFactory.protocols.get(i);
            Map history = Tests.get(protocol, "failureHistory");
            assertTrue(history.isEmpty());
        }
    }
    
    public static void failChannel(IChannel channel) throws Exception
    {
        ((TcpNioDispatcher)Tests.get(((Channel)channel).getCompartment(), "dispatcher")).getSelector().close();
        IOs.close(channel);
    }
    
    private static class MembershipServiceMock implements IMembershipManager
    {
        private Node localNode;
        private Membership membership;
        private ILiveNodeProvider provider;
        private String name;
        
        public MembershipServiceMock(String name, ILiveNodeProvider provider)
        {
            this.name = name;
            this.provider = provider;
        }
        
        @Override
        public synchronized INode getLocalNode()
        {
            if (localNode == null)
                localNode = new Node(name, provider.getLocalNode(), Collections.<String, Object>emptyMap());
            
            return localNode;
        }

        @Override
        public synchronized IMembership getMembership()
        {
            return membership;
        }

        @Override
        public void addMembershipListener(IMembershipListener listener)
        {
        }

        @Override
        public void removeMembershipListener(IMembershipListener listener)
        {
        }

        @Override
        public void removeAllMembershipListeners()
        {
        }

        @Override
        public IMembership getPreparedMembership()
        {
            return membership;
        }

        @Override
        public void prepareInstallMembership(IMembership membership)
        {
        }

        @Override
        public void prepareChangeMembership(IMembership membership, IMembershipChange membershipChange)
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
    
    public static class ChannelReconnectorMock implements IChannelReconnector
    {
        private boolean reconnectRequested;

        @Override
        public void reconnect()
        {
            reconnectRequested = true;
        }
    }
    
    public static class FailureDetectionListenerMock implements IFailureDetectionListener
    {
        private Set<INode> failedMembers = new HashSet<INode>();
        private Set<INode> leftMembers = new HashSet<INode>();
        
        @Override
        public void onMemberFailed(INode member)
        {
            failedMembers.add(member);
        }

        @Override
        public void onMemberLeft(INode member)
        {
            leftMembers.add(member);
        }
    }
    
    private class TestChannelFactory extends ChannelFactory
    {
        private long failureUpdatePeriod = 500;
        private  long failureHistoryPeriod = 2000;
        private  int maxShunCount = 3;
        private List<FailureDetectionProtocol> protocols = new ArrayList<FailureDetectionProtocol>();
        private List<MembershipServiceMock> membershipServices = new ArrayList<MembershipServiceMock>();
        private List<FailureDetectionListenerMock> failureListeners = new ArrayList<FailureDetectionListenerMock>();
        private List<ChannelReconnectorMock> channelReconnectors = new ArrayList<ChannelReconnectorMock>();
        private List<IMessageFactory> messageFactories = new ArrayList<IMessageFactory>();
        
        public TestChannelFactory(FactoryParameters factoryParameters)
        {
            super(factoryParameters);
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
            messageFactories.add(messageFactory);
            MembershipServiceMock membershipService = new MembershipServiceMock(channelName, liveNodeProvider);
            membershipServices.add(membershipService);
            FailureDetectionListenerMock failureListener = new FailureDetectionListenerMock();
            failureListeners.add(failureListener);
            ChannelReconnectorMock channelReconnector = new ChannelReconnectorMock();
            channelReconnectors.add(channelReconnector);
            
            FailureDetectionProtocol failureDetectionProtocol = new FailureDetectionProtocol(channelName, messageFactory, membershipService, 
                Collections.<IFailureDetectionListener>singleton(failureListener), failureUpdatePeriod,
                failureHistoryPeriod, maxShunCount);
            failureDetectionProtocol.setChannelReconnector(channelReconnector);
            protocols.add(failureDetectionProtocol);
            failureObservers.add(failureDetectionProtocol);
            
            this.protocols.add(failureDetectionProtocol);
            
            serializationRegistry.register(new MembershipSerializationRegistrar());
        }
        
        @Override
        protected void wireProtocols(Channel channel, TcpTransport transport, ProtocolStack protocolStack)
        {
            GroupNodeTrackingStrategy strategy = (GroupNodeTrackingStrategy)protocolStack.find(HeartbeatProtocol.class).getNodeTrackingStrategy();
            FailureDetectionProtocol failureDetectionProtocol = protocolStack.find(FailureDetectionProtocol.class);
            failureDetectionProtocol.setFailureObserver(transport);
            strategy.setFailureDetector(failureDetectionProtocol);
            strategy.setMembershipManager((IMembershipManager)failureDetectionProtocol.getMembersipService());
        }
    }
}
