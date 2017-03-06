/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.Map;

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
     * Returns membership deltas with domain name as key by given membership and provider state.
     *
     * @param membership membership with domain name as key or null if membership is not set, i.e full delta is calculated against empty membership 
     *        (used for sending to new nodes)
     * @return membership deltas with domain name as key
     */
    Map<String, IClusterMembershipElementDelta> getDeltas(Map<String, IClusterMembershipElement> membership);

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
     * Creates full membership delta based on given membership.
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
     * @param delta membership delta
     * @param oldMembership old membership, can be null (if delta is full)
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