/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.unit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IDomainMembershipChange;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.IMembershipListener.LeaveReason;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.api.groups.cluster.INodesMembership;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;
import com.exametrika.common.messaging.impl.transports.UnicastAddressSerializer;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.groups.cluster.membership.ClusterMembership;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipChange;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipSerializationRegistrar;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipStateTransferFactory;
import com.exametrika.impl.groups.cluster.membership.DomainMembership;
import com.exametrika.impl.groups.cluster.membership.DomainMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.Group;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.GroupDefinition;
import com.exametrika.impl.groups.cluster.membership.GroupDefinitionStateTransferFactory;
import com.exametrika.impl.groups.cluster.membership.GroupDelta;
import com.exametrika.impl.groups.cluster.membership.GroupLeaveGracefulExitStrategy;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.GroupsMembership;
import com.exametrika.impl.groups.cluster.membership.GroupsMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.GroupsMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.ICoreClusterMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.IGroupDelta;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;
import com.exametrika.impl.groups.cluster.membership.Node;
import com.exametrika.impl.groups.cluster.membership.NodesMembership;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembership;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembershipProvider;
import com.exametrika.spi.groups.cluster.state.ISimpleStateTransferClient;
import com.exametrika.spi.groups.cluster.state.ISimpleStateTransferServer;
import com.exametrika.tests.groups.mocks.ClusterMembershipListenerMock;
import com.exametrika.tests.groups.mocks.GroupManagementServiceMock;
import com.exametrika.tests.groups.mocks.LiveNodeProviderMock;
import com.exametrika.tests.groups.mocks.PropertyProviderMock;
import com.exametrika.tests.groups.mocks.SimpleStateStoreMock;

/**
 * The {@link ClusterMembershipManagerTests} are tests for cluster membership manager.
 * 
 * @author Medvedev-A
 */
public class ClusterMembershipManagerTests
{
    @Test
    public void testClusterMembership()
    {
        ClusterMembership clusterMembership = createClusterMembership();
        ClusterMembership clusterMembership2 = new ClusterMembership(1, Collections.<IDomainMembership>emptyList(), null);
        ClusterMembership clusterMembership3 = new ClusterMembership(2, Collections.<IDomainMembership>emptyList(), null);
        
        IDomainMembership domainMembership1 = clusterMembership.getDomains().get(0);
        IDomainMembership domainMembership2 = clusterMembership.getDomains().get(1);
        
        assertThat(clusterMembership, is(clusterMembership2));
        assertThat(clusterMembership, not(is(clusterMembership3)));
        assertThat(clusterMembership.getDomains(), is(Arrays.<IDomainMembership>asList(domainMembership1, domainMembership2)));
        assertThat(clusterMembership.findDomain("domain1"), is(clusterMembership.getDomains().get(0)));
        
        assertThat(domainMembership1.findElement(INodesMembership.class), is((INodesMembership)domainMembership1.getElements().get(0)));
    }
    
