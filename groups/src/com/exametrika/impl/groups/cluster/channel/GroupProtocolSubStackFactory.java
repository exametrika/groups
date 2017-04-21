/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.impl.groups.cluster.membership.GroupProtocolSubStack;
import com.exametrika.impl.groups.cluster.membership.IGroupProtocolSubStackFactory;

/**
 * The {@link GroupProtocolSubStackFactory} is a worker group protocol sub-stack factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class GroupProtocolSubStackFactory implements IGroupProtocolSubStackFactory
{
    @Override
    public GroupProtocolSubStack createProtocolSubStack(IGroup group)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
