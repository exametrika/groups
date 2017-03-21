/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.api.groups.cluster.IGroupChange;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.common.utils.Assert;

/**
 * The {@link GroupMembershipChange} is implementation of {@link IGroupMembershipChange}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupMembershipChange implements IGroupMembershipChange
{
    private final IGroupChange group;

    public GroupMembershipChange(IGroupChange group)
    {
        Assert.notNull(group);
        
        this.group = group;
    }

    @Override
    public IGroupChange getGroup()
    {
        return group;
    }
    
    @Override
    public String toString()
    {
        return group.toString();
    }
}