    @Test
    public void testClusterMembershipDelta()
    {
        Node node1 = new Node(new UnicastAddress(UUID.randomUUID(), "node1"), Collections.<String, Object>singletonMap("key", "value"), "core");
        UUID leftNodeId = UUID.randomUUID();
        UUID failedNodeId = UUID.randomUUID();
        Group group1 = new Group(new GroupAddress(UUID.randomUUID(), "test"), true, Arrays.<INode>asList(node1),
            Enums.noneOf(GroupOption.class));
        UUID removedGroupId = UUID.randomUUID();
        IGroupDelta changedGroup = new GroupDelta(UUID.randomUUID(), true, Arrays.<INode>asList(node1), 
            com.exametrika.common.utils.Collections.asSet(leftNodeId), com.exametrika.common.utils.Collections.asSet(failedNodeId));
        NodesMembershipDelta nodesMembershipDelta = new NodesMembershipDelta(Arrays.<INode>asList(node1), 
            com.exametrika.common.utils.Collections.asSet(leftNodeId), com.exametrika.common.utils.Collections.asSet(failedNodeId));
        GroupsMembershipDelta groupsMembershipDelta = new GroupsMembershipDelta(Arrays.<IGroup>asList(group1), 
            com.exametrika.common.utils.Collections.asSet(changedGroup), 
            com.exametrika.common.utils.Collections.asSet(removedGroupId));
        WorkerToCoreMembershipDelta workerToCoreMembershipDelta = new WorkerToCoreMembershipDelta(Arrays.<INode>asList(node1), 
            com.exametrika.common.utils.Collections.asSet(leftNodeId), com.exametrika.common.utils.Collections.asSet(failedNodeId), 
            new MapBuilder<UUID, UUID>().put(UUID.randomUUID(), UUID.randomUUID()).toMap());
        
        DomainMembershipDelta domainMembershipDelta = new DomainMembershipDelta("domain", Arrays.asList(nodesMembershipDelta,
            groupsMembershipDelta, workerToCoreMembershipDelta));
        ClusterMembershipDelta clusterMembershipDelta = new ClusterMembershipDelta(1, true, Arrays.asList(domainMembershipDelta), null);
        
        ISerializationRegistry registry = Serializers.createRegistry();
        registry.register(new ClusterMembershipSerializationRegistrar());
        registry.register(new UnicastAddressSerializer());
        ByteOutputStream stream = new ByteOutputStream(0x1000);
        ISerialization serialization = new Serialization(registry, true, stream);
        serialization.writeObject(clusterMembershipDelta);
        
        ByteInputStream in = new ByteInputStream(stream.getBuffer(), 0, stream.getLength());
        IDeserialization deserialization = new Deserialization(registry, in);
        ClusterMembershipDelta result = deserialization.readObject();
        assertNotNull(result);
    }
    
    @Test
    public void testMembershipManager() throws Exception
    {
        LiveNodeProviderMock liveNodeProvider = new LiveNodeProviderMock();
        PropertyProviderMock propertyProvider = new PropertyProviderMock();
        ClusterMembershipListenerMock listener = new ClusterMembershipListenerMock();
        LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeProvider, propertyProvider, 
            GroupMemberships.CORE_DOMAIN);
        ClusterMembershipManager manager = new ClusterMembershipManager("test", localNodeProvider, 
            com.exametrika.common.utils.Collections.<IClusterMembershipListener>asSet(listener));
        
        manager.start();
        assertThat(manager.getLocalNode().getAddress(), is(liveNodeProvider.getLocalNode()));
        assertThat(manager.getLocalNode().getId(), is(liveNodeProvider.getLocalNode().getId()));
        assertThat(manager.getLocalNode().getName(), is(liveNodeProvider.getLocalNode().getName()));
        assertThat(manager.getLocalNode().getProperties(), is(Collections.<String, Object>singletonMap("key", "value")));
        
        ClusterMembership membership = new ClusterMembership(1l, Collections.<IDomainMembership>emptyList(), null);
        
        manager.installMembership(membership);
        assertThat(manager.getMembership(), is((IClusterMembership)membership));
        assertTrue(listener.onJoined);
        
        ClusterMembership membership2 = new ClusterMembership(2l, Collections.<IDomainMembership>emptyList(), null);
        ClusterMembershipChange membershipChange = new ClusterMembershipChange(Collections.<IDomainMembership>emptyList(), 
            Collections.<IDomainMembershipChange>emptyList(), Collections.<IDomainMembership>emptySet(), null);
        
        manager.changeMembership(membership2, membershipChange);
        
        assertThat(manager.getMembership(), is((IClusterMembership)membership2));
       
        assertThat(listener.onMembershipChangedEvent.getNewMembership(), is((IClusterMembership)membership2));
        assertThat(listener.onMembershipChangedEvent.getOldMembership(), is((IClusterMembership)membership));
        assertThat(listener.onMembershipChangedEvent.getMembershipChange() == membershipChange, is(true));
        
