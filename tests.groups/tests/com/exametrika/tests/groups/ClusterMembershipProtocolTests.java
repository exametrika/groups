/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.api.groups.cluster.GroupMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.cluster.membership.AbstractClusterMembershipProtocol;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipManager;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipMessagePart;
import com.exametrika.impl.groups.cluster.membership.ClusterMembershipResponseMessagePart;
import com.exametrika.impl.groups.cluster.membership.CoreCoordinatorClusterMembershipProtocol;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.DefaultWorkerToCoreMappingStrategy;
import com.exametrika.impl.groups.cluster.membership.DomainMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.IClusterMembershipElementDelta;
import com.exametrika.impl.groups.cluster.membership.IClusterMembershipManager;
import com.exametrika.impl.groups.cluster.membership.IClusterMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.IDomainMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;
import com.exametrika.impl.groups.cluster.membership.Node;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipProvider;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembershipProvider;
import com.exametrika.tests.groups.channel.TestProtocolStack;
import com.exametrika.tests.groups.mocks.ClusterFailureDetectorMock;
import com.exametrika.tests.groups.mocks.ClusterMembershipListenerMock;
import com.exametrika.tests.groups.mocks.FailureDetectorMock;
import com.exametrika.tests.groups.mocks.FlushManagerMock;
import com.exametrika.tests.groups.mocks.GroupMembershipListenerMock;
import com.exametrika.tests.groups.mocks.LiveNodeProviderMock;
import com.exametrika.tests.groups.mocks.NodeDiscovererMock;
import com.exametrika.tests.groups.mocks.PreparedGroupMembershipListenerMock;
import com.exametrika.tests.groups.mocks.PropertyProviderMock;
import com.exametrika.tests.groups.mocks.TestMemberships;
import com.exametrika.tests.groups.mocks.WorkerNodeDiscovererMock;

/**
 * The {@link ClusterMembershipProtocolTests} are tests for cluster membership protocol.
 * 
 * @author Medvedev-A
 */
public class ClusterMembershipProtocolTests
{
    @Test
    public void testAbstractProtocol()
    {
        LiveNodeProviderMock liveNodeProvider = new LiveNodeProviderMock();
        PropertyProviderMock propertyProvider = new PropertyProviderMock();
        ClusterMembershipListenerMock listener = new ClusterMembershipListenerMock();
        LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeProvider, propertyProvider, 
            GroupMemberships.CORE_DOMAIN);
        ClusterMembershipManager manager = new ClusterMembershipManager("test", localNodeProvider, 
            com.exametrika.common.utils.Collections.<IClusterMembershipListener>asSet(listener));
        
        TestProtocolStack stack = TestProtocolStack.create("test");
        TestClusterMembershipProtocol protocol = new TestClusterMembershipProtocol("test", stack.getMessageFactory(), 
            manager, Arrays.<IClusterMembershipProvider>asList(new NodesMembershipProvider()));
        stack.setProtocol(protocol);
        
        Node node1 = new Node(new UnicastAddress(UUID.randomUUID(), "node1"), Collections.<String, Object>singletonMap("key", "value"), "domain1");
        Node node2 = new Node(new UnicastAddress(UUID.randomUUID(), "node2"), Collections.<String, Object>singletonMap("key", "value"), "dimain1");
        
        NodesMembershipDelta nodesMembershipDelta = new NodesMembershipDelta(Arrays.<INode>asList(node1, node2), Collections.<UUID>emptySet(),
            Collections.<UUID>emptySet());
        DomainMembershipDelta domainDelta1 = new DomainMembershipDelta("domain1", Arrays.<IClusterMembershipElementDelta>asList(
            nodesMembershipDelta));
        DomainMembershipDelta domainDelta2 = new DomainMembershipDelta("domain2", Arrays.<IClusterMembershipElementDelta>asList(
            nodesMembershipDelta));
        DomainMembershipDelta domainDelta3 = new DomainMembershipDelta("domain3", Arrays.<IClusterMembershipElementDelta>asList(
            nodesMembershipDelta));
        ClusterMembershipDelta delta = new ClusterMembershipDelta(1, true, Arrays.asList(domainDelta1, domainDelta2,
            domainDelta3));
        ClusterMembershipMessagePart part = new ClusterMembershipMessagePart(1, delta);
        protocol.installMembership(part);
        
