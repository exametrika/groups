/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.core;


/**
 * The {@link IMembershipService} manages group membership.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IMembershipService
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
    IMembership getMembership();
    
    /**
     * Adds membership listener.
     *
     * @param listener listener
     */
    void addMembershipListener(IMembershipListener listener);
    
    /**
     * Removes membership listener.
     *
     * @param listener listener
     */
    void removeMembershipListener(IMembershipListener listener);
    
    /**
     * Removes all membership listeners.
     */
    void removeAllMembershipListeners();
}