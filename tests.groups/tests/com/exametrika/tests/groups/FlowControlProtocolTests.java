/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Test;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.api.groups.cluster.IMembershipListener.LeaveReason;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IAddress;
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
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.cluster.channel.IChannelReconnector;
import com.exametrika.impl.groups.cluster.failuredetection.CoreGroupFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.failuredetection.GroupNodeTrackingStrategy;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.membership.Group;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.GroupMembership;
import com.exametrika.impl.groups.cluster.membership.GroupMembershipSerializationRegistrar;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.Node;
import com.exametrika.impl.groups.cluster.multicast.FlowControlProtocol;
import com.exametrika.impl.groups.cluster.multicast.QueueCapacityController;
import com.exametrika.impl.groups.cluster.multicast.RemoteFlowId;
import com.exametrika.tests.common.messaging.ReceiverMock;
import com.exametrika.tests.common.messaging.TestAddress;

/**
 * The {@link FlowControlProtocolTests} are tests for {@link FlowControlProtocol}.
 * 
 * @see CoreGroupFailureDetectionProtocol
 * @author Medvedev-A
 */
public class FlowControlProtocolTests
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
    public void testFlowControl() throws Exception
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
        
        GroupMembership membership = new GroupMembership(1, new Group(new GroupAddress(UUID.randomUUID(), "test"), true, nodes));
        for (int i = 0; i < COUNT; i++)
        {
            channelFactory.protocols.get(i).onPreparedMembershipChanged(null, membership, null);
            channelFactory.membershipServices.get(i).membership = membership;
        }
        
        UUID flowId1 = UUID.randomUUID();
        UUID flowId2 = UUID.randomUUID();
        RemoteFlowId remoteFlow1 = new RemoteFlowId(channels[COUNT - 2].getLiveNodeProvider().getLocalNode(),
            channels[0].getLiveNodeProvider().getLocalNode(), flowId1);
        RemoteFlowId remoteFlow2 = new RemoteFlowId(channels[COUNT - 1].getLiveNodeProvider().getLocalNode(),
            channels[0].getLiveNodeProvider().getLocalNode(), flowId1);
        RemoteFlowId remoteFlow3 = new RemoteFlowId(channels[COUNT - 2].getLiveNodeProvider().getLocalNode(),
            channels[1].getLiveNodeProvider().getLocalNode(), flowId2);
        RemoteFlowId remoteFlow4 = new RemoteFlowId(channels[COUNT - 1].getLiveNodeProvider().getLocalNode(),
            channels[1].getLiveNodeProvider().getLocalNode(), flowId2);
        channelFactory.flowControlProtocols.get(0).lockFlow(remoteFlow1);
        channelFactory.flowControlProtocols.get(0).lockFlow(remoteFlow2);
        channelFactory.flowControlProtocols.get(0).lockFlow(remoteFlow1);
        channelFactory.flowControlProtocols.get(0).lockFlow(remoteFlow2);
        channelFactory.flowControlProtocols.get(1).lockFlow(remoteFlow3);
        channelFactory.flowControlProtocols.get(1).lockFlow(remoteFlow4);
        
        Threads.sleep(1000);
        
        FlowControllerMock controller1 = channelFactory.flowControllers.get(COUNT - 2);
        FlowControllerMock controller2 = channelFactory.flowControllers.get(COUNT - 1);
        
        assertThat(controller1.lockedFlows.size(), is(2));
        assertThat(controller1.unlockedFlows.size(), is(0));
        assertThat(controller1.lockedFlows.get(0).getFlowId(), is(flowId1));
        assertThat(controller1.lockedFlows.get(0).getReceiver(), is(channels[0].getLiveNodeProvider().getLocalNode()));
        assertThat(controller1.lockedFlows.get(0).getSender(), is(channels[COUNT - 2].getLiveNodeProvider().getLocalNode()));
        
        assertThat(controller1.lockedFlows.get(1).getFlowId(), is(flowId2));
        assertThat(controller1.lockedFlows.get(1).getReceiver(), is(channels[1].getLiveNodeProvider().getLocalNode()));
        assertThat(controller1.lockedFlows.get(1).getSender(), is(channels[COUNT - 2].getLiveNodeProvider().getLocalNode()));
        
        assertThat(controller2.lockedFlows.size(), is(2));
        assertThat(controller2.unlockedFlows.size(), is(0));
        assertThat(controller2.lockedFlows.get(0).getFlowId(), is(flowId1));
        assertThat(controller2.lockedFlows.get(0).getReceiver(), is(channels[0].getLiveNodeProvider().getLocalNode()));
        assertThat(controller2.lockedFlows.get(0).getSender(), is(channels[COUNT - 1].getLiveNodeProvider().getLocalNode()));
        
        assertThat(controller2.lockedFlows.get(1).getFlowId(), is(flowId2));
        assertThat(controller2.lockedFlows.get(1).getReceiver(), is(channels[1].getLiveNodeProvider().getLocalNode()));
        assertThat(controller2.lockedFlows.get(1).getSender(), is(channels[COUNT - 1].getLiveNodeProvider().getLocalNode()));
    
        channelFactory.flowControlProtocols.get(0).unlockFlow(remoteFlow1);
        channelFactory.flowControlProtocols.get(0).unlockFlow(remoteFlow2);
        channelFactory.flowControlProtocols.get(0).unlockFlow(remoteFlow1);
        channelFactory.flowControlProtocols.get(0).unlockFlow(remoteFlow2);
        channelFactory.flowControlProtocols.get(1).unlockFlow(remoteFlow3);
        channelFactory.flowControlProtocols.get(1).unlockFlow(remoteFlow4);
        
        Threads.sleep(1000);
        
        assertThat(controller1.unlockedFlows.size(), is(2));
        assertThat(controller1.unlockedFlows.get(0).getFlowId(), is(flowId1));
        assertThat(controller1.unlockedFlows.get(0).getReceiver(), is(channels[0].getLiveNodeProvider().getLocalNode()));
        assertThat(controller1.unlockedFlows.get(0).getSender(), is(channels[COUNT - 2].getLiveNodeProvider().getLocalNode()));
        
        assertThat(controller1.unlockedFlows.get(1).getFlowId(), is(flowId2));
        assertThat(controller1.unlockedFlows.get(1).getReceiver(), is(channels[1].getLiveNodeProvider().getLocalNode()));
        assertThat(controller1.unlockedFlows.get(1).getSender(), is(channels[COUNT - 2].getLiveNodeProvider().getLocalNode()));
        
        assertThat(controller2.unlockedFlows.size(), is(2));
        assertThat(controller2.unlockedFlows.get(0).getFlowId(), is(flowId1));
        assertThat(controller2.unlockedFlows.get(0).getReceiver(), is(channels[0].getLiveNodeProvider().getLocalNode()));
        assertThat(controller2.unlockedFlows.get(0).getSender(), is(channels[COUNT - 1].getLiveNodeProvider().getLocalNode()));
        
        assertThat(controller2.unlockedFlows.get(1).getFlowId(), is(flowId2));
        assertThat(controller2.unlockedFlows.get(1).getReceiver(), is(channels[1].getLiveNodeProvider().getLocalNode()));
        assertThat(controller2.unlockedFlows.get(1).getSender(), is(channels[COUNT - 1].getLiveNodeProvider().getLocalNode()));
    }
    
    @Test
    public void testFlowControlWithNodeFailures() throws Exception
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
        
        GroupMembership membership = new GroupMembership(1, new Group(new GroupAddress(UUID.randomUUID(), "test"), true, nodes));
        for (int i = 0; i < COUNT; i++)
        {
            channelFactory.protocols.get(i).onPreparedMembershipChanged(null, membership, null);
            channelFactory.membershipServices.get(i).membership = membership;
        }
        
        UUID flowId1 = UUID.randomUUID();
        UUID flowId2 = UUID.randomUUID();
        RemoteFlowId remoteFlow1 = new RemoteFlowId(channels[COUNT - 2].getLiveNodeProvider().getLocalNode(),
            channels[0].getLiveNodeProvider().getLocalNode(), flowId1);
        RemoteFlowId remoteFlow2 = new RemoteFlowId(channels[COUNT - 1].getLiveNodeProvider().getLocalNode(),
            channels[0].getLiveNodeProvider().getLocalNode(), flowId1);
        RemoteFlowId remoteFlow3 = new RemoteFlowId(channels[COUNT - 2].getLiveNodeProvider().getLocalNode(),
            channels[1].getLiveNodeProvider().getLocalNode(), flowId2);
        RemoteFlowId remoteFlow4 = new RemoteFlowId(channels[COUNT - 1].getLiveNodeProvider().getLocalNode(),
            channels[1].getLiveNodeProvider().getLocalNode(), flowId2);
        channelFactory.flowControlProtocols.get(0).lockFlow(remoteFlow1);
        channelFactory.flowControlProtocols.get(0).lockFlow(remoteFlow2);
        channelFactory.flowControlProtocols.get(0).lockFlow(remoteFlow1);
        channelFactory.flowControlProtocols.get(0).lockFlow(remoteFlow2);
        channelFactory.flowControlProtocols.get(1).lockFlow(remoteFlow3);
        channelFactory.flowControlProtocols.get(1).lockFlow(remoteFlow4);
        
        Threads.sleep(1000);
        
        FlowControllerMock controller1 = channelFactory.flowControllers.get(COUNT - 2);
        FlowControllerMock controller2 = channelFactory.flowControllers.get(COUNT - 1);
        
        assertThat(controller1.lockedFlows.size(), is(2));
        assertThat(controller1.unlockedFlows.size(), is(0));
        assertThat(controller1.lockedFlows.get(0).getFlowId(), is(flowId1));
        assertThat(controller1.lockedFlows.get(0).getReceiver(), is(channels[0].getLiveNodeProvider().getLocalNode()));
        assertThat(controller1.lockedFlows.get(0).getSender(), is(channels[COUNT - 2].getLiveNodeProvider().getLocalNode()));
        
        assertThat(controller1.lockedFlows.get(1).getFlowId(), is(flowId2));
        assertThat(controller1.lockedFlows.get(1).getReceiver(), is(channels[1].getLiveNodeProvider().getLocalNode()));
        assertThat(controller1.lockedFlows.get(1).getSender(), is(channels[COUNT - 2].getLiveNodeProvider().getLocalNode()));
        
        assertThat(controller2.lockedFlows.size(), is(2));
        assertThat(controller2.unlockedFlows.size(), is(0));
        assertThat(controller2.lockedFlows.get(0).getFlowId(), is(flowId1));
        assertThat(controller2.lockedFlows.get(0).getReceiver(), is(channels[0].getLiveNodeProvider().getLocalNode()));
        assertThat(controller2.lockedFlows.get(0).getSender(), is(channels[COUNT - 1].getLiveNodeProvider().getLocalNode()));
        
        assertThat(controller2.lockedFlows.get(1).getFlowId(), is(flowId2));
        assertThat(controller2.lockedFlows.get(1).getReceiver(), is(channels[1].getLiveNodeProvider().getLocalNode()));
        assertThat(controller2.lockedFlows.get(1).getSender(), is(channels[COUNT - 1].getLiveNodeProvider().getLocalNode()));
    
        channelFactory.protocols.get(2).addFailedMembers(com.exametrika.common.utils.Collections.asSet(
            nodes.get(0).getId(), nodes.get(1).getId()));
        channelFactory.protocols.get(1).addFailedMembers(com.exametrika.common.utils.Collections.asSet(
            nodes.get(0).getId(), nodes.get(1).getId()));
        
        failChannel(channels[0]);
        failChannel(channels[1]);
        
        Threads.sleep(3000);
        
        assertThat(controller1.unlockedFlows.size(), is(2));
        assertThat(controller1.unlockedFlows.get(0).getFlowId(), is(flowId1));
        assertThat(controller1.unlockedFlows.get(0).getReceiver(), is(channels[0].getLiveNodeProvider().getLocalNode()));
        assertThat(controller1.unlockedFlows.get(0).getSender(), is(channels[COUNT - 2].getLiveNodeProvider().getLocalNode()));
        
        assertThat(controller1.unlockedFlows.get(1).getFlowId(), is(flowId2));
        assertThat(controller1.unlockedFlows.get(1).getReceiver(), is(channels[1].getLiveNodeProvider().getLocalNode()));
        assertThat(controller1.unlockedFlows.get(1).getSender(), is(channels[COUNT - 2].getLiveNodeProvider().getLocalNode()));
        
        assertThat(controller2.unlockedFlows.size(), is(2));
        assertThat(controller2.unlockedFlows.get(0).getFlowId(), is(flowId1));
        assertThat(controller2.unlockedFlows.get(0).getReceiver(), is(channels[0].getLiveNodeProvider().getLocalNode()));
        assertThat(controller2.unlockedFlows.get(0).getSender(), is(channels[COUNT - 1].getLiveNodeProvider().getLocalNode()));
        
        assertThat(controller2.unlockedFlows.get(1).getFlowId(), is(flowId2));
        assertThat(controller2.unlockedFlows.get(1).getReceiver(), is(channels[1].getLiveNodeProvider().getLocalNode()));
        assertThat(controller2.unlockedFlows.get(1).getSender(), is(channels[COUNT - 1].getLiveNodeProvider().getLocalNode()));
    }
    
    @Test
    public void testCapacityController()
    {
        QueueCapacityController controller = new QueueCapacityController(1000, 500, GroupMemberships.CORE_GROUP_ADDRESS, 
            GroupMemberships.CORE_GROUP_ID);
        FlowControllerMock flowController = new FlowControllerMock();
        controller.setFlowController(flowController);
        IAddress address1 = new TestAddress(UUID.randomUUID(), "test1");
        IAddress address2 = new TestAddress(UUID.randomUUID(), "test2");
        controller.addCapacity(address1, 600);
        
        assertThat(flowController.lockedFlows.size(), is(0));
        
        controller.addCapacity(address2, 600);
        controller.addCapacity(address1, 600);
        controller.addCapacity(address1, 600);
        controller.addCapacity(address1, 600);
        
        assertThat(controller.getCapacity(), is(3000));
        assertThat(flowController.lockedFlows.size(), is(2));
        assertThat(flowController.lockedFlows.get(0).getSender(), is(address2));
        assertThat(flowController.lockedFlows.get(1).getSender(), is(address1));
        
        controller.removeCapacity(500);
        assertThat(flowController.unlockedFlows.size(), is(0));
        controller.removeCapacity(2000);
        
        assertThat(controller.getCapacity(), is(500));
        assertThat(flowController.unlockedFlows.size(), is(2));
        assertThat(flowController.unlockedFlows.get(0).getSender(), is(address2));
        assertThat(flowController.unlockedFlows.get(1).getSender(), is(address1));
        
        controller.removeCapacity(500);
        assertThat(flowController.unlockedFlows.size(), is(2));
        
        flowController.lockedFlows.clear();
        flowController.unlockedFlows.clear();
        
        controller.addCapacity(address1, 600);
        controller.addCapacity(address1, 600);
        controller.addCapacity(address2, 600);
        
        controller.clearCapacity();
        assertThat(controller.getCapacity(), is(0));
        assertThat(flowController.unlockedFlows.size(), is(2));
        assertThat(flowController.unlockedFlows.get(0).getSender(), is(address1));
        assertThat(flowController.unlockedFlows.get(1).getSender(), is(address2));
    }
    
    private  static void failChannel(IChannel channel) throws Exception
    {
        ((TcpNioDispatcher)Tests.get(((Channel)channel).getCompartment(), "dispatcher")).getSelector().close();
        IOs.close(channel);
    }
    
    private static class FlowControllerMock implements IFlowController<RemoteFlowId>
    {
        private List<RemoteFlowId> lockedFlows = new ArrayList<RemoteFlowId>();
        private List<RemoteFlowId> unlockedFlows = new ArrayList<RemoteFlowId>();
        
        @Override
        public synchronized void lockFlow(RemoteFlowId flow)
        {
            lockedFlows.add(flow);
        }

        @Override
        public void unlockFlow(RemoteFlowId flow)
        {
            unlockedFlows.add(flow);
        }
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
    
    public static class ChannelReconnectorMock implements IChannelReconnector
    {
        @Override
        public void reconnect()
        {
        }
    }
    
    private class TestChannelFactory extends ChannelFactory
    {
        private long failureUpdatePeriod = 500;
        private  long failureHistoryPeriod = 2000;
        private  int maxShunCount = 3;
        private List<CoreGroupFailureDetectionProtocol> protocols = new ArrayList<CoreGroupFailureDetectionProtocol>();
        private List<FlowControlProtocol> flowControlProtocols = new ArrayList<FlowControlProtocol>();
        private List<FlowControllerMock> flowControllers = new ArrayList<FlowControllerMock>();
        private List<MembershipServiceMock> membershipServices = new ArrayList<MembershipServiceMock>();
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
            MembershipServiceMock membershipService = new MembershipServiceMock(liveNodeProvider);
            membershipServices.add(membershipService);
            ChannelReconnectorMock channelReconnector = new ChannelReconnectorMock();
            
            FlowControllerMock flowController = new FlowControllerMock();
            flowControllers.add(flowController);
            
            FlowControlProtocol flowControlProtocol = new FlowControlProtocol(channelName, messageFactory,
                membershipService);
            flowControlProtocol.setFlowController(flowController);
            protocols.add(flowControlProtocol);
            flowControlProtocols.add(flowControlProtocol);
            
            CoreGroupFailureDetectionProtocol failureDetectionProtocol = new CoreGroupFailureDetectionProtocol(channelName, messageFactory, membershipService, 
                Collections.<IFailureDetectionListener>singleton(flowControlProtocol), failureUpdatePeriod,
                failureHistoryPeriod, maxShunCount);
            failureDetectionProtocol.setChannelReconnector(channelReconnector);
            protocols.add(failureDetectionProtocol);
            failureObservers.add(failureDetectionProtocol);
            this.protocols.add(failureDetectionProtocol);
            flowControlProtocol.setFailureDetector(failureDetectionProtocol);
            
            serializationRegistry.register(new GroupMembershipSerializationRegistrar());
        }
        
        @Override
        protected void wireProtocols(Channel channel, TcpTransport transport, ProtocolStack protocolStack)
        {
            GroupNodeTrackingStrategy strategy = (GroupNodeTrackingStrategy)protocolStack.find(HeartbeatProtocol.class).getNodeTrackingStrategy();
            CoreGroupFailureDetectionProtocol failureDetectionProtocol = protocolStack.find(CoreGroupFailureDetectionProtocol.class);
            failureDetectionProtocol.setFailureObserver(transport);
            strategy.setFailureDetector(failureDetectionProtocol);
            strategy.setMembershipManager((IGroupMembershipManager)failureDetectionProtocol.getMembersipService());
        }
    }
}
