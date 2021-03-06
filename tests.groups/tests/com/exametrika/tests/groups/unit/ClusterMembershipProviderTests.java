/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.unit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupChange;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.cluster.membership.ClusterMembership;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.DefaultWorkerToCoreMappingStrategy;
import com.exametrika.impl.groups.cluster.membership.DomainMembership;
import com.exametrika.impl.groups.cluster.membership.DomainMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.Group;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.GroupChange;
import com.exametrika.impl.groups.cluster.membership.GroupDelta;
import com.exametrika.impl.groups.cluster.membership.GroupMembership;
import com.exametrika.impl.groups.cluster.membership.GroupMembershipChange;
import com.exametrika.impl.groups.cluster.membership.GroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.GroupsMembership;
import com.exametrika.impl.groups.cluster.membership.GroupsMembershipChange;
import com.exametrika.impl.groups.cluster.membership.GroupsMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.GroupsMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.IGroupDelta;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;
import com.exametrika.impl.groups.cluster.membership.Node;
import com.exametrika.impl.groups.cluster.membership.NodesMembership;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipChange;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembership;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembershipChange;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembershipProvider;
import com.exametrika.tests.groups.mocks.ClusterFailureDetectorMock;
import com.exametrika.tests.groups.mocks.LiveNodeProviderMock;
import com.exametrika.tests.groups.mocks.PropertyProviderMock;
import com.exametrika.tests.groups.mocks.TestGroupMappingStrategy;
import com.exametrika.tests.groups.mocks.WorkerNodeDiscovererMock;

/**
 * The {@link ClusterMembershipProviderTests} are tests for cluster membership provider.
 * 
 * @author Medvedev-A
 */
