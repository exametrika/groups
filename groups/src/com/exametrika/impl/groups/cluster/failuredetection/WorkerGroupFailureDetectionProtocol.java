/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.ClusterMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IDomainMembershipChange;
import com.exametrika.api.groups.cluster.IGroupChange;
import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.membership.GroupsMembershipChange;

/**
 * The {@link WorkerGroupFailureDetectionProtocol} represents a worker group failure detection protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class WorkerGroupFailureDetectionProtocol extends GroupFailureDetectionProtocol implements IClusterMembershipListener
{
    private final UUID groupId;

    public WorkerGroupFailureDetectionProtocol(String channelName, IMessageFactory messageFactory, IGroupMembershipService membershipService, 
        Set<IFailureDetectionListener> failureDetectionListeners, UUID groupId)
    {
        super(channelName, messageFactory, membershipService, failureDetectionListeners);
        
        Assert.notNull(groupId);
        
        this.groupId = groupId;
    }

    @Override
    public void onJoined()
    {
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
    }

    @Override
    public void onMembershipChanged(ClusterMembershipEvent event)
    {
        INode localNode = membershipService.getLocalNode();
        IDomainMembershipChange domain = event.getMembershipChange().findDomain(localNode.getDomain());
        if (domain == null)
            return;
        
        GroupsMembershipChange groupsChange = domain.findChange(GroupsMembershipChange.class);
        IGroupChange changedGroup = groupsChange.findChangedGroup(groupId);
        if (changedGroup == null)
            return;
        
        if (!changedGroup.getFailedMembers().isEmpty())
        {
            Set<UUID> memberIds = new HashSet<UUID>();
            for (INode node : changedGroup.getFailedMembers())
                memberIds.add(node.getId());
            
            addFailedMembers(memberIds);
        }
        
        if (!changedGroup.getLeftMembers().isEmpty())
        {
            Set<UUID> memberIds = new HashSet<UUID>();
            for (INode node : changedGroup.getLeftMembers())
                memberIds.add(node.getId());
            
            addLeftMembers(memberIds);
        }
    }
}
