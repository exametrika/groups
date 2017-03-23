/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.UUID;

import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;

/**
 * The {@link GroupMessagePart} is a group message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupMessagePart implements IMessagePart
{
    private final UUID groupId;

    public GroupMessagePart(UUID groupId)
    {
        Assert.notNull(groupId);
        
        this.groupId = groupId;
    }
    
    public UUID getGroupId()
    {
        return groupId;
    }
    
    @Override
    public int getSize()
    {
        return 16;
    }
    
    @Override 
    public String toString()
    {
        return groupId.toString();
    }
}

