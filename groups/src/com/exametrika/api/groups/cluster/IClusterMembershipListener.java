/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;


/**
 * The {@link IClusterMembershipListener} listens to cluster membership events.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IClusterMembershipListener
{
    /**
     * Reason of leaving the cluster.
     */
    enum LeaveReason
    {
        /** Local node has left the cluster gracefully. */
        GRACEFUL_CLOSE,
        /** Local node has left the cluster forcefully. */
        FORCEFUL_CLOSE,
        /** Local node has left the cluster, because other nodes excluded it from the cluster for some reason. */
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
    
    /**
     * Called when cluster membership has been changed. Guaranteed that all group members receive this notification before
     * new membership has been changed. Not called when local node joined or left the group, use {@link #onJoined}, {@link #onLeft} to
     * receive such notifications.
     * 
     * @param event membership event
     */
    void onMembershipChanged(ClusterMembershipEvent event);
}