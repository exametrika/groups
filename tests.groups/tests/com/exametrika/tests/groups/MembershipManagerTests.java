/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipChange;
import com.exametrika.api.groups.core.IMembershipListener;
import com.exametrika.api.groups.core.IMembershipListener.LeaveReason;
import com.exametrika.api.groups.core.INode;
import com.exametrika.api.groups.core.MembershipEvent;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.impl.transports.tcp.TcpAddress;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.impl.groups.core.discovery.INodeDiscoverer;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;
import com.exametrika.impl.groups.core.flush.IFlushManager;
import com.exametrika.impl.groups.core.membership.Group;
import com.exametrika.impl.groups.core.membership.IMembershipDelta;
import com.exametrika.impl.groups.core.membership.IMembershipManager;
import com.exametrika.impl.groups.core.membership.IPreparedMembershipListener;
import com.exametrika.impl.groups.core.membership.Membership;
import com.exametrika.impl.groups.core.membership.MembershipChange;
import com.exametrika.impl.groups.core.membership.MembershipDelta;
import com.exametrika.impl.groups.core.membership.MembershipManager;
import com.exametrika.impl.groups.core.membership.MembershipTracker;
import com.exametrika.impl.groups.core.membership.Memberships;
import com.exametrika.impl.groups.core.membership.Memberships.MembershipChangeInfo;
import com.exametrika.impl.groups.core.membership.Memberships.MembershipDeltaInfo;
import com.exametrika.impl.groups.core.membership.Node;
import com.exametrika.spi.groups.IPropertyProvider;

/**
 * The {@link MembershipManagerTests} are tests for {@link MembershipManager}.
 * 
 * @see MembershipManager
 * @author Medvedev-A
 */
public class MembershipManagerTests
{
    @Test
    public void testNode()
    {
        IAddress local = new TcpAddress(new UUID(1, 1), new InetSocketAddress("localhost", 9090), "test");
        Node node = new Node(local.getId(), local.getName(), local, Collections.<String, Object>singletonMap("key", "value"));
        Node node2 = new Node(new UUID(0, 0), local.getName(), local, Collections.<String, Object>singletonMap("key", "value"));
        assertThat(node, is(node));
        assertThat(!node.equals(node2), is(true));
        assertThat(node.compareTo(node2) > 0, is(true));
        assertThat((String)node.getProperty("key"), is("value"));
    }
    
    @Test
    public void testGroup()
    {
        IAddress address1 = new TcpAddress(UUID.randomUUID(), new InetSocketAddress("localhost", 9090), "test1");
        IAddress address2 = new TcpAddress(UUID.randomUUID(), new InetSocketAddress("localhost", 9090), "test2");
        Node node1 = new Node(address1.getId(), address1.getName(), address1, Collections.<String, Object>singletonMap("key", "value"));
        Node node2 = new Node(address2.getId(), address2.getName(), address2, Collections.<String, Object>singletonMap("key", "value"));
        
        Group group = new Group(new UUID(1, 1), "test", true, Arrays.<INode>asList(node1, node2));
        Group group2 = new Group(new UUID(0, 0), "test", true, Arrays.<INode>asList(node1, node2));
        assertThat(group.getCoordinator(), is((INode)node1));
        assertThat(group.getMembers(), is(Arrays.<INode>asList(node1, node2)));
        assertThat(group.findMember(address2), is((INode)node2));
        assertThat(group.findMember(node1.getId()), is((INode)node1));
        
        assertThat(group, is(group));
        assertThat(!group.equals(group2), is(true));
        assertThat(group.compareTo(group2) > 0, is(true));
    }
    
    @Test
    public void testMembership()
    {
        IAddress address1 = new TcpAddress(UUID.randomUUID(), new InetSocketAddress("localhost", 9090), "test1");
        IAddress address2 = new TcpAddress(UUID.randomUUID(), new InetSocketAddress("localhost", 9090), "test2");
        Node node1 = new Node(address1.getId(), address1.getName(), address1, Collections.<String, Object>singletonMap("key", "value"));
        Node node2 = new Node(address2.getId(), address2.getName(), address2, Collections.<String, Object>singletonMap("key", "value"));
        
        Group group = new Group(UUID.randomUUID(), "test", true, Arrays.<INode>asList(node1, node2));
        
        Membership membership = new Membership(1, group);
        Membership membership2 = new Membership(2, group);
        assertThat(membership, is(membership));
        assertThat(!membership.equals(membership2), is(true));
    }
    
