/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;


/**
 * The {@link IClusterMembershipProvider} is a provider of cluster membership element.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IClusterMembershipProvider
{
    /**
     * Does provider have membership changes to install?
     *
     * @return true if provider has membership changes
     */
    boolean hasChanges();
    
    /**
     * Creates membership delta by given membership and provider state.
     *
     * @param membership membership or null if membership is not set, i.e full delta is calculated against empty membership 
     *        (used for sending to new nodes)
     * @return membership delta
     */
    IClusterMembershipElementDelta createDelta(IClusterMembershipElement membership);

    /**
     * Creates new membership by delta and old membership.
     *
     * @param delta membership delta
     * @param oldMembership old membership, can be null
     * @return new membership
     */
    IClusterMembershipElement createMembership(IClusterMembershipElementDelta delta, IClusterMembershipElement oldMembership);
    
    /**
     * Creates membership change by delta and old membership.
     *
     * @param delta delta
     * @param oldMembership old membership
     * @return membership change
     */
    IClusterMembershipElementChange createChange(IClusterMembershipElementDelta delta, IClusterMembershipElement oldMembership);
    
    /**
     * Creates membership change between new and old memberships.
     *
     * @param newMembership new membership
     * @param oldMembership old membership
     * @return membership change
     */
    IClusterMembershipElementChange createChange(IClusterMembershipElement newMembership, IClusterMembershipElement oldMembership);
}