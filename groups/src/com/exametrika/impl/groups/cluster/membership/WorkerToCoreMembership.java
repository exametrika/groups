/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
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
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<INode> coreNodes;
    private final Map<UUID, INode> coreNodesMap;
    private final Map<INode, INode> coreByWorkerMap;
    private final Map<INode, Set<INode>> workersByCoreMap;

    public WorkerToCoreMembership(List<INode> coreNodes, Map<INode, INode> coreByWorkerMap)
    {
        Assert.notNull(coreNodes);
        Assert.notNull(coreByWorkerMap);

        this.coreNodes = Immutables.wrap(coreNodes);
        this.coreByWorkerMap = Immutables.wrap(coreByWorkerMap);
        
        Map<UUID, INode> coreNodesMap = new HashMap<UUID, INode>();
        for (INode coreNode : coreNodes)
            coreNodesMap.put(coreNode.getId(), coreNode);
        
        this.coreNodesMap = coreNodesMap;
        
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

    public List<INode> getCoreNodes()
    {
        return coreNodes;
    }
    
    public INode findCoreNode(UUID nodeId)
    {
        Assert.notNull(nodeId);
        
        return coreNodesMap.get(nodeId);
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
        return messages.toString(coreNodes.toString(), Strings.toString(workersByCoreMap.entrySet(), true)).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("core nodes : {0}\nworkers by core: \n{1}")
        ILocalizedMessage toString(String coreNodes, String workersByCore);
    }
}
