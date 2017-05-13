/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.Set;

import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.common.utils.Pair;


/**
 * The {@link IClusterMembershipProvider} is a provider of cluster membership element.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IClusterMembershipProvider
{
    /**
     * Returns list of domains, used in {@link #getDelta}.
     *
     * @return list of domains, used in {@link #getDelta}
     */
    Set<String> getDomains();
    
    /**
     * Returns membership delta by given old membership and internal provider state.
     *
     * @param membershipId new membership identifier
     * @param newDomainMembership new domain membership (partially built)
     * @param domainMembershipDelta domain membership delta
     * @param oldDomainMembership old domain membership or null if old domain membership is not set
     * @param oldMembership old membership or null if membership is not set, i.e full delta is calculated against empty membership 
     *        (used for sending to new nodes)
     * @return pair of new membership and membership delta, new membership can be null if delta is null and old membership is null,
     *         delta can be null if nothing changed in provider
     */
    Pair<IClusterMembershipElement, IClusterMembershipElementDelta> getDelta(long membershipId, IDomainMembership newDomainMembership,
        IDomainMembershipDelta domainMembershipDelta, IDomainMembership oldDomainMembership, IClusterMembershipElement oldMembership);

    /**
     * Clears internal state.
     */
    void clearState();
    
    /**
     * Creates empty membership delta.
     *
     * @return empty membership delta
     */
    IClusterMembershipElementDelta createEmptyDelta();
    
    /**
     * Is membership empty?
     *
     * @param membership membership
     * @return true if membership is empty
     */
    boolean isEmptyMembership(IClusterMembershipElement membership);
    
    /**
     * Creates core full membership delta based on given membership.
     *
     * @param membership membership
     * @return core full membership delta
     */
    IClusterMembershipElementDelta createCoreFullDelta(IClusterMembershipElement membership);
    
    /**
     * Creates worker membership delta based on given membership. Only one of membership or delta must be specified.
     *
     * @param membership membership
     * @param delta delta
     * @param full if true worker delta is a difference between empty and given membership, if false - based on given delta
     * @param publicPart if true only public part is included (if any) to be used in external domain
     * @return worker membership delta
     */
    IClusterMembershipElementDelta createWorkerDelta(IClusterMembershipElement membership, IClusterMembershipElementDelta delta,
        boolean full, boolean publicPart);
    
    /**
     * Creates new membership by delta and old membership.
     *
     * @param newDomainMembership new domain membership (partially built)
     * @param delta membership delta
     * @param oldMembership old membership, can be null (if delta is full)
     * @return new membership
     */
    IClusterMembershipElement createMembership(IDomainMembership newDomainMembership, IClusterMembershipElementDelta delta,
        IClusterMembershipElement oldMembership);
    
    /**
     * Creates membership change by delta and old membership.
     *
     * @param newDomainMembership new domain membership (partially built)
     * @param delta delta
     * @param oldMembership old membership
     * @return membership change
     */
    IClusterMembershipElementChange createChange(IDomainMembership newDomainMembership, IClusterMembershipElementDelta delta, 
        IClusterMembershipElement oldMembership);
    
    /**
     * Creates membership change between new and old memberships.
     *
     * @param newDomainMembership new domain membership (partially built)
     * @param newMembership new membership
     * @param oldMembership old membership
     * @return membership change
     */
    IClusterMembershipElementChange createChange(IDomainMembership newDomainMembership, IClusterMembershipElement newMembership, 
        IClusterMembershipElement oldMembership);
}