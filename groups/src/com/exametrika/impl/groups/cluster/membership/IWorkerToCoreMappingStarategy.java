/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.List;
import java.util.Map;

import com.exametrika.api.groups.cluster.INode;

/**
 * The {@link IWorkerToCoreMappingStarategy} represents a strategy which maps worker nodes to core nodes.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IWorkerToCoreMappingStarategy
{

    /**
     * Maps worker nodes to core nodes.
     *
     * @param coreNodes list of core nodes
     * @param workerNodes list of worker nodes
     * @param oldCoreByWorkerMap old mapping of worker to core nodes or null if mapping is not set
     * @return new mapping of worker to core nodes
     */
    Map<INode, INode> mapWorkers(List<INode> coreNodes, List<INode> workerNodes, Map<INode, INode> oldCoreByWorkerMap);
}