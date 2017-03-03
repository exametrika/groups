/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;


/**
 * The {@link IClusterMembershipDelta} represents a difference between old and new cluster membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IClusterMembershipDelta
{
    /**
     * Returns identifier of new membership.
     *
     * @return identifier of new membership
     */
    long getId();
    
    /**
     * Is delta full, i.e computed against null membership.
     *
     * @return true if delta is full
     */
    boolean isFull();
    
    /**
     * Returns list of all domain deltas.
     *
     * @return list of all domain deltas
     */
    List<IDomainMembershipDelta> getDomains();
}