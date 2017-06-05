/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.Set;

import com.exametrika.common.utils.Assert;

/**
 * The {@link GroupOption} defines group option.
 */
public enum GroupOption
{
    /** Implements durable multicast. */
    DURABLE,
    /** Implements total ordered multicast. */
    ORDERED,
    /** Implements simple state transfer. */
    SIMPLE_STATE_TRANSFER,
    /** Implements async state transfer. */
    ASYNC_STATE_TRANSFER,
    /** Implements group state equality check.*/
    CHECK_STATE;
    
    public static Set<GroupOption> validate(Set<GroupOption> options)
    {
        Assert.notNull(options);
        
        if (options.contains(ORDERED))
            options.add(DURABLE);
        
        if (!options.contains(SIMPLE_STATE_TRANSFER) && !options.contains(ASYNC_STATE_TRANSFER))
            options.add(SIMPLE_STATE_TRANSFER);
        
        if (options.contains(ASYNC_STATE_TRANSFER))
            options.remove(SIMPLE_STATE_TRANSFER);
        
        return options;
    }
}