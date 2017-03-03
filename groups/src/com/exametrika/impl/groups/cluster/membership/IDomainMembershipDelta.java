/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;


/**
 * The {@link IDomainMembershipDelta} represents a difference between old and new domain membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IDomainMembershipDelta
{
    /**
     * Returns domain name.
     *
     * @return domain name
     */
    String getName();
    
    /**
     * Returns list of all membership elements deltas.
     *
     * @return list of all membership elements deltas
     */
    List<IClusterMembershipElementDelta> getDeltas();
}