    @Test
    public void testMemberships()
    {
        IAddress address1 = new TcpAddress(UUID.randomUUID(), new InetSocketAddress("localhost", 9090), "test1");
        IAddress address2 = new TcpAddress(UUID.randomUUID(), new InetSocketAddress("localhost", 9090), "test2");
        IAddress address3 = new TcpAddress(UUID.randomUUID(), new InetSocketAddress("localhost", 9090), "test3");
        IAddress address4 = new TcpAddress(UUID.randomUUID(), new InetSocketAddress("localhost", 9090), "test4");
        IAddress address5 = new TcpAddress(UUID.randomUUID(), new InetSocketAddress("localhost", 9090), "test5");
        Node node1 = new Node(address1.getId(), address1.getName(), address1, Collections.<String, Object>singletonMap("key", "value"));
        Node node2 = new Node(address2.getId(), address2.getName(), address2, Collections.<String, Object>singletonMap("key", "value"));
        Node node3 = new Node(address3.getId(), address3.getName(), address3, Collections.<String, Object>singletonMap("key", "value"));
        Node node4 = new Node(address4.getId(), address4.getName(), address4, Collections.<String, Object>singletonMap("key", "value"));
        Node node5 = new Node(address5.getId(), address5.getName(), address5, Collections.<String, Object>singletonMap("key", "value"));
        
        IMembership membership = Memberships.createMembership(node1, com.exametrika.common.utils.Collections.<INode>asSet(node2, node3));
        assertThat(membership.getId(), is(1l));
        assertThat(membership.getGroup().getCoordinator(), is((INode)node1));
        assertThat(membership.getGroup().getMembers().size(), is(3));
        assertThat(membership.getGroup().getName(), is("core"));
        assertThat(membership.getGroup().isPrimary(), is(true));
        assertThat(membership.getGroup().findMember(node1.getId()) == node1, is(true));
        assertThat(membership.getGroup().findMember(node2.getId()) == node2, is(true));
        assertThat(membership.getGroup().findMember(node3.getId()) == node3, is(true));
        
        MembershipChangeInfo changeInfo = Memberships.createMembership(membership, new MembershipDelta(2, Arrays.<INode>asList(node4, node5), 
            com.exametrika.common.utils.Collections.<UUID>asSet(node1.getId()), com.exametrika.common.utils.Collections.<UUID>asSet(node2.getId())));
        assertTrue(changeInfo.oldMembership == membership);
        assertTrue(changeInfo.newMembership.getId() == 2);
        assertThat(changeInfo.newMembership.getGroup().getCoordinator(), is((INode)node3));
        assertThat(changeInfo.newMembership.getGroup().getMembers(), is(Arrays.<INode>asList(node3, node4, node5)));
        assertThat(changeInfo.newMembership.getGroup().getName(), is("core"));
        assertThat(changeInfo.newMembership.getGroup().isPrimary(), is(false));
        
        assertThat(changeInfo.membershipChange.getJoinedMembers(), is(com.exametrika.common.utils.Collections.<INode>asSet(node4, node5)));
        assertThat(changeInfo.membershipChange.getLeftMembers(), is(com.exametrika.common.utils.Collections.<INode>asSet(node1)));
        assertThat(changeInfo.membershipChange.getFailedMembers(), is(com.exametrika.common.utils.Collections.<INode>asSet(node2)));
        
        MembershipDeltaInfo deltaInfo = Memberships.createMembership(membership, com.exametrika.common.utils.Collections.<INode>asSet(node1), 
            com.exametrika.common.utils.Collections.<INode>asSet(node2), com.exametrika.common.utils.Collections.<INode>asSet(node4, node5));
        assertTrue(deltaInfo.oldMembership == membership);
        assertTrue(deltaInfo.newMembership.getId() == 2);
        assertThat(deltaInfo.newMembership.getGroup().getCoordinator(), is((INode)node3));
        assertThat(deltaInfo.newMembership.getGroup().getMembers().size(), is(3));
        assertThat(deltaInfo.newMembership.getGroup().getName(), is("core"));
        assertThat(deltaInfo.newMembership.getGroup().isPrimary(), is(false));
        assertThat(deltaInfo.newMembership.getGroup().findMember(node3.getId()) == node3, is(true));
        assertThat(deltaInfo.newMembership.getGroup().findMember(node4.getId()) == node4, is(true));
        assertThat(deltaInfo.newMembership.getGroup().findMember(node5.getId()) == node5, is(true));
        
        List<INode> joined = new ArrayList<INode>(deltaInfo.newMembership.getGroup().getMembers());
        joined.remove(node3);
        assertThat(deltaInfo.membershipDelta.getJoinedMembers(), is(joined));
        assertThat(deltaInfo.membershipDelta.getFailedMembers(), is(com.exametrika.common.utils.Collections.<UUID>asSet(node1.getId())));
        assertThat(deltaInfo.membershipDelta.getLeftMembers(), is(com.exametrika.common.utils.Collections.<UUID>asSet(node2.getId())));
        
        changeInfo = Memberships.createMembership(membership, new MembershipDelta(2, Arrays.<INode>asList(), 
            com.exametrika.common.utils.Collections.<UUID>asSet(node1.getId()), com.exametrika.common.utils.Collections.<UUID>asSet(node2.getId())));
        assertThat(changeInfo.newMembership.getGroup().isPrimary(), is(false));
        
        deltaInfo = Memberships.createMembership(membership, com.exametrika.common.utils.Collections.<INode>asSet(node1), 
            com.exametrika.common.utils.Collections.<INode>asSet(node2), com.exametrika.common.utils.Collections.<INode>asSet());
        assertThat(deltaInfo.newMembership.getGroup().isPrimary(), is(false));
    }
    
