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
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.IMembershipListener.LeaveReason;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.CoreGroupMembershipTracker;
import com.exametrika.impl.groups.cluster.membership.Group;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.GroupChange;
import com.exametrika.impl.groups.cluster.membership.GroupDelta;
import com.exametrika.impl.groups.cluster.membership.GroupMembership;
import com.exametrika.impl.groups.cluster.membership.GroupMembershipChange;
import com.exametrika.impl.groups.cluster.membership.GroupMembershipDelta;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships.MembershipChangeInfo;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships.MembershipDeltaInfo;
import com.exametrika.tests.groups.mocks.FailureDetectorMock;
import com.exametrika.tests.groups.mocks.FlushManagerMock;
import com.exametrika.tests.groups.mocks.GroupMembershipListenerMock;
import com.exametrika.tests.groups.mocks.LiveNodeProviderMock;
import com.exametrika.tests.groups.mocks.MembershipManagerMock;
import com.exametrika.tests.groups.mocks.NodeDiscovererMock;
import com.exametrika.tests.groups.mocks.PreparedGroupMembershipListenerMock;
import com.exametrika.tests.groups.mocks.PropertyProviderMock;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;
import com.exametrika.impl.groups.cluster.membership.Node;

/**
 * The {@link GroupMembershipManagerTests} are tests for {@link CoreGroupMembershipManager}.
 * 
 * @see CoreGroupMembershipManager
 * @author Medvedev-A
 */
public class GroupMembershipManagerTests
{
    @Test
    public void testNode()
    {
        IAddress local = new UnicastAddress(new UUID(1, 1), "test");
        Node node = new Node(local, Collections.<String, Object>singletonMap("key", "value"), "core");
        Node node2 = new Node(new UnicastAddress(new UUID(0, 0), "test"), 
            Collections.<String, Object>singletonMap("key", "value"), "core");
        assertThat(node, is(node));
        assertThat(!node.equals(node2), is(true));
        assertThat(node.compareTo(node2) > 0, is(true));
        assertThat((String)node.findProperty("key"), is("value"));
    }
    
    @Test
    public void testGroup()
    {
        IAddress address1 = new UnicastAddress(UUID.randomUUID(), "test1");
        IAddress address2 = new UnicastAddress(UUID.randomUUID(), "test2");
        Node node1 = new Node(address1, Collections.<String, Object>singletonMap("key", "value"), "core");
        Node node2 = new Node(address2, Collections.<String, Object>singletonMap("key", "value"), "core");
        
        Group group = new Group(new GroupAddress(new UUID(1, 1), "test"), true, Arrays.<INode>asList(node1, node2),
            Enums.noneOf(GroupOption.class), 1);
        Group group2 = new Group(new GroupAddress(new UUID(0, 0), "test"), true, Arrays.<INode>asList(node1, node2),
            Enums.noneOf(GroupOption.class), 1);
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
        IAddress address1 = new UnicastAddress(UUID.randomUUID(), "test1");
        IAddress address2 = new UnicastAddress(UUID.randomUUID(), "test2");
        Node node1 = new Node(address1, Collections.<String, Object>singletonMap("key", "value"), "core");
        Node node2 = new Node(address2, Collections.<String, Object>singletonMap("key", "value"), "core");
        
        Group group = new Group(new GroupAddress(UUID.randomUUID(), "test"), true, Arrays.<INode>asList(node1, node2),
            Enums.noneOf(GroupOption.class), 1);
        
        GroupMembership membership = new GroupMembership(1, group);
        GroupMembership membership2 = new GroupMembership(2, group);
        assertThat(membership, is(membership));
        assertThat(!membership.equals(membership2), is(true));
    }
    