public class ClusterMembershipProviderTests
{
    @Test
    public void testNodesMembershipProvider()
    {
        ClusterFailureDetectorMock failureDetector = new ClusterFailureDetectorMock();
        WorkerNodeDiscovererMock nodeDiscoverer = new WorkerNodeDiscovererMock();
        NodesMembershipProvider provider = new NodesMembershipProvider(failureDetector);
        provider.setNodeDiscoverer(nodeDiscoverer);
        
        assertTrue(provider.getDomains().isEmpty());
        assertThat(provider.getDelta(1, null, null, null, null), is(new Pair(null, null)));
        provider.clearState();
        
        INode node1 = createNode("node1", "domain1");
        INode node2 = createNode("node2", "domain1");
        INode node3 = createNode("node3", "domain1");
        INode node4 = createNode("node4", "domain1");
        INode node = createNode("node", "domain1");
        
        INode node5 = createNode("node5", "domain2");
        INode node6 = createNode("node6", "domain2");
        
        DomainMembership domainMembership1 = new DomainMembership("domain1", Arrays.<IClusterMembershipElement>asList());
        nodeDiscoverer.discoveredNodes.add(node1);
        nodeDiscoverer.discoveredNodes.add(node2);
        nodeDiscoverer.discoveredNodes.add(node3);
        nodeDiscoverer.discoveredNodes.add(node5);
        nodeDiscoverer.discoveredNodes.add(node6);
        
        assertThat(provider.getDomains(), is(com.exametrika.common.utils.Collections.asSet("domain1", "domain2")));
        Pair pair = provider.getDelta(1, domainMembership1, null, null, null);
        provider.clearState();
        
        NodesMembership membership = (NodesMembership)pair.getKey();
        NodesMembership oldMembership = membership;
        assertThat(membership.getNodes(), is(Arrays.asList(node1, node2, node3)));
        
        NodesMembershipDelta delta = (NodesMembershipDelta)pair.getValue();
        assertThat(delta.getJoinedNodes(), is(Arrays.asList(node1, node2, node3)));
        assertTrue(delta.getLeftNodes().isEmpty());
        assertTrue(delta.getFailedNodes().isEmpty());
        
        nodeDiscoverer.discoveredNodes.add(node1);
        nodeDiscoverer.discoveredNodes.add(node4);
        
        failureDetector.failedNodes.add(node2);
        failureDetector.leftNodes.add(node3);
        failureDetector.failedNodes.add(node);
        failureDetector.failedNodes.add(node4);
        failureDetector.leftNodes.add(node5);
        
        assertThat(provider.getDomains(), is(com.exametrika.common.utils.Collections.asSet("domain1", "domain2")));
        pair = provider.getDelta(1, domainMembership1, null, null, membership);
        provider.clearState();
        
        membership = (NodesMembership)pair.getKey();
        assertThat(membership.getNodes(), is(Arrays.asList(node1, node4)));
        
        delta = (NodesMembershipDelta)pair.getValue();
        NodesMembershipDelta newDelta = delta;
        assertThat(delta.getJoinedNodes(), is(Arrays.asList(node4)));
        assertThat(delta.getFailedNodes(), is(com.exametrika.common.utils.Collections.asSet(node2.getId())));
        assertThat(delta.getLeftNodes(), is(com.exametrika.common.utils.Collections.asSet(node3.getId())));
        
        assertTrue(nodeDiscoverer.discoveredNodes.isEmpty());
        assertTrue(failureDetector.leftNodes.isEmpty());
        assertTrue(failureDetector.failedNodes.isEmpty());
        
        assertTrue(provider.getDomains().isEmpty());
        pair = provider.getDelta(1, domainMembership1, null, null, membership);
        provider.clearState();
        assertTrue(pair.getKey() == membership);
        assertTrue(pair.getValue() == null);
        
        provider = new NodesMembershipProvider();
        assertTrue(provider.isEmptyMembership(new NodesMembership(Collections.<INode>emptyList())));
        NodesMembershipDelta emptyDelta = (NodesMembershipDelta)provider.createEmptyDelta();
        assertTrue(emptyDelta.getJoinedNodes().isEmpty());
        assertTrue(emptyDelta.getLeftNodes().isEmpty());
        assertTrue(emptyDelta.getFailedNodes().isEmpty());
        
        delta = (NodesMembershipDelta)provider.createCoreFullDelta(membership);
        assertThat(delta.getJoinedNodes(), is(membership.getNodes()));
        assertTrue(delta.getLeftNodes().isEmpty());
        assertTrue(delta.getFailedNodes().isEmpty());
        
        NodesMembershipDelta workerDelta = (NodesMembershipDelta)provider.createWorkerDelta(membership, delta, true, true);
        assertTrue(workerDelta.getJoinedNodes().isEmpty());
        assertTrue(workerDelta.getLeftNodes().isEmpty());
        assertTrue(workerDelta.getFailedNodes().isEmpty());
        
        workerDelta = (NodesMembershipDelta)provider.createWorkerDelta(membership, delta, false, false);
        assertTrue(workerDelta == delta);
        
        workerDelta = (NodesMembershipDelta)provider.createWorkerDelta(membership, delta, true, false);
        assertThat(workerDelta.getJoinedNodes(), is(membership.getNodes()));
        assertTrue(workerDelta.getLeftNodes().isEmpty());
        assertTrue(workerDelta.getFailedNodes().isEmpty());
        
        NodesMembership newMembership = (NodesMembership)provider.createMembership(null, delta, null);
        assertThat(newMembership.getNodes(), is(Arrays.asList(node1, node4)));
        
        newMembership = (NodesMembership)provider.createMembership(null, newDelta, oldMembership);
        assertThat(newMembership.getNodes(), is(Arrays.asList(node1, node4)));
        
        NodesMembershipChange change = (NodesMembershipChange)provider.createChange(null, newDelta, oldMembership);
        assertThat(change.getJoinedNodes(), is(Arrays.asList(node4)));
        assertThat(change.getFailedNodes(), is(com.exametrika.common.utils.Collections.asSet(node2)));
        assertThat(change.getLeftNodes(), is(com.exametrika.common.utils.Collections.asSet(node3)));
    
        change = (NodesMembershipChange)provider.createChange(null, newMembership, oldMembership);
        assertThat(change.getJoinedNodes(), is(Arrays.asList(node4)));
        assertThat(change.getFailedNodes(), is(com.exametrika.common.utils.Collections.asSet(node2, node3)));
        assertThat(change.getLeftNodes(), is(Collections.<INode>emptySet()));
    }
    
