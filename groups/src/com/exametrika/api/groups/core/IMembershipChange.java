/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.core;

/**
 * The {@link IMembershipChange} represents a difference between old and new membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IMembershipChange
{
    /**
     * Returns group change.
     *
     * @return group change
     */
    IGroupChange getGroup();
}