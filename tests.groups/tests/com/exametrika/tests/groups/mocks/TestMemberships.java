/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.cluster.membership.Group;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.GroupChange;
import com.exametrika.impl.groups.cluster.membership.GroupMembership;
import com.exametrika.impl.groups.cluster.membership.GroupMembershipChange;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.Node;

public class TestMemberships
{
    public static INode createCoreNode(String name)
    {
        return createNode(name, GroupMemberships.CORE_DOMAIN);
    }
    
    public static INode createNode(String name, String domain)
    {
        IAddress address = new UnicastAddress(UUID.randomUUID(), name);
        return new Node(address, Collections.<String, Object>singletonMap("key", "value"), domain);
    }
    
    public static IGroup createGroup(String name, String domain, int nodeCount)
    {
        List<INode> nodes = new ArrayList<INode>();
        for (int i = 0; i < nodeCount; i++)
            nodes.add(createNode("test" + i, domain));
        return new Group(new GroupAddress(UUID.randomUUID(), name), true, nodes, Enums.noneOf(GroupOption.class), 1);
    }
    
    public static IGroup createCoreGroup(int nodeCount)
    {
        List<INode> nodes = new ArrayList<INode>();
        for (int i = 0; i < nodeCount; i++)
            nodes.add(createCoreNode("test" + i));
        return new Group(GroupMemberships.CORE_GROUP_ADDRESS, true, nodes, Enums.noneOf(GroupOption.class), 1);
    }
    
    public static IGroupMembership createGroupMembership(String name, String domain, int nodeCount)
    {
        return new GroupMembership(1, createGroup(name, domain, nodeCount));
    }
    
    public static IGroupMembership createCoreGroupMembership(int nodeCount)
    {
        return new GroupMembership(1, createCoreGroup(nodeCount));
    }
    
    public static Pair<IGroupMembership, IGroupMembershipChange> changeGroupMembership(IGroupMembership membership)
    {
        List<INode> members = new ArrayList<INode>(membership.getGroup().getMembers());
        INode removedNode = members.remove(members.size() - 1);
        INode newNode = createNode("test" + (members.size() + membership.getId() - 1), members.get(0).getDomain());
        members.add(newNode);
        
        IGroup newGroup = new Group((GroupAddress)membership.getGroup().getAddress(), true, members, 
            membership.getGroup().getOptions(), 1);
        GroupMembershipChange change = new GroupMembershipChange(new GroupChange(newGroup, membership.getGroup(), 
            Arrays.asList(newNode), Collections.<INode>emptySet(), Collections.<INode>singleton(removedNode)));
        GroupMembership newMembership = new GroupMembership(membership.getId() + 1, newGroup);
        
        return new Pair(newMembership, change);
    }
    
    public static Set<IAddress> buildNodeAddresses(List<INode> nodes)
    {
        Set<IAddress> set = new HashSet<IAddress>();
        for (INode node : nodes)
            set.add(node.getAddress());
        
        return set;
    }
}
