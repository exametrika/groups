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
     * Returns membership element.
     *
     * @param elementClass element class
     * @return membership element
     */
    <T extends IClusterMembershipElement> T getElement(Class<T> elementClass);
    
    /**
     * Returns list of all membership elements.
     *
     * @return list of all membership elements
     */
    List<IClusterMembershipElement> getElements();

    @Override
    boolean equals(Object o);
    
    @Override
    int hashCode();
}