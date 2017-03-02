/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;

/**
 * The {@link WorkerToCoreMembership} is worker to core node mapping membership.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class WorkerToCoreMembership implements IClusterMembershipElement
{
    private final Map<INode, INode> coreByWorkerMap;
    private final Map<INode, Set<INode>> workersByCoreMap;

    public WorkerToCoreMembership(Map<INode, INode> coreByWorkerMap)
    {
        Assert.notNull(coreByWorkerMap);

        this.coreByWorkerMap = Immutables.wrap(coreByWorkerMap);
        
        Map<INode, Set<INode>> workersByCoreMap = new HashMap<INode, Set<INode>>();
        for (Map.Entry<INode, INode> entry : coreByWorkerMap.entrySet())
        {
            INode workerNode = entry.getKey();
            INode coreNode = entry.getValue();
            Set<INode> workerNodes = workersByCoreMap.get(coreNode);
            if (workerNodes == null)
            {
                workerNodes = Immutables.wrap(new LinkedHashSet<INode>());
                workersByCoreMap.put(coreNode, workerNodes);
            }
            workerNodes = Immutables.unwrap(workerNodes);
            workerNodes.add(workerNode);
        }
        
        this.workersByCoreMap = workersByCoreMap;
    }

    public Map<INode, INode> getCoreByWorkerMap()
    {
        return coreByWorkerMap;
    }

    public Set<INode> findWorkerNodes(INode coreNode)
    {
        Assert.notNull(coreNode);
        
        return workersByCoreMap.get(coreNode);
    }
    
    public INode findCoreNode(INode workerNode)
    {
        Assert.notNull(workerNode);
        
        return coreByWorkerMap.get(workerNode);
    }
    
    @Override
    public String toString()
    {
        return Strings.toString(workersByCoreMap.entrySet(), false);
    }
}
