/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;


/**
 * The {@link IGroupMembershipListener} listens to group membership events.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IGroupMembershipListener extends IMembershipListener
{
    /**
     * Called when cluster membership has been changed. Guaranteed that all group members receive this notification before
     * new membership has been changed. Not called when local node joined or left the group, use {@link #onJoined}, {@link #onLeft} to
     * receive such notifications.
     * 
     * @param event membership event
     */
    void onMembershipChanged(GroupMembershipEvent event);
}