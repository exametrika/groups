/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipChange;


/**
 * The {@link IFlush} represents an information about current flush.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IFlush
{
    /**
     * Is group being formed?
     *
     * @return true if group is being formed 
     */
    boolean isGroupForming();
    
    /**
     * Returns old membership, that was before flush protocol. 
     * 
     * @return old membership, or <c>null</c>, if old membership is not set
     */
    IMembership getOldMembership();

    /**
     * Returns new membership, that flush protocol is installing.
     * 
     * @return new membership
     */
    IMembership getNewMembership();

    /**
     * Returns membership change.
     *
     * @return membership change or <c>null</c> if old membership is <c>null</c>
     */
    IMembershipChange getMembershipChange();
    
    /**
     * Allows proceeding of flush protocol. Called by flush participant, when it has completed all
     * neccessary actions of current phase of flush protocol.
     * 
     * @param participant which grants flush
     */
    void grantFlush(IFlushParticipant participant);
}