    @Test
    public void testGroupsMembershipProvider()
    {
        Node node1 = new Node(new UnicastAddress(UUID.randomUUID(), "node1"), Collections.<String, Object>emptyMap(), "domain1");
        Node node2 = new Node(new UnicastAddress(UUID.randomUUID(), "node2"), Collections.<String, Object>emptyMap(), "domain1");
        Node node3 = new Node(new UnicastAddress(UUID.randomUUID(), "node3"), Collections.<String, Object>emptyMap(), "domain1");
        Group group1 = new Group(new GroupAddress(UUID.randomUUID(), "group1"), true, Arrays.<INode>asList(node1, node2), Enums.of(GroupOption.DURABLE), 1);
        Group group2 = new Group(new GroupAddress(UUID.randomUUID(), "group2"), true, Arrays.<INode>asList(node1, node2), Enums.of(GroupOption.DURABLE), 1);
        Group group3 = new Group(new GroupAddress(UUID.randomUUID(), "group3"), true, Arrays.<INode>asList(node1), Enums.of(GroupOption.DURABLE), 1);
        Group group4 = new Group(new GroupAddress(UUID.randomUUID(), "group4"), true, Arrays.<INode>asList(node1), Enums.of(GroupOption.DURABLE), 1);
        
        GroupDelta delta1 = new GroupDelta(group1.getId(), true, Arrays.<INode>asList(node1, node2), Collections.<UUID>emptySet(), Collections.<UUID>emptySet(), 1);
        GroupDelta delta2 = new GroupDelta(group2.getId(), true, Arrays.<INode>asList(node1, node2), Collections.<UUID>emptySet(), Collections.<UUID>emptySet(), 1);
        GroupDelta delta3 = new GroupDelta(group3.getId(), true, Arrays.<INode>asList(node1), Collections.<UUID>emptySet(), Collections.<UUID>emptySet(), 1);
        GroupDelta delta4 = new GroupDelta(group4.getId(), true, Arrays.<INode>asList(node1), Collections.<UUID>emptySet(), Collections.<UUID>emptySet(), 1);
        
        Group group21 = new Group(new GroupAddress(group2.getId(), group2.getName()), true, Arrays.<INode>asList(node1, node3), Enums.of(GroupOption.DURABLE), 1);
        IGroupDelta delta21 = new GroupDelta(group2.getId(), true, Collections.<INode>singletonList(node3),
            Collections.<UUID>emptySet(), Collections.<UUID>singleton(node2.getId()), 1);
        
        TestGroupMappingStrategy mappingStrategy = new TestGroupMappingStrategy();
        GroupsMembershipProvider provider = new GroupsMembershipProvider(mappingStrategy);
        NodesMembership nodesMembership = new NodesMembership(Arrays.<INode>asList(node1, node2));
        DomainMembership domainMembership = new DomainMembership("domain", Arrays.asList(nodesMembership));
        NodesMembershipDelta nodesMembershipDelta = new NodesMembershipDelta(Arrays.<INode>asList(node1, node2),
            Collections.<UUID>emptySet(), Collections.<UUID>emptySet());
        DomainMembershipDelta domainMembershipDelta = new DomainMembershipDelta("domain", Arrays.asList(nodesMembershipDelta));
        assertThat((Pair)provider.getDelta(1, domainMembership, domainMembershipDelta, null, null), is(new Pair(null, null)));
        mappingStrategy.result = (List)Arrays.asList(new Pair(group1, delta1), new Pair(group2, delta2), new Pair(group3, delta3));
        Pair pair = provider.getDelta(1, domainMembership, domainMembershipDelta, null, null);
        GroupsMembership membership = (GroupsMembership)pair.getKey();
        GroupsMembershipDelta delta = (GroupsMembershipDelta)pair.getValue();
        assertThat(membership.getGroups(), is(Arrays.<IGroup>asList(group1, group2, group3)));
        assertThat(delta.getNewGroups(), is(Arrays.<IGroup>asList(group1, group2, group3)));
        assertTrue(delta.getChangedGroups().isEmpty());
        assertTrue(delta.getRemovedGroups().isEmpty());
        
        mappingStrategy.result = (List)Arrays.asList(new Pair(group1, null), new Pair(group2, null), new Pair(group3, null));
        pair = provider.getDelta(1, domainMembership, domainMembershipDelta, null, membership);
        membership = (GroupsMembership)pair.getKey();
        delta = (GroupsMembershipDelta)pair.getValue();
        assertThat(membership.getGroups(), is(Arrays.<IGroup>asList(group1, group2, group3)));
        assertTrue(delta == null);
        
        mappingStrategy.result = (List)Arrays.asList(new Pair(group1, null), new Pair(group21, delta21), new Pair(group4, delta4));
        pair = provider.getDelta(1, domainMembership, domainMembershipDelta, null, membership);
        membership = (GroupsMembership)pair.getKey();
        delta = (GroupsMembershipDelta)pair.getValue();
        
        assertThat(membership.getGroups(), is(Arrays.<IGroup>asList(group1, group2, group4)));
        assertThat(membership.getGroups().get(1).getMembers(), is(group21.getMembers()));
        assertThat(delta.getNewGroups(), is(Arrays.<IGroup>asList(group4)));
        assertTrue(delta.getChangedGroups().size() == 1);
        
        IGroupDelta newGroupDelta = delta.getChangedGroups().iterator().next();
        assertThat(newGroupDelta.getJoinedMembers(), is(delta21.getJoinedMembers()));
        assertThat(newGroupDelta.getFailedMembers(), is(delta21.getFailedMembers()));
        assertThat(newGroupDelta.getLeftMembers(), is(delta21.getLeftMembers()));
        assertThat(delta.getRemovedGroups(), is(Collections.<UUID>singleton(group3.getId())));
        
        provider = new GroupsMembershipProvider();
        GroupsMembershipDelta emptyDelta = (GroupsMembershipDelta)provider.createEmptyDelta();
        assertTrue(emptyDelta.getNewGroups().isEmpty());
        assertTrue(emptyDelta.getChangedGroups().isEmpty());
        assertTrue(emptyDelta.getRemovedGroups().isEmpty());
        
        assertTrue(provider.isEmptyMembership(new GroupsMembership(Collections.<IGroup>emptyList())));
        
        membership = new GroupsMembership(Arrays.<IGroup>asList(group1, group2, group3));
        delta = (GroupsMembershipDelta)provider.createCoreFullDelta(membership);
        assertThat(delta.getNewGroups(), is(Arrays.<IGroup>asList(group1, group2, group3)));
        assertTrue(delta.getChangedGroups().isEmpty());
        assertTrue(delta.getRemovedGroups().isEmpty());
        
        GroupsMembershipDelta workerDelta = (GroupsMembershipDelta)provider.createWorkerDelta(membership, delta, true, true);
        assertTrue(workerDelta.getNewGroups().isEmpty());
        assertTrue(workerDelta.getChangedGroups().isEmpty());
        assertTrue(workerDelta.getRemovedGroups().isEmpty());
        
        workerDelta = (GroupsMembershipDelta)provider.createWorkerDelta(membership, delta, false, false);
        assertTrue(workerDelta == delta);
        
        workerDelta = (GroupsMembershipDelta)provider.createWorkerDelta(membership, delta, true, false);
        assertThat(workerDelta.getNewGroups(), is(Arrays.<IGroup>asList(group1, group2, group3)));
        assertTrue(workerDelta.getChangedGroups().isEmpty());
        assertTrue(workerDelta.getRemovedGroups().isEmpty());
        
        GroupsMembership newMembership = (GroupsMembership)provider.createMembership(null, delta, null);
        assertThat(newMembership.getGroups(), is(delta.getNewGroups()));
        
        IGroupDelta changedGroup = new GroupDelta(group2.getId(), true, Collections.<INode>singletonList(node3),
            Collections.<UUID>emptySet(), Collections.<UUID>singleton(node2.getId()), 1);
        GroupsMembershipDelta newDelta = new GroupsMembershipDelta(Arrays.<IGroup>asList(group4), 
            com.exametrika.common.utils.Collections.asSet(changedGroup), 
            com.exametrika.common.utils.Collections.asSet(group3.getId()));
        newMembership = (GroupsMembership)provider.createMembership(null, newDelta, newMembership);
        assertThat(newMembership.getGroups(), is(Arrays.<IGroup>asList(group1, group2, group4)));
        assertThat(newMembership.getGroups().get(1).getMembers(), is(Arrays.<INode>asList(node1, node3)));
  
        domainMembership = new DomainMembership("domain1",  Arrays.asList(newMembership));
        
        GroupsMembershipChange change = (GroupsMembershipChange)provider.createChange(domainMembership, newDelta, membership);
        assertThat(change.getNewGroups(), is(Arrays.<IGroup>asList(group4)));
        assertThat(change.getRemovedGroups(), is(Collections.<IGroup>singleton(group3)));
        assertThat(change.getChangedGroups().size(), is(1));
        IGroupChange groupChange = change.getChangedGroups().iterator().next();
        assertThat(groupChange.getJoinedMembers(), is(Arrays.<INode>asList(node3)));
        assertThat(groupChange.getFailedMembers(), is(Collections.<INode>singleton(node2)));
        
        change = (GroupsMembershipChange)provider.createChange(null, newMembership, membership);
        assertThat(change.getNewGroups(), is(Arrays.<IGroup>asList(group4)));
        assertThat(change.getRemovedGroups(), is(Collections.<IGroup>singleton(group3)));
        assertThat(change.getChangedGroups().size(), is(1));
        groupChange = change.getChangedGroups().iterator().next();
        assertThat(groupChange.getJoinedMembers(), is(Arrays.<INode>asList(node3)));
        assertThat(groupChange.getFailedMembers(), is(Collections.<INode>singleton(node2)));
    }
    
