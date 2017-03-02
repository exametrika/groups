/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.core;


/**
 * The {@link IMembership} is a group membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IMembership
{
    /**
     * Returns membership identifier.
     * 
     * @return membership identifier
     */
    long getId();

    /**
     * Returns core group.
     *
     * @return core group
     */
    IGroup getGroup();

    @Override
    boolean equals(Object o);
    
    @Override
    int hashCode();
}