    @Test
    public void testMembershipTracker()
    {
        MembershipManagerMock membershipManager = new MembershipManagerMock();
        NodeDiscovererMock nodeDiscoverer = new NodeDiscovererMock();
        FailureDetectorMock failureDetector = new FailureDetectorMock();
        FlushManagerMock flushManager = new FlushManagerMock();
        MembershipTracker tracker = new MembershipTracker(1000, membershipManager, nodeDiscoverer, failureDetector, flushManager);
        
        INode discoveredNode1 = new Node(UUID.randomUUID(), "test", new TcpAddress(UUID.randomUUID(), 
            new InetSocketAddress("localhost", 9090), "test"), Collections.<String, Object>emptyMap());
        INode discoveredNode2 = new Node(UUID.randomUUID(), "test", new TcpAddress(UUID.randomUUID(), 
            new InetSocketAddress("localhost", 9090), "test"), Collections.<String, Object>emptyMap());
        nodeDiscoverer.canFormGroup = true;
        nodeDiscoverer.discoveredNodes.add(discoveredNode1);
        tracker.onTimer(1);
        assertTrue(flushManager.membershipDelta == null);
        assertThat(flushManager.membership.getId(), is(1l));
        assertThat(flushManager.membership.getGroup().getMembers(), is(Arrays.asList(membershipManager.getLocalNode(), discoveredNode1)));
        
        membershipManager.preparedMembership = flushManager.membership;
        membershipManager.membership = flushManager.membership;
        flushManager.membership = null;
        tracker.onTimer(1);
        assertTrue(flushManager.membership == null);
        nodeDiscoverer.discoveredNodes.clear();
        
        flushManager.flushInProgress = true;
        tracker.onTimer(10000);
        assertTrue(flushManager.membership == null);
        
        flushManager.flushInProgress = false;
        tracker.onTimer(10000);
        assertTrue(flushManager.membership == null);
        
        failureDetector.failedNodes.add(discoveredNode1);
        nodeDiscoverer.discoveredNodes.add(discoveredNode2);
        tracker.onTimer(10000);
        assertTrue(flushManager.membership == null);
        
        tracker.onTimer(20000);
        assertTrue(flushManager.membershipDelta.getId() == 2);
        assertThat(flushManager.membershipDelta.getJoinedMembers(), is(Arrays.<INode>asList(discoveredNode2)));
        assertThat(flushManager.membershipDelta.getLeftMembers(), is(com.exametrika.common.utils.Collections.<UUID>asSet()));
        assertThat(flushManager.membershipDelta.getFailedMembers(), is(com.exametrika.common.utils.Collections.<UUID>asSet(discoveredNode1.getId())));
        assertThat(flushManager.membership.getId(), is(2l));
        assertThat(flushManager.membership.getGroup().getMembers(), is(Arrays.asList(membershipManager.getLocalNode(), discoveredNode2)));
    }
    
