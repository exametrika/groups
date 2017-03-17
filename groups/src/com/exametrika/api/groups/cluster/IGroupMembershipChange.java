/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.Set;

import com.exametrika.api.groups.core.IGroup;
import com.exametrika.api.groups.core.IGroupChange;


/**
 * The {@link IGroupMembershipChange} represents a difference between old and new group membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IGroupMembershipChange extends IClusterMembershipElementChange
{
    /**
     * Returns set of new cluster groups
     * 
     * @return set of new cluster groups
     */
    Set<IGroup> getNewGroups();
    
    /**
     * Returns set of changed cluster groups.
     * 
     * @return set of changed custer groups
     */
    Set<IGroupChange> getChangedGroups();
    
    /**
     * Returns set of removed cluster groups.
     * 
     * @return set of removed custer groups
     */
    Set<IGroup> getRemovedGroups();
}