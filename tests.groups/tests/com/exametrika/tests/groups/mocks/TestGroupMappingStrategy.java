/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.mocks;

import java.util.List;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.cluster.membership.GroupsMembership;
import com.exametrika.impl.groups.cluster.membership.IGroupDelta;
import com.exametrika.impl.groups.cluster.membership.IGroupMappingStrategy;
import com.exametrika.impl.groups.cluster.membership.NodesMembership;
import com.exametrika.impl.groups.cluster.membership.NodesMembershipDelta;

public class TestGroupMappingStrategy implements IGroupMappingStrategy
{
    public List<Pair<IGroup, IGroupDelta>> result;
    
    @Override
    public List<Pair<IGroup, IGroupDelta>> mapGroups(long membershipId, String domain,
        NodesMembership nodeMembership, NodesMembershipDelta nodesMembershipDelta,
        GroupsMembership oldGroupMembership)
    {
        return result;
    }
}