    @Test
    public void testWorkerToCoreMembershipProvider()
    {
        LiveNodeProviderMock liveNodeProvider = new LiveNodeProviderMock();
        PropertyProviderMock propertyProvider = new PropertyProviderMock();
        LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeProvider, propertyProvider, 
            GroupMemberships.CORE_DOMAIN);
        GroupMembershipManager membershipManager = new GroupMembershipManager("test", localNodeProvider, 
            Collections.<IPreparedGroupMembershipListener>emptySet(), 
            Collections.<IGroupMembershipListener>emptySet());
        
        WorkerToCoreMembershipProvider provider = new WorkerToCoreMembershipProvider(membershipManager, new DefaultWorkerToCoreMappingStrategy());
        WorkerToCoreMembershipProvider provider2 = new WorkerToCoreMembershipProvider();
        List<INode> coreNodes = new ArrayList<INode>();
        coreNodes.add(localNodeProvider.getLocalNode());
        for (int i = 0; i < 5; i++)
            coreNodes.add(new Node(new UnicastAddress(UUID.randomUUID(), "core" + i), Collections.<String, Object>emptyMap(), "core"));
        List<INode> workerNodes = new ArrayList<INode>();
        for (int i = 0; i < 10; i++)
            workerNodes.add(new Node(new UnicastAddress(UUID.randomUUID(), "worker" + i), Collections.<String, Object>emptyMap(), "domain1"));
        
