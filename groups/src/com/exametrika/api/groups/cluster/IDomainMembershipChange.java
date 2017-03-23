/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.List;


/**
 * The {@link IDomainMembershipChange} represents a difference between old and new cluster domain membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IDomainMembershipChange
{
    /**
     * Returns domain name.
     *
     * @return domain name
     */
    String getName();
    
    /**
     * Finds membership element change.
     *
     * @param changeClass change class
     * @return membership element change or null if change is not found
     */
    <T extends IClusterMembershipElementChange> T findChange(Class<T> changeClass);
    
    /**
     * Returns list of all membership elements changes.
     *
     * @return list of all membership elements changes
     */
    List<IClusterMembershipElementChange> getChanges();
}