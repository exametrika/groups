/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;

/**
 * The {@link IPreparedGroupMembershipListener} is used to notify about prepared phase of group membership change.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IPreparedGroupMembershipListener
{
    /**
     * Called when new membership is prepared to be installed. After receiving this call flush participant must send
     * messages in new membership.
     *
     * @param oldMembership old membership. Can be <c>null</c>, if old membership is not set yet
     * @param newMembership new membership
     * @param change membership change. Can be <c>null</c>, if old membership is not set yet
     */
    void onPreparedMembershipChanged(IGroupMembership oldMembership, IGroupMembership newMembership, IGroupMembershipChange change);
}
