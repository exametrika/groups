/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

/**
 * The {@link IMembershipDelta} represents a difference between old and new membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IMembershipDelta
{
    /**
     * Returns identifier of new membership.
     *
     * @return identifier of new membership
     */
    long getId();
    
    /**
     * Returns group delta.
     *
     * @return group delta
     */
    IGroupDelta getGroup();
}