    @Test
    public void testMembershipManager() throws Exception
    {
        LiveNodeProviderMock liveNodeProvider = new LiveNodeProviderMock();
        PropertyProviderMock propertyProvider = new PropertyProviderMock();
        PreparedMembershipListenerMock preparedListener = new PreparedMembershipListenerMock();
        MembershipListenerMock listener = new MembershipListenerMock();
        NodeDiscovererMock nodeDiscoverer = new NodeDiscovererMock();
        MembershipManager manager = new MembershipManager("test", liveNodeProvider, propertyProvider, 
            com.exametrika.common.utils.Collections.<IPreparedMembershipListener>asSet(preparedListener), 
            com.exametrika.common.utils.Collections.<IMembershipListener>asSet(listener));
        manager.setNodeDiscoverer(nodeDiscoverer);
        
        manager.start();
        assertThat(nodeDiscoverer.startDiscovery, is(true));
        assertThat(manager.getLocalNode().getAddress(), is(liveNodeProvider.getLocalNode()));
        assertThat(manager.getLocalNode().getId(), is(liveNodeProvider.getLocalNode().getId()));
        assertThat(manager.getLocalNode().getName(), is(liveNodeProvider.getLocalNode().getName()));
        assertThat(manager.getLocalNode().getProperties(), is(Collections.<String, Object>singletonMap("key", "value")));
        
        IAddress address1 = new TcpAddress(UUID.randomUUID(), new InetSocketAddress("localhost", 9090), "test1");
        Node node1 = new Node(address1.getId(), address1.getName(), address1, Collections.<String, Object>singletonMap("key", "value"));
        IAddress address2 = new TcpAddress(UUID.randomUUID(), new InetSocketAddress("localhost", 9090), "test2");
        Node node2 = new Node(address2.getId(), address2.getName(), address2, Collections.<String, Object>singletonMap("key", "value"));
        Group group = new Group(UUID.randomUUID(), "test", true, Arrays.<INode>asList(manager.getLocalNode(), node1));
        Membership membership = new Membership(1, group);
        
        manager.prepareInstallMembership(membership);
        assertThat(manager.getPreparedMembership(), is((IMembership)membership));
        assertThat(manager.getMembership() == null, is(true));
        assertThat(preparedListener.newMembership, is((IMembership)membership));
        assertThat(preparedListener.oldMembership == null, is(true));
        assertThat(preparedListener.change == null, is(true));
        
        manager.commitMembership();
        assertThat(manager.getMembership(), is((IMembership)membership));
        assertTrue(listener.onJoined);
        
        Group group2 = new Group(UUID.randomUUID(), "test", true, Arrays.<INode>asList(manager.getLocalNode(), node1, node2));
        Membership membership2 = new Membership(2, group2);
        MembershipChange membershipChange = new MembershipChange(Collections.<INode>singleton(node2), 
            Collections.<INode>emptySet(), Collections.<INode>emptySet());
        manager.prepareChangeMembership(membership2, membershipChange);
        
        assertThat(manager.getPreparedMembership(), is((IMembership)membership2));
        assertThat(manager.getMembership(), is((IMembership)membership));
        assertThat(preparedListener.newMembership, is((IMembership)membership2));
        assertThat(preparedListener.oldMembership, is((IMembership)membership));
        assertThat(preparedListener.change.getJoinedMembers(), is(membershipChange.getJoinedMembers()));
        
        manager.commitMembership();
        assertThat(manager.getMembership(), is((IMembership)membership2));
        assertThat(listener.onMembershipChangedEvent.getNewMembership(), is((IMembership)membership2));
        assertThat(listener.onMembershipChangedEvent.getOldMembership(), is((IMembership)membership));
        assertThat(listener.onMembershipChangedEvent.getMembershipChange().getJoinedMembers(), is(membershipChange.getJoinedMembers()));
        assertThat(listener.onMembershipChangedEvent.getMembershipChange().getFailedMembers(), is(membershipChange.getFailedMembers()));
        assertThat(listener.onMembershipChangedEvent.getMembershipChange().getLeftMembers(), is(membershipChange.getLeftMembers()));
        
        manager.uninstallMembership(IMembershipListener.LeaveReason.GRACEFUL_CLOSE);
        assertThat(manager.getPreparedMembership() == null, is(true));
        assertThat(manager.getMembership() == null, is(true));
        assertThat(listener.leaveReason, is(LeaveReason.GRACEFUL_CLOSE));
        
        manager.stop();
        assertThat(Tests.get(manager, "localNode") == null, is(true));
    }
    
