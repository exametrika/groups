/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.List;


/**
 * The {@link IClusterMembershipChange} represents a difference between old and new cluster membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IClusterMembershipChange
{
    /**
     * Finds domain by name.
     *
     * @param name domain name
     * @return domain or null if domain change is not found
     */
    IDomainMembershipChange findDomain(String name);
    
    /**
     * Returns list of all cluster domains.
     *
     * @return list of all cluster domains
     */
    List<IDomainMembershipChange> getDomains();
}