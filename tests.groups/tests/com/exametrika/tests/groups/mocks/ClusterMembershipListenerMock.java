/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import com.exametrika.api.groups.cluster.ClusterMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;

public class ClusterMembershipListenerMock implements IClusterMembershipListener
{
    public LeaveReason leaveReason;
    public ClusterMembershipEvent onMembershipChangedEvent;
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
    public void onMembershipChanged(ClusterMembershipEvent event)
    {
        this.onMembershipChangedEvent = event;
    }
}