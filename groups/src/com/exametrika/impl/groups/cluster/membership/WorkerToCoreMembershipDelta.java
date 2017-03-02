/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.Map;
import java.util.UUID;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link WorkerToCoreMembershipDelta} is worker to core node mapping membership delta.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class WorkerToCoreMembershipDelta implements IClusterMembershipElementDelta
{
    private final Map<UUID, UUID> newCoreByWorkerMap;

    public WorkerToCoreMembershipDelta(Map<UUID, UUID> newCoreByWorkerMap)
    {
        Assert.notNull(newCoreByWorkerMap);

        this.newCoreByWorkerMap = Immutables.wrap(newCoreByWorkerMap);
    }

    public Map<UUID, UUID> getNewCoreByWorkerMap()
    {
        return newCoreByWorkerMap;
    }

    @Override
    public String toString()
    {
        return newCoreByWorkerMap.toString();
    }
}
