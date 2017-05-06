/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.impl.groups.cluster.flush.IFlushManager;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipDelta;

public class FlushManagerMock implements IFlushManager
{
    public IGroupMembership membership;
    public IGroupMembershipDelta membershipDelta;
    public boolean flushInProgress;

    @Override
    public boolean isFlushInProgress()
    {
        return flushInProgress;
    }

    @Override
    public void install(IGroupMembership membership, IGroupMembershipDelta membershipDelta)
    {
        this.membership = membership;
        this.membershipDelta = membershipDelta;
    }
}