    @Test
    public void testMemberships()
    {
        IAddress address1 = new UnicastAddress(UUID.randomUUID(), "test1");
        IAddress address2 = new UnicastAddress(UUID.randomUUID(), "test2");
        IAddress address3 = new UnicastAddress(UUID.randomUUID(), "test3");
        IAddress address4 = new UnicastAddress(UUID.randomUUID(), "test4");
        IAddress address5 = new UnicastAddress(UUID.randomUUID(), "test5");
        Node node1 = new Node(address1, Collections.<String, Object>singletonMap("key", "value"), "core");
        Node node2 = new Node(address2, Collections.<String, Object>singletonMap("key", "value"), "core");
        Node node3 = new Node(address3, Collections.<String, Object>singletonMap("key", "value"), "core");
        Node node4 = new Node(address4, Collections.<String, Object>singletonMap("key", "value"), "core");
        Node node5 = new Node(address5, Collections.<String, Object>singletonMap("key", "value"), "core");
        
        IGroupMembership membership = GroupMemberships.createCoreMembership(node1, com.exametrika.common.utils.Collections.<INode>asSet(node2, node3));
        assertThat(membership.getId(), is(1l));
        assertThat(membership.getGroup().getCoordinator(), is((INode)node1));
        assertThat(membership.getGroup().getMembers().size(), is(3));
        assertThat(membership.getGroup().getName(), is("core"));
        assertThat(membership.getGroup().isPrimary(), is(true));
        assertThat(membership.getGroup().findMember(node1.getId()) == node1, is(true));
        assertThat(membership.getGroup().findMember(node2.getId()) == node2, is(true));
        assertThat(membership.getGroup().findMember(node3.getId()) == node3, is(true));
        
        MembershipChangeInfo changeInfo = GroupMemberships.createMembership(membership, new GroupMembershipDelta(2,
            new GroupDelta(membership.getGroup().getId(), false, Arrays.<INode>asList(node4, node5), 
            com.exametrika.common.utils.Collections.<UUID>asSet(node1.getId()), com.exametrika.common.utils.Collections.<UUID>asSet(node2.getId()), 1)));
        assertTrue(changeInfo.oldMembership == membership);
        assertTrue(changeInfo.newMembership.getId() == 2);
        assertThat(changeInfo.newMembership.getGroup().getCoordinator(), is((INode)node3));
        assertThat(changeInfo.newMembership.getGroup().getMembers(), is(Arrays.<INode>asList(node3, node4, node5)));
        assertThat(changeInfo.newMembership.getGroup().getName(), is("core"));
        assertThat(changeInfo.newMembership.getGroup().isPrimary(), is(false));
        
        assertThat(changeInfo.membershipChange.getGroup().getJoinedMembers(), is(Arrays.<INode>asList(node4, node5)));
        assertThat(changeInfo.membershipChange.getGroup().getLeftMembers(), is(com.exametrika.common.utils.Collections.<INode>asSet(node1)));
        assertThat(changeInfo.membershipChange.getGroup().getFailedMembers(), is(com.exametrika.common.utils.Collections.<INode>asSet(node2)));
        
        MembershipDeltaInfo deltaInfo = GroupMemberships.createCoreMembership(membership, com.exametrika.common.utils.Collections.<INode>asSet(node1), 
            com.exametrika.common.utils.Collections.<INode>asSet(node2), com.exametrika.common.utils.Collections.<INode>asSet(node4, node5), null);
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
        assertThat(deltaInfo.membershipDelta.getGroup().getJoinedMembers(), is(joined));
        assertThat(deltaInfo.membershipDelta.getGroup().getFailedMembers(), is(com.exametrika.common.utils.Collections.<UUID>asSet(node1.getId())));
        assertThat(deltaInfo.membershipDelta.getGroup().getLeftMembers(), is(com.exametrika.common.utils.Collections.<UUID>asSet(node2.getId())));
        
        changeInfo = GroupMemberships.createMembership(membership, new GroupMembershipDelta(2,
            new GroupDelta(membership.getGroup().getId(), false, Arrays.<INode>asList(), 
            com.exametrika.common.utils.Collections.<UUID>asSet(node1.getId()), com.exametrika.common.utils.Collections.<UUID>asSet(node2.getId()), 1)));
        assertThat(changeInfo.newMembership.getGroup().isPrimary(), is(false));
        
        deltaInfo = GroupMemberships.createCoreMembership(membership, com.exametrika.common.utils.Collections.<INode>asSet(node1), 
            com.exametrika.common.utils.Collections.<INode>asSet(node2), com.exametrika.common.utils.Collections.<INode>asSet(), null);
        assertThat(deltaInfo.newMembership.getGroup().isPrimary(), is(false));
    }
    
    @Test
    public void testMembershipTracker()
    {
        MembershipManagerMock membershipManager = new MembershipManagerMock();
        NodeDiscovererMock nodeDiscoverer = new NodeDiscovererMock();
        FailureDetectorMock failureDetector = new FailureDetectorMock();
        FlushManagerMock flushManager = new FlushManagerMock();
        CoreGroupMembershipTracker tracker = new CoreGroupMembershipTracker(1000, membershipManager, nodeDiscoverer, failureDetector, flushManager, null);
        
        INode discoveredNode1 = new Node(new UnicastAddress(UUID.randomUUID(), 
            "test"), Collections.<String, Object>emptyMap(), "core");
        INode discoveredNode2 = new Node(new UnicastAddress(UUID.randomUUID(), 
            "test"), Collections.<String, Object>emptyMap(), "core");
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
        assertThat(flushManager.membershipDelta.getGroup().getJoinedMembers(), is(Arrays.<INode>asList(discoveredNode2)));
        assertThat(flushManager.membershipDelta.getGroup().getLeftMembers(), is(com.exametrika.common.utils.Collections.<UUID>asSet()));
        assertThat(flushManager.membershipDelta.getGroup().getFailedMembers(), is(com.exametrika.common.utils.Collections.<UUID>asSet(discoveredNode1.getId())));
        assertThat(flushManager.membership.getId(), is(2l));
        assertThat(flushManager.membership.getGroup().getMembers(), is(Arrays.asList(membershipManager.getLocalNode(), discoveredNode2)));
    }
    