        GroupMembership coreMembership = new GroupMembership(1, new Group(GroupMemberships.CORE_GROUP_ADDRESS, true, coreNodes, 
            Enums.of(GroupOption.DURABLE), 1));
        membershipManager.prepareInstallMembership(coreMembership);
        membershipManager.commitMembership();
        
        NodesMembership nodesMembership = new NodesMembership(workerNodes);
        DomainMembership domainMembership = new DomainMembership("domain", Arrays.asList(nodesMembership));
        NodesMembershipDelta nodesMembershipDelta = new NodesMembershipDelta(workerNodes,
            Collections.<UUID>emptySet(), Collections.<UUID>emptySet());
        DomainMembershipDelta domainMembershipDelta = new DomainMembershipDelta("domain", Arrays.asList(nodesMembershipDelta));
        ClusterMembership clusterMembership = new ClusterMembership(1, Arrays.asList(domainMembership), null);
        ClusterMembershipDelta clusterMembershipDelta = new ClusterMembershipDelta(1, true, Arrays.asList(domainMembershipDelta), null);
        Pair pair = provider.getDelta(1, clusterMembership, clusterMembershipDelta, null, null);
        WorkerToCoreMembership membership = (WorkerToCoreMembership)pair.getKey();
        WorkerToCoreMembershipDelta delta = (WorkerToCoreMembershipDelta)pair.getValue();
        assertThat(membership.getCoreNodes(), is(coreNodes));
        assertThat(membership.getCoreByWorkerMap().size(), is(workerNodes.size()));
        for (INode node : coreNodes)
            assertTrue(membership.findWorkerNodes(node).size() >= workerNodes.size() / coreNodes.size());
        assertThat(delta.getJoinedCoreNodes(), is(coreNodes));
        assertTrue(delta.getFailedCoreNodes().isEmpty());
        assertTrue(delta.getLeftCoreNodes().isEmpty());
        assertTrue(delta.getNewCoreByWorkerMap().size() == workerNodes.size());
        WorkerToCoreMembership oldMembership = membership;
        WorkerToCoreMembership newMembership = (WorkerToCoreMembership)provider2.createMembership(clusterMembership, delta, null);
        Map<UUID, UUID> map = new HashMap<UUID, UUID>();
        for (Map.Entry<INode, INode> entry : newMembership.getCoreByWorkerMap().entrySet())
            map.put(entry.getKey().getId(), entry.getValue().getId());
        assertThat(map, is(delta.getNewCoreByWorkerMap()));
        