        manager.uninstallMembership(IGroupMembershipListener.LeaveReason.GRACEFUL_EXIT);
        assertThat(manager.getMembership() == null, is(true));
        assertThat(listener.leaveReason, is(LeaveReason.GRACEFUL_EXIT));
        
        manager.stop();
    }
    
    @Test
    public void testGroupLeaveGracefulStrategy() throws Exception
    {
        LiveNodeProviderMock liveNodeProvider = new LiveNodeProviderMock();
        PropertyProviderMock propertyProvider = new PropertyProviderMock();
        ClusterMembershipListenerMock listener = new ClusterMembershipListenerMock();
        LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeProvider, propertyProvider, 
            "domain1");
        ClusterMembershipManager manager = new ClusterMembershipManager("test", localNodeProvider, 
            com.exametrika.common.utils.Collections.<IClusterMembershipListener>asSet(listener));
        
        GroupLeaveGracefulExitStrategy strategy = new GroupLeaveGracefulExitStrategy(manager);
        
        INode node1 = localNodeProvider.getLocalNode();
        Node node2 = new Node(new UnicastAddress(UUID.randomUUID(), "node2"), Collections.<String, Object>singletonMap("key", "value"), "core");
        
        NodesMembership nodesMembership = new NodesMembership(Arrays.asList(node1, node2));
        assertThat(nodesMembership.getNodes(), is(Arrays.<INode>asList(node1, node2)));
        assertThat(nodesMembership.findNode(node1.getId()), is(node1));
        assertThat(nodesMembership.findNode(node1.getAddress()), is(node1));
        
        Group group1 = new Group(new GroupAddress(UUID.randomUUID(), "test"), true, Arrays.<INode>asList(node1, node2),
            Enums.noneOf(GroupOption.class));
        Group group2 = new Group(new GroupAddress(UUID.randomUUID(), "test"), true, Arrays.<INode>asList(node1, node2),
            Enums.noneOf(GroupOption.class));
        
        GroupsMembership groupsMembership = new GroupsMembership(Arrays.<IGroup>asList(group1, group2));
        assertThat(groupsMembership.getGroups(), is(Arrays.<IGroup>asList(group1, group2)));
        assertThat(groupsMembership.findGroup(group1.getId()), is((IGroup)group1));
        assertThat(groupsMembership.findGroup(group1.getAddress()), is((IGroup)group1));
        assertThat(groupsMembership.findNodeGroups(node1.getId()), is(Arrays.<IGroup>asList(group1, group2)));
        
        DomainMembership domainMembership1 = new DomainMembership("domain1", Arrays.<IClusterMembershipElement>asList(
            nodesMembership, groupsMembership));
        DomainMembership domainMembership2 = new DomainMembership("domain2", Collections.<IClusterMembershipElement>emptyList());
        ClusterMembership clusterMembership = new ClusterMembership(1, Arrays.asList(domainMembership1, domainMembership2), null);
        manager.installMembership(clusterMembership);
        
        assertThat(strategy.requestExit(), is(false));
        
        ClusterMembership membership2 = new ClusterMembership(2l, Collections.<IDomainMembership>emptyList(), null);
        ClusterMembershipChange membershipChange = new ClusterMembershipChange(Collections.<IDomainMembership>emptyList(), 
            Collections.<IDomainMembershipChange>emptyList(), Collections.<IDomainMembership>emptySet(), null);
        
        manager.changeMembership(membership2, membershipChange);
        
        assertThat(strategy.requestExit(), is(true));
    }
    
    @Test
    public void testClusterMembershipStateTransferFactory() throws Exception
    {
        LiveNodeProviderMock liveNodeProvider = new LiveNodeProviderMock();
        PropertyProviderMock propertyProvider = new PropertyProviderMock();
        LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeProvider, propertyProvider, 
            "domain1");
        ClusterMembershipManager serverManager = new ClusterMembershipManager("test", localNodeProvider, 
            Collections.<IClusterMembershipListener>emptySet());
        ClusterMembershipManager clientManager = new ClusterMembershipManager("test", localNodeProvider, 
            Collections.<IClusterMembershipListener>emptySet());
        
        NodesMembershipProvider nodesMembershipProvider = new NodesMembershipProvider();
        GroupsMembershipProvider groupsMembershipProvider = new GroupsMembershipProvider();
        WorkerToCoreMembershipProvider workerToCoreMembershipProvider = new WorkerToCoreMembershipProvider();
        ClusterMembershipStateTransferFactory serverFactory = new ClusterMembershipStateTransferFactory(serverManager, 
            Arrays.asList(nodesMembershipProvider, groupsMembershipProvider),
            Arrays.<ICoreClusterMembershipProvider>asList(workerToCoreMembershipProvider), new SimpleStateStoreMock());
        ISimpleStateTransferServer server = serverFactory.createServer(GroupMemberships.CORE_GROUP_ID);
        
        ClusterMembershipStateTransferFactory clientFactory = new ClusterMembershipStateTransferFactory(clientManager, 
            Arrays.asList(nodesMembershipProvider, groupsMembershipProvider),
            Arrays.<ICoreClusterMembershipProvider>asList(workerToCoreMembershipProvider), new SimpleStateStoreMock());
        ISimpleStateTransferClient client = clientFactory.createClient(GroupMemberships.CORE_GROUP_ID);
        
        ByteArray state = server.saveSnapshot(true);
        client.loadSnapshot(true, state);
        assertTrue(clientManager.getMembership() == null);
        
        IClusterMembership membership = createClusterMembership();
        serverManager.installMembership(membership);
        
        state = server.saveSnapshot(true);
        client.loadSnapshot(true, state);
        assertThat(clientManager.getMembership(), is(membership));
    }
    
    @Test
    public void testGroupDefinitionStateTransferFactory() throws Exception
    {
        GroupManagementServiceMock serverManager = new GroupManagementServiceMock();
        GroupDefinitionStateTransferFactory serverFactory = new GroupDefinitionStateTransferFactory(new SimpleStateStoreMock());
        serverFactory.setGroupManagementService(serverManager);
        ISimpleStateTransferServer server = serverFactory.createServer(GroupMemberships.CORE_GROUP_ID);
        
        GroupManagementServiceMock clientManager = new GroupManagementServiceMock();
        GroupDefinitionStateTransferFactory clientFactory = new GroupDefinitionStateTransferFactory(new SimpleStateStoreMock());
        clientFactory.setGroupManagementService(clientManager);
        ISimpleStateTransferClient client = clientFactory.createClient(GroupMemberships.CORE_GROUP_ID);
        
        GroupDefinition group1 = new GroupDefinition("domain1", UUID.randomUUID(), "group1", Enums.of(GroupOption.DURABLE),
            "filter1", 10, "type");
        GroupDefinition group2 = new GroupDefinition("domain2", UUID.randomUUID(), "group2", Enums.noneOf(GroupOption.class),
            null, 10, null);
        serverManager.addGroupDefinition(group1);
        serverManager.addGroupDefinition(group2);
        
        ByteArray state = server.saveSnapshot(true);
        client.loadSnapshot(true, state);
        assertTrue(clientManager.getGroupDefinitions().size() == 2);
        
        GroupDefinition group11 = clientManager.getGroupDefinitions().get(0);
        GroupDefinition group21 = clientManager.getGroupDefinitions().get(1);
        
        checkGroupDefinition(group1, group11);
        checkGroupDefinition(group2, group21);
    }

    private void checkGroupDefinition(GroupDefinition group1, GroupDefinition group2)
    {
        assertThat(group2.getDomain(), is(group1.getDomain()));
        assertThat(group2.getId(), is(group1.getId()));
        assertThat(group2.getName(), is(group1.getName()));
        assertThat(group2.getOptions(), is(group1.getOptions()));
        assertThat(group2.getNodeFilterExpression(), is(group1.getNodeFilterExpression()));
        assertThat(group2.getNodeCount(), is(group1.getNodeCount()));
        assertThat(group2.getType(), is(group1.getType()));
    }
    
    private ClusterMembership createClusterMembership()
    {
        Node node1 = new Node(new UnicastAddress(UUID.randomUUID(), "node1"), Collections.<String, Object>singletonMap("key", "value"), "domain1");
        Node node2 = new Node(new UnicastAddress(UUID.randomUUID(), "node2"), Collections.<String, Object>singletonMap("key", "value"), "dimain1");
        
        NodesMembership nodesMembership = new NodesMembership(Arrays.asList(node1, node2));
        assertThat(nodesMembership.getNodes(), is(Arrays.<INode>asList(node1, node2)));
        assertThat(nodesMembership.findNode(node1.getId()), is((INode)node1));
        assertThat(nodesMembership.findNode(node1.getAddress()), is((INode)node1));
        
        Group group1 = new Group(new GroupAddress(UUID.randomUUID(), "test"), true, Arrays.<INode>asList(node1, node2),
            Enums.noneOf(GroupOption.class));
        Group group2 = new Group(new GroupAddress(UUID.randomUUID(), "test"), true, Arrays.<INode>asList(node1, node2),
            Enums.noneOf(GroupOption.class));
        
        GroupsMembership groupsMembership = new GroupsMembership(Arrays.<IGroup>asList(group1, group2));
        assertThat(groupsMembership.getGroups(), is(Arrays.<IGroup>asList(group1, group2)));
        assertThat(groupsMembership.findGroup(group1.getId()), is((IGroup)group1));
        assertThat(groupsMembership.findGroup(group1.getAddress()), is((IGroup)group1));
        assertThat(groupsMembership.findNodeGroups(node1.getId()), is(Arrays.<IGroup>asList(group1, group2)));
        
        Node core1 = new Node(new UnicastAddress(UUID.randomUUID(), "core1"), Collections.<String, Object>singletonMap("key", "value"), 
            GroupMemberships.CORE_DOMAIN);
        Node core2 = new Node(new UnicastAddress(UUID.randomUUID(), "core2"), Collections.<String, Object>singletonMap("key", "value"), 
            GroupMemberships.CORE_DOMAIN);
        WorkerToCoreMembership workerToCoreMembership = new WorkerToCoreMembership(Arrays.<INode>asList(core1, core2), 
            new MapBuilder<INode, INode>().put(node1, core1).put(node2, core2).toMap());
        assertThat(workerToCoreMembership.getCoreNodes(), is(Arrays.<INode>asList(core1, core2)));
        assertThat(workerToCoreMembership.findCoreNode(core1.getId()), is((INode)core1));
        assertThat(workerToCoreMembership.findCoreNode(node1), is((INode)core1));
        assertThat(workerToCoreMembership.findWorkerNodes(core1), is((Set)com.exametrika.common.utils.Collections.asSet(node1)));
        
        DomainMembership domainMembership1 = new DomainMembership("domain1", Arrays.<IClusterMembershipElement>asList(
            nodesMembership, groupsMembership, workerToCoreMembership));
        DomainMembership domainMembership2 = new DomainMembership("domain2", Arrays.<IClusterMembershipElement>asList(
            nodesMembership, groupsMembership, workerToCoreMembership));
        DomainMembership coreDomainMembership = new DomainMembership("core", Arrays.<IClusterMembershipElement>asList(
            workerToCoreMembership));
        return new ClusterMembership(1, Arrays.asList(domainMembership1, domainMembership2), coreDomainMembership);
    }
}
