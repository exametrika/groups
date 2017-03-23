/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import com.exametrika.api.groups.cluster.IGroup;


/**
 * The {@link IGroupProtocolSubStackFactory} represents a factory of group sub-stack.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IGroupProtocolSubStackFactory
{
    /**
     * Creates protocol sub-stack for specified group.
     *
     * @param group group
     * @return protocol sub-stack
     */
    GroupProtocolSubStack createProtocolSubStack(IGroup group);
}