        nodesMembershipDelta = new NodesMembershipDelta(Collections.<INode>emptyList(), Collections.<UUID>emptySet(), 
            Collections.<UUID>emptySet());
        domainMembershipDelta = new DomainMembershipDelta("domain1", Arrays.asList(nodesMembershipDelta));
        clusterMembershipDelta = new ClusterMembershipDelta(1, true, Arrays.asList(domainMembershipDelta), null);
        assertTrue(provider.getDelta(1, clusterMembership, clusterMembershipDelta, null, membership).getValue() == null);
        coreNodes = new ArrayList<INode>(coreNodes);
        INode removedCore = coreNodes.remove(1);
        INode removedWorker = workerNodes.remove(0);
        coreNodes.add(new Node(new UnicastAddress(UUID.randomUUID(), "core6"), Collections.<String, Object>emptyMap(), "core"));
        workerNodes.add(new Node(new UnicastAddress(UUID.randomUUID(), "worker11"), Collections.<String, Object>emptyMap(), "domain1"));
   
        GroupMembership newCoreMembership = new GroupMembership(2, new Group(GroupMemberships.CORE_GROUP_ADDRESS, true, coreNodes, 
            Enums.of(GroupOption.DURABLE), 1));
        GroupMembershipChange membershipChange = new GroupMembershipChange(new GroupChange(
            newCoreMembership.getGroup(), coreMembership.getGroup(), Arrays.<INode>asList(coreNodes.get(coreNodes.size() - 1)), 
            Collections.<INode>emptySet(), Collections.<INode>singleton(removedCore)));
        membershipManager.prepareChangeMembership(newCoreMembership, membershipChange);
        membershipManager.commitMembership();
        
