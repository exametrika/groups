/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.api.groups.cluster.IMembershipListener.LeaveReason;


/**
 * The {@link IGroupMembershipManager} manages group membership information.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IGroupMembershipManager extends IGroupMembershipService
{
    /**
     * Returns prepared membership.
     *
     * @return prepared membership
     */
    IGroupMembership getPreparedMembership();
    
    /**
     * Prepares installation of first membership.
     *
     * @param membership first membership
     */
    void prepareInstallMembership(IGroupMembership membership);
    
    /**
     * Prepares installation of subsequent new membership.
     *
     * @param membership new membership
     * @param membershipChange membership change
     */
    void prepareChangeMembership(IGroupMembership membership, IGroupMembershipChange membershipChange);
    
    /**
     * Commits membership of local node.
     */
    void commitMembership();
    
    /**
     * Uninstalls membership of local node as part of group leaving process.
     *
     * @param reason leave reason
     */
    void uninstallMembership(LeaveReason reason);
}