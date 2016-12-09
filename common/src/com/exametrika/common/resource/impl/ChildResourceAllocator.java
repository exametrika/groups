/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.util.Map;

import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.time.ITimeService;

/**
 * The {@link ChildResourceAllocator} is an implementation of {@link IResourceAllocator} which itself is a consumer of
 * some part of resource.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ChildResourceAllocator extends ResourceAllocator
{
    public ChildResourceAllocator(String name, Map<String, IAllocationPolicy> policies, IAllocationPolicy defaultPolicy, 
        long quotaIncreaseDelay, long initializePeriod, ITimeService timeService)
    {
        super(name, policies, defaultPolicy, quotaIncreaseDelay, initializePeriod, timeService);
    }
}