        NodesMembership newNodesMembership = new NodesMembership(workerNodes);
        DomainMembership newDomainMembership = new DomainMembership("domain", Arrays.asList(newNodesMembership));
        NodesMembershipDelta newNodesMembershipDelta = new NodesMembershipDelta(workerNodes.subList(workerNodes.size() - 1, workerNodes.size()),
            Collections.<UUID>emptySet(), Collections.<UUID>singleton(removedWorker.getId()));
        DomainMembershipDelta newDomainMembershipDelta = new DomainMembershipDelta("domain", Arrays.asList(newNodesMembershipDelta));
        ClusterMembership newClusterMembership = new ClusterMembership(1, Arrays.asList(newDomainMembership), null);
        ClusterMembershipDelta newClusterMembershipDelta = new ClusterMembershipDelta(1, true, Arrays.asList(newDomainMembershipDelta), null);
        pair = provider.getDelta(2, newClusterMembership, newClusterMembershipDelta, null, membership);
        membership = (WorkerToCoreMembership)pair.getKey();
        delta = (WorkerToCoreMembershipDelta)pair.getValue();
        assertThat(membership.getCoreNodes(), is(coreNodes));
        assertThat(membership.getCoreByWorkerMap().size(), is(workerNodes.size()));
        for (INode node : coreNodes)
            assertTrue(membership.findWorkerNodes(node).size() >= workerNodes.size() / coreNodes.size());
        assertThat(delta.getJoinedCoreNodes(), is(coreNodes.subList(coreNodes.size() - 1, coreNodes.size())));
        assertThat(delta.getFailedCoreNodes(), is(Collections.singleton(removedCore.getId())));
        assertTrue(delta.getLeftCoreNodes().isEmpty());
        assertTrue(!delta.getNewCoreByWorkerMap().isEmpty());
        
        newDomainMembership = new DomainMembership("domain1", Arrays.asList(newNodesMembership));
        IDomainMembership newCoreDomainMembership = new DomainMembership("CORE", Arrays.asList(membership));
        newClusterMembership = new ClusterMembership(1, Arrays.asList(newDomainMembership), newCoreDomainMembership);
        newMembership = (WorkerToCoreMembership)provider2.createMembership(newClusterMembership, delta, newMembership);
        WorkerToCoreMembershipChange mappingChange = (WorkerToCoreMembershipChange)provider2.createChange(newClusterMembership, delta, oldMembership);
        assertThat(delta.getJoinedCoreNodes(), is(coreNodes.subList(coreNodes.size() - 1, coreNodes.size())));
        assertThat(delta.getFailedCoreNodes(), is(Collections.singleton(removedCore.getId())));
        assertTrue(delta.getLeftCoreNodes().isEmpty());
        assertThat(mappingChange.getNewCoreByWorkerMap().size(), is(delta.getNewCoreByWorkerMap().size()));
        
        mappingChange = (WorkerToCoreMembershipChange)provider2.createChange(newClusterMembership, newMembership, oldMembership);
        assertThat(newMembership.getCoreNodes(), is(coreNodes));
        assertThat(newMembership.getCoreByWorkerMap(), is(membership.getCoreByWorkerMap()));
        assertThat(mappingChange.getJoinedCoreNodes(), is(coreNodes.subList(coreNodes.size() - 1, coreNodes.size())));
        assertThat(mappingChange.getFailedCoreNodes(), is(Collections.singleton(removedCore)));
        assertTrue(mappingChange.getLeftCoreNodes().isEmpty());
        assertTrue(mappingChange.getNewCoreByWorkerMap().size() == delta.getNewCoreByWorkerMap().size());
        
        delta = (WorkerToCoreMembershipDelta)provider2.createEmptyDelta();
        assertTrue(delta.getJoinedCoreNodes().isEmpty());
        assertTrue(delta.getFailedCoreNodes().isEmpty());
        assertTrue(delta.getLeftCoreNodes().isEmpty());
        assertTrue(delta.getNewCoreByWorkerMap().isEmpty());
        
        delta = (WorkerToCoreMembershipDelta)provider2.createCoreFullDelta(membership);
        assertThat(delta.getJoinedCoreNodes(), is(membership.getCoreNodes()));
        assertTrue(delta.getFailedCoreNodes().isEmpty());
        assertTrue(delta.getLeftCoreNodes().isEmpty());
        map = new HashMap<UUID, UUID>();
        for (Map.Entry<INode, INode> entry : membership.getCoreByWorkerMap().entrySet())
            map.put(entry.getKey().getId(), entry.getValue().getId());
        assertThat(delta.getNewCoreByWorkerMap(), is(map));
    }
    
    private INode createNode(String name, String domain)
    {
        return new Node(new UnicastAddress(UUID.randomUUID(), name), Collections.<String, Object>emptyMap(), domain);
    }
}
