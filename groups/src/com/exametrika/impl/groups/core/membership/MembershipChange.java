/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import com.exametrika.api.groups.core.IGroupChange;
import com.exametrika.api.groups.core.IMembershipChange;
import com.exametrika.common.utils.Assert;

/**
 * The {@link MembershipChange} is implementation of {@link IMembershipChange}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MembershipChange implements IMembershipChange
{
    private final IGroupChange group;

    public MembershipChange(IGroupChange group)
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
