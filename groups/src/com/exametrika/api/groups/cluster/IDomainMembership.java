/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.List;

/**
 * The {@link IDomainMembership} is a cluster domain membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IDomainMembership
{
    /**
     * Returns domain name.
     *
     * @return domain name
     */
    String getName();
    
    /**
     * Finds membership element.
     *
     * @param elementClass element class
     * @return membership element or null if element is not found
     */
    <T extends IClusterMembershipElement> T findElement(Class<T> elementClass);
    
    /**
     * Returns list of all membership elements.
     *
     * @return list of all membership elements
     */
    List<IClusterMembershipElement> getElements();
}