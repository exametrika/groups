/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;


/**
 * The {@link IMembershipListener} listens to membership events.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IMembershipListener
{
    /**
     * Reason of leaving the cluster group.
     */
    enum LeaveReason
    {
        /** Local node has left the group gracefully. */
        GRACEFUL_EXIT,
        /** Local node has left the group forcefully. */
        FORCEFUL_CLOSE,
        /** Local node has left the group, because other nodes excluded it from the group for some reason. */
        RECONNECT,
    }
    
    /**
     * Called when local node successfully joined the cluster.
     */
    void onJoined();

    /**
     * Called when local node left the cluster.
     * 
     * @param reason leave reason
     */
    void onLeft(LeaveReason reason);
}