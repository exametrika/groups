/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

/**
 * The {@link IClusterMembershipService} manages cluster membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IClusterMembershipService
{
    /**
     * Returns local node.
     * 
     * @return local node
     */
    INode getLocalNode();

    /**
     * Returns current membership
     * 
     * @return current membership or <cc>null<cc> if current membership is not installed
     */
    IClusterMembership getMembership();
    
    /**
     * Adds membership listener.
     *
     * @param listener listener
     */
    void addMembershipListener(IClusterMembershipListener listener);
    
    /**
     * Removes membership listener.
     *
     * @param listener listener
     */
    void removeMembershipListener(IClusterMembershipListener listener);
    
    /**
     * Removes all membership listeners.
     */
    void removeAllMembershipListeners();
}