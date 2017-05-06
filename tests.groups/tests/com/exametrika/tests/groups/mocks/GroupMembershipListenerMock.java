/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import com.exametrika.api.groups.cluster.GroupMembershipEvent;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;

public class GroupMembershipListenerMock implements IGroupMembershipListener
{
    public LeaveReason leaveReason;
    public GroupMembershipEvent onMembershipChangedEvent;
    public boolean onJoined;

    @Override
    public void onJoined()
    {
        onJoined = true;
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
        this.leaveReason = reason;
    }

    @Override
    public void onMembershipChanged(GroupMembershipEvent event)
    {
        this.onMembershipChangedEvent = event;
    }
}