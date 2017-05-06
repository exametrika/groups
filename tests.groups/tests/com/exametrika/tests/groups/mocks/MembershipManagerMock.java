/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.Collections;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.api.groups.cluster.IMembershipListener.LeaveReason;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.Node;

public class MembershipManagerMock implements IGroupMembershipManager
{
    public INode localNode = new Node(new UnicastAddress(UUID.randomUUID(), 
        "test"), Collections.<String, Object>emptyMap(), "core");
    public IGroupMembership membership;
    public IGroupMembership preparedMembership;
        
    @Override
    public INode getLocalNode()
    {
        return localNode;
    }

    @Override
    public IGroupMembership getMembership()
    {
        return membership;
    }

    @Override
    public IGroupMembership getPreparedMembership()
    {
        return preparedMembership;
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
}