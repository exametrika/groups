/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.impl.groups.cluster.flush.IFlush;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;


public class FlushMock implements IFlush
{
    public boolean groupForming;
    public IGroupMembership oldMembership;
    public IGroupMembership newMembership;
    public IGroupMembershipChange membershipChange;
    public boolean granted;
    
    @Override
    public boolean isGroupForming()
    {
        return groupForming;
    }

    @Override
    public IGroupMembership getOldMembership()
    {
        return oldMembership;
    }

    @Override
    public IGroupMembership getNewMembership()
    {
        return newMembership;
    }

    @Override
    public IGroupMembershipChange getMembershipChange()
    {
        return membershipChange;
    }

    @Override
    public void grantFlush(IFlushParticipant participant)
    {
        granted = true;
    }
}
