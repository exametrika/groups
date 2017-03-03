/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.List;

/**
 * The {@link IClusterMembership} is a cluster membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IClusterMembership
{
    /**
     * Returns membership identifier.
     * 
     * @return membership identifier
     */
    long getId();

    /**
     * Finds domain by name.
     *
     * @param name dimain name
     * @return domain
     */
    IDomainMembership findDomain(String name);
    
    /**
     * Returns list of all cluster domains.
     *
     * @return list of all cluster domains
     */
    List<IDomainMembership> getDomains();

    @Override
    boolean equals(Object o);
    
    @Override
    int hashCode();
}