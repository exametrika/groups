/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;

public class PreparedGroupMembershipListenerMock implements IPreparedGroupMembershipListener
{
    public IGroupMembership oldMembership;
    public IGroupMembership newMembership;
    public IGroupMembershipChange change;

    @Override
    public void onPreparedMembershipChanged(IGroupMembership oldMembership, IGroupMembership newMembership,
        IGroupMembershipChange change)
    {
        this.oldMembership = oldMembership;
        this.newMembership = newMembership;
        this.change = change;
    }
}