    private static class PreparedMembershipListenerMock implements IPreparedMembershipListener
    {
        private IMembership oldMembership;
        private IMembership newMembership;
        private IMembershipChange change;

        @Override
        public void onPreparedMembershipChanged(IMembership oldMembership, IMembership newMembership,
            IMembershipChange change)
        {
            this.oldMembership = oldMembership;
            this.newMembership = newMembership;
            this.change = change;
        }
    }
    
    private static class MembershipListenerMock implements IMembershipListener
    {
        private LeaveReason leaveReason;
        private MembershipEvent onMembershipChangedEvent;
        private boolean onJoined;

        @Override
        public void onJoined()
        {
            onJoined = true;
        }

        @Override
        public void onLeft(LeaveReason reason)
        {
            this.leaveReason = reason;
        }

        @Override
        public void onMembershipChanged(MembershipEvent event)
        {
            this.onMembershipChangedEvent = event;
        }
    }
    
    public static class PropertyProviderMock implements IPropertyProvider
    {
        private Map<String, Object> properties = new MapBuilder<String, Object>().put("key", "value").toMap();
        
        @Override
        public Map<String, Object> getProperties()
        {
            return properties;
        }
    }
    
    private static class LiveNodeProviderMock implements ILiveNodeProvider
    {
        private IAddress localNode = new TcpAddress(UUID.randomUUID(), new InetSocketAddress("localhost", 9090), "test");
        
        @Override
        public long getId()
        {
            return 0;
        }

        @Override
        public IAddress getLocalNode()
        {
            return localNode;
        }

        @Override
        public List<IAddress> getLiveNodes()
        {
            return null;
        }

        @Override
        public boolean isLive(IAddress node)
        {
            return false;
        }

        @Override
        public IAddress findById(UUID id)
        {
            return null;
        }

        @Override
        public IAddress findByName(String name)
        {
            return null;
        }

        @Override
        public IAddress findByConnection(String connection)
        {
            return null;
        }
    }
    
    private static class MembershipManagerMock implements IMembershipManager
    {
        private INode localNode = new Node(UUID.randomUUID(), "test", new TcpAddress(UUID.randomUUID(), 
            new InetSocketAddress("localhost", 9090), "test"), Collections.<String, Object>emptyMap());
        private IMembership membership;
        private IMembership preparedMembership;
            
        @Override
        public INode getLocalNode()
        {
            return localNode;
        }

        @Override
        public IMembership getMembership()
        {
            return membership;
        }

        @Override
        public IMembership getPreparedMembership()
        {
            return preparedMembership;
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
    }
    
    private static class NodeDiscovererMock implements INodeDiscoverer
    {
        private boolean canFormGroup;
        private Set<INode> discoveredNodes = new TreeSet<INode>();
        private boolean startDiscovery;
        
        @Override
        public void startDiscovery()
        {
            startDiscovery = true;
        }

        @Override
        public boolean canFormGroup()
        {
            return canFormGroup;
        }

        @Override
        public Set<INode> getDiscoveredNodes()
        {
            return discoveredNodes;
        }
    }
    
    private static class FailureDetectorMock implements IFailureDetector
    {
        private INode currentCoordinator;
        private Set<INode> failedNodes = new HashSet<INode>();
        private Set<INode> leftNodes = new HashSet<INode>();
        
        @Override
        public INode getCurrentCoordinator()
        {
            return currentCoordinator;
        }

        @Override
        public List<INode> getHealthyMembers()
        {
            return null;
        }

        @Override
        public Set<INode> getFailedMembers()
        {
            return failedNodes;
        }

        @Override
        public Set<INode> getLeftMembers()
        {
            return leftNodes;
        }

        @Override
        public void addFailedMembers(Set<UUID> memberIds)
        {
        }

        @Override
        public void addLeftMembers(Set<UUID> memberIds)
        {
        }
    }
    
    private static class FlushManagerMock implements IFlushManager
    {
        private IMembership membership;
        private IMembershipDelta membershipDelta;
        private boolean flushInProgress;

        @Override
        public boolean isFlushInProgress()
        {
            return flushInProgress;
        }

        @Override
        public void install(IMembership membership, IMembershipDelta membershipDelta)
        {
            this.membership = membership;
            this.membershipDelta = membershipDelta;
        }
    }
}
