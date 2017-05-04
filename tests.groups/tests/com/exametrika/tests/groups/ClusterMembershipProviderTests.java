/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupChange;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.cluster.discovery.IWorkerNodeDiscoverer;
import com.exametrika.impl.groups.cluster.failuredetection.IClusterFailureDetector;
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
import com.exametrika.impl.groups.cluster.membership.IGroupMappingStrategy;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;
import com.exametrika.impl.groups.cluster.membership.Node;
import com.exametrika.impl.groups.cluster.membership.NodesMembership;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipChange;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembership;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembershipProvider;
import com.exametrika.tests.groups.GroupMembershipManagerTests.LiveNodeProviderMock;
import com.exametrika.tests.groups.GroupMembershipManagerTests.PropertyProviderMock;

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
        Group group1 = new Group(new GroupAddress(UUID.randomUUID(), "group1"), true, Arrays.<INode>asList(node1, node2), Enums.of(GroupOption.DURABLE));
        Group group2 = new Group(new GroupAddress(UUID.randomUUID(), "group2"), true, Arrays.<INode>asList(node1, node2), Enums.of(GroupOption.DURABLE));
        Group group3 = new Group(new GroupAddress(UUID.randomUUID(), "group3"), true, Arrays.<INode>asList(node1), Enums.of(GroupOption.DURABLE));
        Group group4 = new Group(new GroupAddress(UUID.randomUUID(), "group4"), true, Arrays.<INode>asList(node1), Enums.of(GroupOption.DURABLE));
        
        GroupDelta delta1 = new GroupDelta(group1.getId(), true, Arrays.<INode>asList(node1, node2), Collections.<UUID>emptySet(), Collections.<UUID>emptySet());
        GroupDelta delta2 = new GroupDelta(group2.getId(), true, Arrays.<INode>asList(node1, node2), Collections.<UUID>emptySet(), Collections.<UUID>emptySet());
        GroupDelta delta3 = new GroupDelta(group3.getId(), true, Arrays.<INode>asList(node1), Collections.<UUID>emptySet(), Collections.<UUID>emptySet());
        GroupDelta delta4 = new GroupDelta(group4.getId(), true, Arrays.<INode>asList(node1), Collections.<UUID>emptySet(), Collections.<UUID>emptySet());
        
        Group group21 = new Group(new GroupAddress(UUID.randomUUID(), "group2"), true, Arrays.<INode>asList(node1, node3), Enums.of(GroupOption.DURABLE));
        IGroupDelta delta21 = new GroupDelta(group2.getId(), true, Collections.<INode>singletonList(node3),
            Collections.<UUID>emptySet(), Collections.<UUID>singleton(node2.getId()));
        
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
        pair = provider.getDelta(1, domainMembership, domainMembershipDelta, null, null);
        membership = (GroupsMembership)pair.getKey();
        delta = (GroupsMembershipDelta)pair.getValue();
        assertThat(membership.getGroups(), is(Arrays.<IGroup>asList(group1, group2, group3)));
        assertTrue(delta == null);
        
        mappingStrategy.result = (List)Arrays.asList(new Pair(group1, null), new Pair(group21, delta21), new Pair(group4, delta4));
        pair = provider.getDelta(1, domainMembership, domainMembershipDelta, null, null);
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
        
        membership = new GroupsMembership(Arrays.<IGroup>asList(group2, group2, group3));
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
            Collections.<UUID>emptySet(), Collections.<UUID>singleton(node2.getId()));
        GroupsMembershipDelta newDelta = new GroupsMembershipDelta(Arrays.<IGroup>asList(group4), 
            com.exametrika.common.utils.Collections.asSet(changedGroup), 
            com.exametrika.common.utils.Collections.asSet(group4.getId()));
        newMembership = (GroupsMembership)provider.createMembership(null, newDelta, newMembership);
        assertThat(newMembership.getGroups(), is(Arrays.<IGroup>asList(group1, group2, group4)));
        assertThat(newMembership.getGroups().get(0).getMembers(), is(Arrays.<INode>asList(node1, node3)));
  
        GroupsMembershipChange change = (GroupsMembershipChange)provider.createChange(null, newDelta, membership);
        assertThat(change.getNewGroups(), is(Arrays.<IGroup>asList(group4)));
        assertThat(change.getRemovedGroups(), is(Collections.<IGroup>singleton(group4)));
        assertThat(change.getChangedGroups().size(), is(1));
        IGroupChange groupChange = change.getChangedGroups().iterator().next();
        assertThat(groupChange.getJoinedMembers(), is(Arrays.<INode>asList(node3)));
        assertThat(groupChange.getFailedMembers(), is(Collections.<INode>singleton(node2)));
        
        change = (GroupsMembershipChange)provider.createChange(null, newMembership, membership);
        assertThat(change.getNewGroups(), is(Arrays.<IGroup>asList(group4)));
        assertThat(change.getRemovedGroups(), is(Collections.<IGroup>singleton(group4)));
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
        assertThat(provider.getDomains(), is(Collections.singleton(GroupMemberships.CORE_DOMAIN)));
        List<INode> coreNodes = new ArrayList<INode>();
        for (int i = 0; i < 5; i++)
            coreNodes.add(new Node(new UnicastAddress(UUID.randomUUID(), "core" + i), Collections.<String, Object>emptyMap(), "core"));
        List<INode> workerNodes = new ArrayList<INode>();
        for (int i = 0; i < 10; i++)
            workerNodes.add(new Node(new UnicastAddress(UUID.randomUUID(), "worker" + i), Collections.<String, Object>emptyMap(), "domain1"));
        
        GroupMembership coreMembership = new GroupMembership(1, new Group(GroupMemberships.CORE_GROUP_ADDRESS, true, coreNodes, 
            Enums.of(GroupOption.DURABLE)));
        membershipManager.prepareInstallMembership(coreMembership);
        membershipManager.commitMembership();
        
        NodesMembership nodesMembership = new NodesMembership(workerNodes);
        DomainMembership domainMembership = new DomainMembership("domain", Arrays.asList(nodesMembership));
        NodesMembershipDelta nodesMembershipDelta = new NodesMembershipDelta(workerNodes,
            Collections.<UUID>emptySet(), Collections.<UUID>emptySet());
        DomainMembershipDelta domainMembershipDelta = new DomainMembershipDelta("domain", Arrays.asList(nodesMembershipDelta));
        
        Pair pair = provider.getDelta(1, domainMembership, domainMembershipDelta, null, null);
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
        
        assertTrue(provider.getDelta(1, domainMembership, domainMembershipDelta, null, membership).getValue() == null);
        
        INode removedCore = coreNodes.remove(0);
        INode removedWorker = workerNodes.remove(0);
        coreNodes.add(new Node(new UnicastAddress(UUID.randomUUID(), "core6"), Collections.<String, Object>emptyMap(), "core"));
        workerNodes.add(new Node(new UnicastAddress(UUID.randomUUID(), "worker11"), Collections.<String, Object>emptyMap(), "domain1"));
   
        GroupMembership newCoreMembership = new GroupMembership(2, new Group(GroupMemberships.CORE_GROUP_ADDRESS, true, coreNodes, 
            Enums.of(GroupOption.DURABLE)));
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
        
        pair = provider.getDelta(2, newDomainMembership, newDomainMembershipDelta, null, membership);
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
        
        provider = new WorkerToCoreMembershipProvider();
        delta = (WorkerToCoreMembershipDelta)provider.createEmptyDelta();
        assertTrue(delta.getJoinedCoreNodes().isEmpty());
        assertTrue(delta.getFailedCoreNodes().isEmpty());
        assertTrue(delta.getLeftCoreNodes().isEmpty());
        assertTrue(delta.getNewCoreByWorkerMap().isEmpty());
        
        membership = new WorkerToCoreMembership(Collections.<INode>emptyList(), Collections.<INode, INode>emptyMap());
        assertTrue(provider.isEmptyMembership(membership));
    }
    
    private INode createNode(String name, String domain)
    {
        return new Node(new UnicastAddress(UUID.randomUUID(), name), Collections.<String, Object>emptyMap(), domain);
    }
    
    private static class ClusterFailureDetectorMock implements IClusterFailureDetector
    {
        private Set<INode> failedNodes = new HashSet<INode>();
        private Set<INode> leftNodes = new HashSet<INode>();
        
        @Override
        public Set<INode> takeFailedNodes()
        {
            Set<INode> result = failedNodes;
            failedNodes = new LinkedHashSet<INode>();
            return result;
        }

        @Override
        public Set<INode> takeLeftNodes()
        {
            Set<INode> result = leftNodes;
            leftNodes = new LinkedHashSet<INode>();
            return result;
        }
    }
    
    private static class WorkerNodeDiscovererMock implements IWorkerNodeDiscoverer
    {
        private Set<INode> discoveredNodes = new LinkedHashSet<INode>();
        
        @Override
        public Set<INode> takeDiscoveredNodes()
        {
            Set<INode> result = discoveredNodes;
            discoveredNodes = new LinkedHashSet<INode>();
            return result;
        }
    }
    
    private static class TestGroupMappingStrategy implements IGroupMappingStrategy
    {
        private List<Pair<IGroup, IGroupDelta>> result;
        @Override
        public List<Pair<IGroup, IGroupDelta>> mapGroups(long membershipId, String domain,
            NodesMembership nodeMembership, NodesMembershipDelta nodesMembershipDelta,
            GroupsMembership oldGroupMembership)
        {
            return result;
        }
    }
}
