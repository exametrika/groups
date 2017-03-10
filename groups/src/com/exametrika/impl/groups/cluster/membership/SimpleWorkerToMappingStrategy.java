/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.groups.core.INode;
import com.exametrika.common.utils.Assert;

/**
 * The {@link SimpleWorkerToMappingStrategy} is simple implementation of {@link IWorkerToCoreMappingStarategy}
 * which privides even mapping of worker nodes between core nodes.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimpleWorkerToMappingStrategy implements IWorkerToCoreMappingStarategy
{
    @Override
    public Map<INode, INode> mapWorkers(List<INode> coreNodes, List<INode> workerNodes,
        Map<INode, INode> oldCoreByWorkerMap)
    {
        List<MapInfo> mapInfos = new ArrayList<MapInfo>();
        for (INode node : coreNodes)
        {
            MapInfo info = new MapInfo();
            info.coreNode = node;
            mapInfos.add(info);
        }
        
        Map<INode, INode> newCoreByWorkerMap = new LinkedHashMap<INode, INode>();
        if (oldCoreByWorkerMap != null)
        {
            for (Map.Entry<INode, INode> entry : oldCoreByWorkerMap.entrySet())
            {
                INode workerNode = entry.getKey();
                INode coreNode = entry.getValue();
                newCoreByWorkerMap.put(workerNode, coreNode);
                incrementUsage(mapInfos, coreNode);
            }
        }
        
        for (INode node : workerNodes)
        {
            if (oldCoreByWorkerMap == null || oldCoreByWorkerMap.get(node) == null)
            {
                MapInfo minInfo = findLowestUsage(mapInfos);
                newCoreByWorkerMap.put(node, minInfo.coreNode);
                minInfo.count++;
            }
        }
        
        return newCoreByWorkerMap;
    }
    
    private void incrementUsage(List<MapInfo> mapInfos, INode coreNode)
    {
        for (MapInfo info : mapInfos)
        {
            if (info.coreNode.equals(coreNode))
            {
                info.count++;
                return;
            }
        }
        
        Assert.error();
    }
    
    private MapInfo findLowestUsage(List<MapInfo> mapInfos)
    {
        MapInfo minInfo = null;
        for (MapInfo info : mapInfos)
        {
            if (minInfo == null || minInfo.count > info.count)
                minInfo = info;
        }
        
        return minInfo;
    }
    
    private static class MapInfo
    {
        private INode coreNode;
        private int count;
    }
}
