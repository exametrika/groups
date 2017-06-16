/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.messaging.impl.SubChannel;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Serializers;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannel;
import com.exametrika.impl.groups.cluster.channel.WorkerNodeChannel;
import com.exametrika.impl.groups.cluster.exchange.CoreFeedbackProtocol;
import com.exametrika.impl.groups.cluster.exchange.IFeedbackProvider;
import com.exametrika.impl.groups.cluster.feedback.DataLossFeedbackData;
import com.exametrika.impl.groups.cluster.feedback.DataLossFeedbackProvider;
import com.exametrika.impl.groups.cluster.feedback.DataLossState;
import com.exametrika.impl.groups.cluster.feedback.GroupFeedbackData;
import com.exametrika.impl.groups.cluster.feedback.GroupFeedbackProvider;
import com.exametrika.impl.groups.cluster.feedback.GroupState;
import com.exametrika.impl.groups.cluster.feedback.IDataLossState;
import com.exametrika.impl.groups.cluster.feedback.IGroupState;
import com.exametrika.impl.groups.cluster.feedback.INodeFeedbackService;
import com.exametrika.impl.groups.cluster.feedback.INodeState;
import com.exametrika.impl.groups.cluster.feedback.INodeState.State;
import com.exametrika.impl.groups.cluster.feedback.NodeFeedbackData;
import com.exametrika.impl.groups.cluster.feedback.NodeFeedbackProvider;
import com.exametrika.impl.groups.cluster.feedback.NodeState;
import com.exametrika.impl.groups.cluster.membership.ClusterMembership;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;
import com.exametrika.impl.groups.cluster.membership.DomainMembership;
import com.exametrika.impl.groups.cluster.membership.Group;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.GroupsMembership;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;
import com.exametrika.spi.groups.cluster.state.IDataLossObserver;
import com.exametrika.tests.groups.mocks.ClusterMembershipListenerMock;
import com.exametrika.tests.groups.mocks.LiveNodeProviderMock;
import com.exametrika.tests.groups.mocks.PropertyProviderMock;

public class ClusterFeedbackTests extends AbstractClusterTests
{
    private static final int CORE_NODE_COUNT = 5;
    private static final int WORKER_NODE_COUNT = 10;
    
    @Before
    public void setUp()
    {
        createCluster(CORE_NODE_COUNT, WORKER_NODE_COUNT);
    }
    
    @After
    public void tearDown()
    {
        stopCluster();
    }
    
    @Test
    public void testDatalossFeedbackProvider()
    {
        DataLossFeedbackProvider provider = new DataLossFeedbackProvider();
        assertTrue(provider.getData(true) == null);

        DataLossState state = new DataLossState("test", UUID.randomUUID());
        provider.updateDataLossState(state);

        DataLossFeedbackData data = (DataLossFeedbackData)provider.getData(false);
        checkDataLossStates(data.getStates(), Collections.singletonList(state));
        assertTrue(provider.getData(false) == null);

        data = (DataLossFeedbackData)provider.getData(true);
        checkDataLossStates(data.getStates(), Collections.singletonList(state));

        ClusterMembership membership = new ClusterMembership(1, Collections.<IDomainMembership> emptyList(), null);
        provider.onClusterMembershipChanged(membership);
        assertTrue(((DataLossFeedbackData)provider.getData(true)).getStates().isEmpty());

        provider.setData(data);
        data = (DataLossFeedbackData)provider.getData(false);
        checkDataLossStates(data.getStates(), Collections.singletonList(state));
    }
    
    @Test
    public void testDatalossFeedbackObserver()
    {
        LiveNodeProviderMock liveNodeProvider = new LiveNodeProviderMock();
        PropertyProviderMock propertyProvider = new PropertyProviderMock();
        ClusterMembershipListenerMock listener = new ClusterMembershipListenerMock();
        LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeProvider, propertyProvider, 
            GroupMemberships.CORE_DOMAIN);
        ClusterMembershipManager manager = new ClusterMembershipManager("test", localNodeProvider, 
            com.exametrika.common.utils.Collections.<IClusterMembershipListener>asSet(listener));
        manager.start();
        
        Group group1 = new Group(new GroupAddress(UUID.randomUUID(), "test"), true, Arrays.<INode>asList(),
            Enums.noneOf(GroupOption.class));
        GroupsMembership groupsMembership = new GroupsMembership(Arrays.<IGroup>asList(group1));
        DomainMembership domainMembership1 = new DomainMembership("domain1", Arrays.<IClusterMembershipElement>asList(
            groupsMembership));
        ClusterMembership membership = new ClusterMembership(1, Arrays.asList(domainMembership1), null);
        manager.installMembership(membership);
        
