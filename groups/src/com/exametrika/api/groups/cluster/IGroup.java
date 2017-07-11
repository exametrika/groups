/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.common.messaging.IAddress;

/**
 * The {@link IGroup} is a cluster dynamic group of nodes.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IGroup extends Comparable<IGroup>
{
    /**
     * Returns unique identifier of group.
     * 
     * @return unique identifier of group
     */
    UUID getId();
    
    /**
     * Returns identifier, monotonically increasing on each group change.
     *
     * @return group change identifier
     */
    long getChangeId();
    
    /**
     * Returns unique group name.
     * 
     * @return group name
     */
    String getName();
    
    /**
     * Returns group address.
     *
     * @return group address
     */
    IAddress getAddress();
    
    /**
     * Returns group options.
     *
     * @return group options
     */
    Set<GroupOption> getOptions();
    
    /**
     * Is local node belongs to group primary partition?
     *
     * @return true - if local node in group primary partition
     */
    boolean isPrimary();

    /**
     * Returns group coordinator.
     * 
     * @return group coordinator
     */
    INode getCoordinator();

    /**
     * Returns list of group members. List is ordered from oldest to youngest member. First member of this list is current group coordinator.
     * 
     * @return list of group members
     */
    List<INode> getMembers();
    
    /**
     * Finds group member by identifier.
     * 
     * @param nodeId node identifier of member
     * @return group member or <c>null</c>, if group member is not found
     */
    INode findMember(UUID nodeId);
    
    /**
     * Finds group member by address.
     * 
     * @param address address of member
     * @return group member or <c>null</c>, if group member is not found
     */
    INode findMember(IAddress address);
    
    @Override
    public abstract boolean equals(Object o);
    
    @Override
    public abstract int hashCode();
}