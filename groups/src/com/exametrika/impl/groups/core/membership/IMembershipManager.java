/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipChange;
import com.exametrika.api.groups.core.IMembershipListener.LeaveReason;
import com.exametrika.api.groups.core.IMembershipService;


/**
 * The {@link IMembershipManager} manages membership information.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IMembershipManager extends IMembershipService
{
    /**
     * Returns prepared membership.
     *
     * @return prepared membership
     */
    IMembership getPreparedMembership();
    
    /**
     * Prepares installation of first membership.
     *
     * @param membership first membership
     */
    void prepareInstallMembership(IMembership membership);
    
    /**
     * Prepares installation of subsequent new membership.
     *
     * @param membership new membership
     * @param membershipChange membership change
     */
    void prepareChangeMembership(IMembership membership, IMembershipChange membershipChange);
    
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