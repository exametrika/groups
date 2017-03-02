/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.Map;

import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link WorkerToCoreMembershipChange} is worker to core node mapping membership change.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class WorkerToCoreMembershipChange implements IClusterMembershipElementChange
{
    private final Map<INode, INode> newCoreByWorkerMap;

    public WorkerToCoreMembershipChange(Map<INode, INode> newCoreByWorkerMap)
    {
        Assert.notNull(newCoreByWorkerMap);

        this.newCoreByWorkerMap = Immutables.wrap(newCoreByWorkerMap);
    }

    public Map<INode, INode> getNewCoreByWorkerMap()
    {
        return newCoreByWorkerMap;
    }

    @Override
    public String toString()
    {
        return newCoreByWorkerMap.toString();
    }
}