        GroupFeedbackProvider groupProvider = new GroupFeedbackProvider();
        GroupState groupState = new GroupState("test", group1.getId(), 1, Arrays.asList(UUID.randomUUID()), true, 
            IGroupState.State.NORMAL);
        groupProvider.updateGroupState(groupState);
        
        DataLossObserverMock dataLossObserver = new DataLossObserverMock();
        DataLossFeedbackProvider provider = new DataLossFeedbackProvider(dataLossObserver, manager, groupProvider);
        DataLossState state = new DataLossState("test", group1.getId());
        provider.updateDataLossState(state);
        
        assertTrue(dataLossObserver.group == group1);
    }
    
    @Test
    public void testGroupFeedbackProvider()
    {
        GroupFeedbackProvider provider = new GroupFeedbackProvider();
        assertTrue(provider.getData(true) == null);

        GroupState state = new GroupState("test", UUID.randomUUID(), 1, Arrays.asList(UUID.randomUUID()), true, 
            IGroupState.State.FLUSH);
        provider.updateGroupState(state);
        assertTrue(provider.findGroupState(state.getId()) == state);
        checkGroupStates(provider.getGroupStates(), Collections.singleton(state));

        GroupFeedbackData data = (GroupFeedbackData)provider.getData(false);
        checkGroupStates(data.getStates(), Collections.singletonList(state));
        assertTrue(provider.getData(false) == null);

        data = (GroupFeedbackData)provider.getData(true);
        checkGroupStates(data.getStates(), Collections.singletonList(state));

        ClusterMembership membership = new ClusterMembership(1, Collections.<IDomainMembership> emptyList(), null);
        provider.onClusterMembershipChanged(membership);
        assertTrue(provider.getGroupStates().isEmpty());

        provider.setData(data);
        checkGroupStates(provider.getGroupStates(), Collections.singletonList(state));
    }
    
    @Test
    public void testNodeFeedbackProvider()
    {
        NodeFeedbackProvider provider = new NodeFeedbackProvider();
        assertTrue(provider.getData(true) == null);
        
        NodeState state = new NodeState("test", UUID.randomUUID(), State.GRACEFUL_EXIT_REQUESTED);
        provider.updateNodeState(state);
        assertTrue(provider.findNodeState(state.getId()) == state);
        checkNodeStates(provider.getNodeStates(), Collections.singleton(state));
        
        NodeFeedbackData data = (NodeFeedbackData)provider.getData(false);
        checkNodeStates(data.getStates(), Collections.singletonList(state));
        assertTrue(provider.getData(false) == null);
        
        data = (NodeFeedbackData)provider.getData(true);
        checkNodeStates(data.getStates(), Collections.singletonList(state));
        
        ClusterMembership membership = new ClusterMembership(1, Collections.<IDomainMembership>emptyList(), null);
        provider.onClusterMembershipChanged(membership);
        assertTrue(provider.getNodeStates().isEmpty());
        
        provider.setData(data);
        checkNodeStates(provider.getNodeStates(), Collections.singletonList(state));
    }
    
    @Test
    public void testSerializers()
    {
        NodeFeedbackProvider nodeProvider = new NodeFeedbackProvider();
        NodeState nodeState = new NodeState("test", UUID.randomUUID(), State.GRACEFUL_EXIT_REQUESTED);
        nodeProvider.updateNodeState(nodeState);
        NodeFeedbackData nodeData = (NodeFeedbackData)nodeProvider.getData(false);
        
        GroupFeedbackProvider groupProvider = new GroupFeedbackProvider();
        GroupState groupState = new GroupState("test", UUID.randomUUID(), 1, Arrays.asList(UUID.randomUUID()), true, 
            IGroupState.State.FLUSH);
        groupProvider.updateGroupState(groupState);
        GroupFeedbackData groupData = (GroupFeedbackData)groupProvider.getData(false);
        
        DataLossFeedbackProvider dataLossProvider = new DataLossFeedbackProvider();
        DataLossState dataLossState = new DataLossState("test", UUID.randomUUID());
        dataLossProvider.updateDataLossState(dataLossState);

        DataLossFeedbackData dataLossData = (DataLossFeedbackData)dataLossProvider.getData(false);
        
        ISerializationRegistry registry = Serializers.createRegistry();
        registry.register(nodeProvider);
        registry.register(groupProvider);
        registry.register(dataLossProvider);

        ByteOutputStream stream = new ByteOutputStream(0x1000);
        ISerialization serialization = new Serialization(registry, true, stream);
        serialization.writeObject(nodeData);
        serialization.writeObject(groupData);
        serialization.writeObject(dataLossData);
        
        ByteInputStream in = new ByteInputStream(stream.getBuffer(), 0, stream.getLength());
        IDeserialization deserialization = new Deserialization(registry, in);
        NodeFeedbackData nodeData1 = deserialization.readObject();
        GroupFeedbackData groupData1 = deserialization.readObject();
        DataLossFeedbackData dataLossData1 = deserialization.readObject();
        
        checkNodeStates(nodeData1.getStates(), nodeData.getStates());
        checkGroupStates(groupData1.getStates(), groupData.getStates());
        checkDataLossStates(dataLossData1.getStates(), dataLossData.getStates());
    }
    
    @Test
    public void testProtocol() throws Throwable
    {
        startCoreNodes(null);
        Threads.sleep(2000);
        
        startWorkerNodes(null);
        Threads.sleep(2000);
        
        checkClusterWorkerNodesMembership(null);
        
        WorkerNodeChannel workerNode = workerChannels.get(0);
        INode localNode = workerNode.getMembershipService().getLocalNode();
        INodeFeedbackService feedbackService = Tests.get(workerNode, "feedbackService");
        INodeState state = new NodeState(localNode.getDomain(), localNode.getId(), INodeState.State.GRACEFUL_EXIT_REQUESTED);
        feedbackService.updateNodeState(state);
        Threads.sleep(200);
        
        CoreNodeChannel coordinator = coreChannels.get(findCoordinator());
        checkFeedbackState(coordinator, state);
        CoreNodeChannel controller = coreChannels.get(findController(workerNode.getMembershipService().getLocalNode()));
        checkFeedbackState(controller, state);
        
        controller.stop();
        Threads.sleep(2000);
        
        CoreNodeChannel newController = coreChannels.get(findController(workerNode.getMembershipService().getLocalNode()));
        assertTrue(newController != controller);
        checkFeedbackState(newController, state);
        
        coordinator.stop();
        Threads.sleep(2000);
        
        CoreNodeChannel newCoordinator = coreChannels.get(findCoordinator());
        assertTrue(newCoordinator != coordinator);
        checkFeedbackState(newCoordinator, state);
        
        workerNode.stop();
        Threads.sleep(2000);
        
        checkFeedbackState(newController, null);
        checkFeedbackState(newCoordinator, null);
    }
    
    private void checkFeedbackState(CoreNodeChannel node, INodeState nodeState) throws Throwable
    {
        CoreFeedbackProtocol protocol = ((SubChannel)node.getMainSubChannel()).getProtocolStack().find(CoreFeedbackProtocol.class);
        List<IFeedbackProvider> providers = Tests.get(protocol, "feedbackProviders");
        NodeFeedbackProvider nodeProvider = null;
        for (IFeedbackProvider provider : providers)
        {
            if (provider instanceof NodeFeedbackProvider)
            {
                nodeProvider = (NodeFeedbackProvider)provider;
                break;
            }
        }
        
        if (nodeState != null)
            checkNodeStates(nodeProvider.getNodeStates(), Arrays.asList(nodeState));
        else
            assertTrue(nodeProvider.getNodeStates().isEmpty());
    }
    
    private void checkNodeStates(Collection<? extends INodeState> result, Collection<? extends INodeState> ethalon)
    {
        assertThat(result.size(), is(ethalon.size()));
        assertThat(result.size(), is(1));
        INodeState first = result.iterator().next();
        INodeState second = ethalon.iterator().next();
        
        assertThat(first.getDomain(), is(second.getDomain()));
        assertThat(first.getId(), is(second.getId()));
        assertThat(first.getState(), is(second.getState()));
    }
    
    private void checkGroupStates(Collection<? extends IGroupState> result, Collection<? extends IGroupState> ethalon)
    {
        assertThat(result.size(), is(ethalon.size()));
        assertThat(result.size(), is(1));
        IGroupState first = result.iterator().next();
        IGroupState second = ethalon.iterator().next();
        
        assertThat(first.getDomain(), is(second.getDomain()));
        assertThat(first.getId(), is(second.getId()));
        assertThat(first.getMembershipId(), is(second.getMembershipId()));
        assertThat(first.isPrimary(), is(second.isPrimary()));
        assertThat(first.getMembers(), is(second.getMembers()));
        assertThat(first.getState(), is(second.getState()));
    }
    
    private void checkDataLossStates(Collection<? extends IDataLossState> result, Collection<? extends IDataLossState> ethalon)
    {
        assertThat(result.size(), is(ethalon.size()));
        assertThat(result.size(), is(1));
        IDataLossState first = result.iterator().next();
        IDataLossState second = ethalon.iterator().next();
        
        assertThat(first.getDomain(), is(second.getDomain()));
        assertThat(first.getId(), is(second.getId()));
    }
    
    private class DataLossObserverMock implements IDataLossObserver
    {
        private IGroup group;

        @Override
        public void onDataLoss(IGroup group)
        {
            this.group = group;
        }
    }
}
