/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;
import com.exametrika.common.utils.Pair;


/**
 * The {@link ICoreClusterMembershipProvider} is a provider of core cluster membership element.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface ICoreClusterMembershipProvider
{
    /**
     * Returns membership delta by given old membership and internal provider state.
     *
     * @param membershipId new membership identifier
     * @param newClusterMembership new cluster membership (partially built)
     * @param clusterMembershipDelta cluster membership delta
     * @param oldClusterMembership old cluster membership or null if old cluster membership is not set
     * @param oldMembership old membership or null if membership is not set, i.e full delta is calculated against empty membership 
     *        (used for sending to new nodes)
     * @return pair of new membership and membership delta, new membership can be null if delta is null and old membership is null,
     *         delta can be null if nothing changed in provider
     */
    Pair<IClusterMembershipElement, IClusterMembershipElementDelta> getDelta(long membershipId, IClusterMembership newClusterMembership,
        IClusterMembershipDelta clusterMembershipDelta, IClusterMembership oldClusterMembership, IClusterMembershipElement oldMembership);
    
    /**
     * Creates empty membership delta.
     *
     * @return empty membership delta
     */
    IClusterMembershipElementDelta createEmptyDelta();
    
    /**
     * Creates core full membership delta based on given membership.
     *
     * @param membership membership
     * @return core full membership delta
     */
    IClusterMembershipElementDelta createCoreFullDelta(IClusterMembershipElement membership);
    
    /**
     * Creates new membership by delta and old membership.
     *
     * @param newClusterMembership new cluster membership (partially built)
     * @param delta membership delta
     * @param oldMembership old membership, can be null (if delta is full)
     * @return new membership
     */
    IClusterMembershipElement createMembership(IClusterMembership newClusterMembership, IClusterMembershipElementDelta delta,
        IClusterMembershipElement oldMembership);
    
    /**
     * Creates membership change by delta and old membership.
     *
     * @param newClusterMembership new cluster membership (partially built)
     * @param delta delta
     * @param oldMembership old membership
     * @return membership change
     */
    IClusterMembershipElementChange createChange(IClusterMembership newClusterMembership, IClusterMembershipElementDelta delta, 
        IClusterMembershipElement oldMembership);
    
    /**
     * Creates membership change between new and old memberships.
     *
     * @param newClusterMembership new cluster membership (partially built)
     * @param newMembership new membership
     * @param oldMembership old membership
     * @return membership change
     */
    IClusterMembershipElementChange createChange(IClusterMembership newClusterMembership, IClusterMembershipElement newMembership, 
        IClusterMembershipElement oldMembership);
}