        IClusterMembership membership = manager.getMembership();
        assertTrue(membership != null);
        assertTrue(membership.getId() == 1);
        checkDomains(membership.getDomains(), Arrays.asList("domain1", "domain2", "domain3"));
        assertTrue(protocol.installedRoundId == 1);
        assertTrue(listener.onJoined);
        
        domainDelta3 = new DomainMembershipDelta("domain3", Arrays.<IClusterMembershipElementDelta>asList(
            new NodesMembershipDelta(Arrays.<INode>asList(), Collections.<UUID>emptySet(),
                Collections.<UUID>singleton(node1.getId()))));
        DomainMembershipDelta domainDelta4 = new DomainMembershipDelta("domain4", Arrays.<IClusterMembershipElementDelta>asList(
            nodesMembershipDelta));
        delta = new ClusterMembershipDelta(2, false, Arrays.asList(domainDelta2,
            domainDelta3, domainDelta4));
        part = new ClusterMembershipMessagePart(2, delta);
        protocol.installMembership(part);
        
        membership = manager.getMembership();
        assertTrue(membership != null);
        assertTrue(membership.getId() == 2);
        checkDomains(membership.getDomains(), Arrays.asList("domain1", "domain2", "domain3", "domain4"));
        assertTrue(protocol.installedRoundId == 2);
        assertTrue(listener.onMembershipChangedEvent != null);
        assertTrue(listener.onMembershipChangedEvent.getMembershipChange().getNewDomains().size() == 1);
        assertTrue(listener.onMembershipChangedEvent.getMembershipChange().getChangedDomains().size() == 1);
        assertTrue(listener.onMembershipChangedEvent.getMembershipChange().getRemovedDomains().size() == 0);
        
        delta = new ClusterMembershipDelta(3, false, Arrays.<IDomainMembershipDelta>asList());
        part = new ClusterMembershipMessagePart(3, delta);
        protocol.installMembership(part);
        
        membership = manager.getMembership();
        assertTrue(membership != null);
        assertTrue(membership.getId() == 3);
        checkDomains(membership.getDomains(), Arrays.asList("domain1", "domain2", "domain4"));
        assertTrue(protocol.installedRoundId == 3);
        assertTrue(listener.onMembershipChangedEvent != null);
        assertTrue(listener.onMembershipChangedEvent.getMembershipChange().getNewDomains().size() == 0);
        assertTrue(listener.onMembershipChangedEvent.getMembershipChange().getChangedDomains().size() == 0);
        assertTrue(listener.onMembershipChangedEvent.getMembershipChange().getRemovedDomains().size() == 1);
        listener.onMembershipChangedEvent = null;
        
