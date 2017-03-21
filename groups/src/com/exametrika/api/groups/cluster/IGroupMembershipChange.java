/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

/**
 * The {@link IGroupMembershipChange} represents a difference between old and new group membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IGroupMembershipChange
{
    /**
     * Returns group change.
     *
     * @return group change
     */
    IGroupChange getGroup();
}