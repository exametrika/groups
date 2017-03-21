/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.flush;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipDelta;

/**
 * The {@link IFlushManager} is a manager of flush protocol.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IFlushManager
{
    /**
     * Returns true if flush is in progress or false if flush is not in progress.
     *
     * @return true if flush is in progress or false if flush is not in progress
     * @exception UnsupportedOperationException if local node is not current coordinator
     */
    boolean isFlushInProgress();

    /**
     * Initiates installing of new membership. Identifier of new membership must be greater by one of
     * old membership. Must be called on current coordinator.
     *
     * @param membership new membership
     * @param membershipDelta membership delta or <c>null</c> if new membership installed first time
     * @exception IllegalArgumentException identifier of new membership is not equal to identifier of
     *            old membership + 1
     * @exception IllegalStateException if flush is already in progress
     * @exception UnsupportedOperationException if called not in current coordinator
     */
    void install(IGroupMembership membership, IGroupMembershipDelta membershipDelta);
}
