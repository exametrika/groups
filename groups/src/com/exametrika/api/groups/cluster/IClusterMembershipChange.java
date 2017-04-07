/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.List;
import java.util.Set;


/**
 * The {@link IClusterMembershipChange} represents a difference between old and new cluster membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IClusterMembershipChange
{
    /**
     * Returns new domains.
     *
     * @return new domains
     */
    List<IDomainMembership> getNewDomains();
    
    /**
     * Finds changed domain by name.
     *
     * @param name domain name
     * @return domain or null if domain change is not found
     */
    IDomainMembershipChange findChangedDomain(String name);
    
    /**
     * Returns list of all changed cluster domains.
     *
     * @return list of all cluster domains
     */
    List<IDomainMembershipChange> getChangedDomains();
    
    /**
     * Returns removed domains.
     *
     * @return removed domains
     */
    Set<IDomainMembership> getRemovedDomains();
}