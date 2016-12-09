/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipChange;

/**
 * The {@link IPreparedMembershipListener} is used to notify about prepared phase of membership change.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IPreparedMembershipListener
{
    /**
     * Called when new membership is prepared to be installed. After receiving this call flush participant must send
     * messages in new membership.
     *
     * @param oldMembership old membership. Can be <c>null</c>, if old membership is not set yet
     * @param newMembership new membership
     * @param change membership change. Can be <c>null</c>, if old membership is not set yet
     */
    void onPreparedMembershipChanged(IMembership oldMembership, IMembership newMembership, IMembershipChange change);
}
