/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.failuredetection;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.core.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeTrackingStrategy;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.core.membership.IMembershipManager;

/**
 * The {@link GroupNodeTrackingStrategy} is a node tracking strategy where group coordinator tracks group members and
 * group members track coordinator.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class GroupNodeTrackingStrategy implements INodeTrackingStrategy
{
    private final IMembershipManager membershipManager;
    private final IFailureDetector failureDetector;

    public GroupNodeTrackingStrategy(IMembershipManager membershipManager, IFailureDetector failureDetector)
    {
        Assert.notNull(membershipManager);
        Assert.notNull(failureDetector);
        
        this.membershipManager = membershipManager;
        this.failureDetector = failureDetector;
    }
    
    @Override
    public Set<IAddress> getTrackedNodes(IAddress localNode, List<IAddress> liveNodes)
    {
        Assert.notNull(localNode);
        Assert.notNull(liveNodes);
        
        if (membershipManager.getPreparedMembership() == null)
        {
            Set<IAddress> trackedNodes = new HashSet(liveNodes);
            trackedNodes.remove(localNode);
            return trackedNodes;
        }
        
        if (localNode.equals(failureDetector.getCurrentCoordinator().getAddress()))
        {
            Set<IAddress> trackedMembers = new HashSet<IAddress>();
            for (INode member : membershipManager.getPreparedMembership().getGroup().getMembers())
            {
                if (!member.getAddress().equals(localNode) && !failureDetector.getFailedMembers().contains(member) &&
                    !failureDetector.getLeftMembers().contains(member))
                    trackedMembers.add(member.getAddress());
            }
            
            return trackedMembers;
        }
        else
            return Collections.singleton(failureDetector.getCurrentCoordinator().getAddress());
    }
}
