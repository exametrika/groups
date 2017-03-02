/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipChange;
import com.exametrika.api.groups.cluster.IClusterMembershipListener.LeaveReason;
import com.exametrika.api.groups.cluster.IClusterMembershipService;


/**
 * The {@link IClusterMembershipManager} manages cluster membership information.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IClusterMembershipManager extends IClusterMembershipService
{
    /**
     * Installs first membership.
     *
     * @param membership first membership
     */
    void installMembership(IClusterMembership membership);
    
    /**
     * Installs subsequent new membership.
     *
     * @param membership new membership
     * @param membershipChange membership change
     */
    void changeMembership(IClusterMembership membership, IClusterMembershipChange membershipChange);
    
    /**
     * Uninstalls membership of local node as part of group leaving process.
     *
     * @param reason leave reason
     */
    void uninstallMembership(LeaveReason reason);
}