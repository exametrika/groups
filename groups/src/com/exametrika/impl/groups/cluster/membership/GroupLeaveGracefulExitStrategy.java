/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.channel.IGracefulExitStrategy;

/**
 * The {@link GroupLeaveGracefulExitStrategy} is a worker node graceful exit strategy that allows exit when local node leaves
 * all its groups.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupLeaveGracefulExitStrategy implements IGracefulExitStrategy
{
    private final IClusterMembershipService membershipService;
    private boolean allowed;

    public GroupLeaveGracefulExitStrategy(IClusterMembershipService membershipService)
    {
        Assert.notNull(membershipService);
        
        this.membershipService = membershipService;
    }
    
    @Override
    public boolean requestExit()
    {
        if (allowed)
            return true;
        
        IClusterMembership membership = membershipService.getMembership();
        if (membership != null)
        {
            IDomainMembership domainMembership = membership.findDomain(membershipService.getLocalNode().getDomain());
            if (domainMembership != null)
            {
                GroupsMembership groupsMembership = domainMembership.findElement(GroupsMembership.class);
                if (!groupsMembership.findNodeGroups(membershipService.getLocalNode().getId()).isEmpty())
                    return false;
            }
        }
        
        allowed = true;
        return true;
    }
}