    @Test
    public void testMembershipManager() throws Exception
    {
        LiveNodeProviderMock liveNodeProvider = new LiveNodeProviderMock();
        PropertyProviderMock propertyProvider = new PropertyProviderMock();
        PreparedGroupMembershipListenerMock preparedListener = new PreparedGroupMembershipListenerMock();
        GroupMembershipListenerMock listener = new GroupMembershipListenerMock();
        NodeDiscovererMock nodeDiscoverer = new NodeDiscovererMock();
        LocalNodeProvider localNodeProvider = new LocalNodeProvider(liveNodeProvider, propertyProvider, 
            GroupMemberships.CORE_DOMAIN);
        CoreGroupMembershipManager manager = new CoreGroupMembershipManager("test", localNodeProvider, 
            com.exametrika.common.utils.Collections.<IPreparedGroupMembershipListener>asSet(preparedListener), 
            com.exametrika.common.utils.Collections.<IGroupMembershipListener>asSet(listener));
        manager.setNodeDiscoverer(nodeDiscoverer);
        
        manager.start();
        assertThat(nodeDiscoverer.startDiscovery, is(true));
        assertThat(manager.getLocalNode().getAddress(), is(liveNodeProvider.getLocalNode()));
        assertThat(manager.getLocalNode().getId(), is(liveNodeProvider.getLocalNode().getId()));
        assertThat(manager.getLocalNode().getName(), is(liveNodeProvider.getLocalNode().getName()));
        assertThat(manager.getLocalNode().getProperties(), is(Collections.<String, Object>singletonMap("key", "value")));
        
        IAddress address1 = new UnicastAddress(UUID.randomUUID(), "test1");
        Node node1 = new Node(address1, Collections.<String, Object>singletonMap("key", "value"), "core");
        IAddress address2 = new UnicastAddress(UUID.randomUUID(), "test2");
        Node node2 = new Node(address2, Collections.<String, Object>singletonMap("key", "value"), "core");
        Group group = new Group(new GroupAddress(UUID.randomUUID(), "test"), true, Arrays.<INode>asList(manager.getLocalNode(), node1),
            Enums.noneOf(GroupOption.class), 1);
        GroupMembership membership = new GroupMembership(1, group);
        
        manager.prepareInstallMembership(membership);
        assertThat(manager.getPreparedMembership(), is((IGroupMembership)membership));
        assertThat(manager.getMembership() == null, is(true));
        assertThat(preparedListener.newMembership, is((IGroupMembership)membership));
        assertThat(preparedListener.oldMembership == null, is(true));
        assertThat(preparedListener.change == null, is(true));
        
        manager.commitMembership();
        assertThat(manager.getMembership(), is((IGroupMembership)membership));
        assertTrue(listener.onJoined);
        
        Group group2 = new Group(new GroupAddress(UUID.randomUUID(), "test"), true, Arrays.<INode>asList(manager.getLocalNode(), node1, node2),
            Enums.noneOf(GroupOption.class), 1);
        GroupMembership membership2 = new GroupMembership(2, group2);
        GroupMembershipChange membershipChange = new GroupMembershipChange(new GroupChange(group2, group, Collections.<INode>singletonList(node2), 
            Collections.<INode>emptySet(), Collections.<INode>emptySet()));
        manager.prepareChangeMembership(membership2, membershipChange);
        
        assertThat(manager.getPreparedMembership(), is((IGroupMembership)membership2));
        assertThat(manager.getMembership(), is((IGroupMembership)membership));
        assertThat(preparedListener.newMembership, is((IGroupMembership)membership2));
        assertThat(preparedListener.oldMembership, is((IGroupMembership)membership));
        assertThat(preparedListener.change.getGroup().getJoinedMembers(), is(membershipChange.getGroup().getJoinedMembers()));
        
        manager.commitMembership();
        assertThat(manager.getMembership(), is((IGroupMembership)membership2));
        assertThat(listener.onMembershipChangedEvent.getNewMembership(), is((IGroupMembership)membership2));
        assertThat(listener.onMembershipChangedEvent.getOldMembership(), is((IGroupMembership)membership));
        assertThat(listener.onMembershipChangedEvent.getMembershipChange().getGroup().getJoinedMembers(), is(membershipChange.getGroup().getJoinedMembers()));
        assertThat(listener.onMembershipChangedEvent.getMembershipChange().getGroup().getFailedMembers(), is(membershipChange.getGroup().getFailedMembers()));
        assertThat(listener.onMembershipChangedEvent.getMembershipChange().getGroup().getLeftMembers(), is(membershipChange.getGroup().getLeftMembers()));
        
        manager.uninstallMembership(IGroupMembershipListener.LeaveReason.GRACEFUL_EXIT);
        assertThat(manager.getPreparedMembership() == null, is(true));
        assertThat(manager.getMembership() == null, is(true));
        assertThat(listener.leaveReason, is(LeaveReason.GRACEFUL_EXIT));
        
        manager.stop();
    }
}