        protocol.installMembership(part);
        membership = manager.getMembership();
        assertTrue(membership != null);
        assertTrue(membership.getId() == 3);
        checkDomains(membership.getDomains(), Arrays.asList("domain1", "domain2", "domain4"));
        assertTrue(protocol.installedRoundId == 3);
        assertTrue(listener.onMembershipChangedEvent == null);
    }
    
    @Test
    public void testCoreCoordinatorClusterMembershipProtocol() throws Exception
    {
        LiveNodeProviderMock liveNodeProvider = new LiveNodeProviderMock();
        PropertyProviderMock propertyProvider = new PropertyProviderMock();
        ClusterMembershipListenerMock clusterListener = new ClusterMembershipListenerMock();
        LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeProvider, propertyProvider, 
            GroupMemberships.CORE_DOMAIN);
        ClusterMembershipManager clusterMembershipManager = new ClusterMembershipManager("test", localNodeProvider, 
            com.exametrika.common.utils.Collections.<IClusterMembershipListener>asSet(clusterListener));
        
        PreparedGroupMembershipListenerMock preparedListener = new PreparedGroupMembershipListenerMock();
        GroupMembershipListenerMock groupListener = new GroupMembershipListenerMock();
        NodeDiscovererMock nodeDiscoverer = new NodeDiscovererMock();
        CoreGroupMembershipManager groupMembershipManager = new CoreGroupMembershipManager("test", localNodeProvider, 
            com.exametrika.common.utils.Collections.<IPreparedGroupMembershipListener>asSet(preparedListener), 
            com.exametrika.common.utils.Collections.<IGroupMembershipListener>asSet(groupListener));
        groupMembershipManager.setNodeDiscoverer(nodeDiscoverer);
        
        TestProtocolStack stack = TestProtocolStack.create("test");
        ClusterFailureDetectorMock clusterFailureDetector = new ClusterFailureDetectorMock();
        WorkerNodeDiscovererMock workerNodeDiscoverer = new WorkerNodeDiscovererMock();
        NodesMembershipProvider nodesMembershipProvider = new NodesMembershipProvider(clusterFailureDetector);
        nodesMembershipProvider.setNodeDiscoverer(workerNodeDiscoverer);
        DefaultWorkerToCoreMappingStrategy mappingStarategy = new DefaultWorkerToCoreMappingStrategy();
        WorkerToCoreMembershipProvider workerToCoreMembershipProvider = new WorkerToCoreMembershipProvider(groupMembershipManager, mappingStarategy);
        
        CoreCoordinatorClusterMembershipProtocol protocol = new CoreCoordinatorClusterMembershipProtocol("test", stack.getMessageFactory(), 
            clusterMembershipManager, Arrays.<IClusterMembershipProvider>asList(nodesMembershipProvider, workerToCoreMembershipProvider), groupMembershipManager, 1000);
        FailureDetectorMock failureDetector = new FailureDetectorMock();
        protocol.setFailureDetector(failureDetector);
        FlushManagerMock flushManager = new FlushManagerMock();
        protocol.setFlushManager(flushManager);
        stack.setProtocol(protocol);
        stack.start();
        
        workerNodeDiscoverer.discoveredNodes.addAll(Arrays.asList(TestMemberships.createNode("test1", "domain1"),
            TestMemberships.createNode("test2", "domain1")));
        stack.onTimer(2000);
        assertTrue(stack.getSentMessages().isEmpty());
        
        IGroupMembership groupMembership = TestMemberships.createCoreGroupMembership(10);
        groupMembershipManager.prepareInstallMembership(groupMembership);
        groupMembershipManager.commitMembership();
        
        stack.onTimer(4000);
        assertTrue(stack.getSentMessages().isEmpty());
        
        Tests.set(localNodeProvider, "localNode", groupMembership.getGroup().getCoordinator());
        stack.onTimer(4000);
        assertTrue(stack.getSentMessages().size() == 1);
        assertThat(stack.getSentMessages().get(0).getDestination(), is((IAddress)GroupMemberships.CORE_GROUP_ADDRESS));
        ClusterMembershipMessagePart part = stack.getSentMessages().get(0).getPart();
        assertTrue(part.getRoundId() == 1);
        assertTrue(part.getDelta().getDomains().size() == 1);
        assertThat((Set<IAddress>)Tests.get(protocol, "respondingNodes"), is(TestMemberships.buildNodeAddresses(groupMembership.getGroup().getMembers())));
        assertThat((Boolean)Tests.get(protocol, "installing"), is(true));
        stack.reset();
        
        stack.onTimer(6000);
        assertTrue(stack.getSentMessages().isEmpty());
        
        Tests.set(protocol, "installing", false);
        
        Pair<IGroupMembership, IGroupMembershipChange> pair = TestMemberships.changeGroupMembership(groupMembership);
        groupMembershipManager.prepareChangeMembership(pair.getKey(), pair.getValue());
        protocol.onMembershipChanged(new GroupMembershipEvent(groupMembership, pair.getKey(), pair.getValue()));
    
        assertTrue((Set<IAddress>)Tests.get(protocol, "respondingNodes") == null);
        assertTrue((Boolean)Tests.get(protocol, "coreNodesFailed"));
        
        workerNodeDiscoverer.discoveredNodes.add(TestMemberships.createNode("test10", "domain1"));
        stack.onTimer(8000);
        assertTrue(stack.getSentMessages().size() == 1);
        assertThat(stack.getSentMessages().get(0).getDestination(), is((IAddress)GroupMemberships.CORE_GROUP_ADDRESS));
        part = stack.getSentMessages().get(0).getPart();
        assertTrue(part.getRoundId() == 1);
        assertTrue(part.getDelta().getDomains().size() == 1);
        IDomainMembershipDelta delta = part.getDelta().getDomains().get(0);
        NodesMembershipDelta nodesDelta = (NodesMembershipDelta)delta.getDeltas().get(0);
        assertTrue(nodesDelta.getJoinedNodes().isEmpty());
        assertTrue(nodesDelta.getFailedNodes().isEmpty());
        WorkerToCoreMembershipDelta workerToCoreDelta = (WorkerToCoreMembershipDelta)delta.getDeltas().get(1);
        assertTrue(workerToCoreDelta.getJoinedCoreNodes().size() == 1);
        assertTrue(workerToCoreDelta.getFailedCoreNodes().size() == 1);
        
        IGroupMembership newGroupMembership = pair.getKey();
        assertThat((Set<IAddress>)Tests.get(protocol, "respondingNodes"), is(TestMemberships.buildNodeAddresses(
            newGroupMembership.getGroup().getMembers())));
        stack.reset();
        
        stack.onTimer(10000);
        assertTrue(stack.getSentMessages().isEmpty());
        
        for (INode node : newGroupMembership.getGroup().getMembers())
        {
            IMessage message = stack.getMessageFactory().create(localNodeProvider.getLocalNode().getAddress(), 
                new ClusterMembershipResponseMessagePart(1)).retarget(node.getAddress(), null);
            stack.receive(message);
        }
        
        assertTrue(Tests.get(protocol, "respondingNodes") == null);
        
        stack.onTimer(10100);
        assertTrue(stack.getSentMessages().isEmpty());
        
        stack.onTimer(12000);
        flushManager.flushInProgress = true;
        assertTrue(stack.getSentMessages().isEmpty());
        
        stack.onTimer(12000);
        flushManager.flushInProgress = false;
        assertTrue(!stack.getSentMessages().isEmpty());
    }
    
    private void checkDomains(List<IDomainMembership> domains, List<String> domainNames)
    {
        assertTrue(domains.size() == domainNames.size());
        for (int i = 0; i < domains.size(); i++)
            assertThat(domains.get(i).getName(), is(domainNames.get(i)));
    }
    
    private class TestClusterMembershipProtocol extends AbstractClusterMembershipProtocol
    {
        private long installedRoundId;
        
        public TestClusterMembershipProtocol(String channelName, IMessageFactory messageFactory,
            IClusterMembershipManager clusterMembershipManager, List<IClusterMembershipProvider> membershipProviders)
        {
            super(channelName, messageFactory, clusterMembershipManager, membershipProviders);
        }
        
        @Override
        public void installMembership(ClusterMembershipMessagePart part)
        {
            super.installMembership(part);
        }
        
        @Override
        protected void onInstalled(long roundId, IClusterMembership newMembership, ClusterMembershipDelta coreDelta)
        {
            installedRoundId = roundId;
        }
    }
}
