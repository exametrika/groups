/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.membership;

import java.util.UUID;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.impl.transports.AbstractAddress;

/**
 * The {@link GroupAddress} represents a group address implementation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupAddress extends AbstractAddress implements IAddress
{
    public GroupAddress(UUID id, String name)
    {
        super(id, name);
    }
    
    @Override
    public String getConnection()
    {
        return "group://" + getName();
    }
}
