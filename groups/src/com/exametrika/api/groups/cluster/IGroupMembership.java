/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;


/**
 * The {@link IGroupMembership} is a group membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IGroupMembership
{
    /**
     * Returns membership identifier.
     * 
     * @return membership identifier
     */
    long getId();

    /**
     * Returns group.
     *
     * @return group
     */
    IGroup getGroup();

    @Override
    boolean equals(Object o);
    
    @Override
    int hashCode();
}