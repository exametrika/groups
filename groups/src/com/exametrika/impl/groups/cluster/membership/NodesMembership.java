/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.api.groups.cluster.INodesMembership;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link NodesMembership} is implementation of {@link INodesMembership}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NodesMembership implements INodesMembership
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<INode> nodes;
    private final Map<UUID, INode> nodesByIdMap;
    private final Map<IAddress, INode> nodesByAddressMap;

    public NodesMembership(List<? extends INode> nodes)
    {
        Assert.notNull(nodes);

        this.nodes = Immutables.wrap(nodes);
        
        Map<UUID, INode> nodesByIdMap = new HashMap<UUID, INode>();
        Map<IAddress, INode> nodesByAddressMap = new HashMap<IAddress, INode>();
        for (INode node : nodes)
        {
            nodesByIdMap.put(node.getId(), node);
            nodesByAddressMap.put(node.getAddress(), node);
        }
        
        this.nodesByIdMap = nodesByIdMap;
        this.nodesByAddressMap = nodesByAddressMap;
    }

    @Override
    public List<INode> getNodes()
    {
        return nodes;
    }

    @Override
    public INode findNode(UUID nodeId)
    {
        Assert.notNull(nodeId);
        
        return nodesByIdMap.get(nodeId);
    }

    @Override
    public INode findNode(IAddress address)
    {
        Assert.notNull(address);
        
        return nodesByAddressMap.get(address);
    }

    @Override
    public String toString()
    {
        return messages.toString(nodes).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("nodes: {0}")
        ILocalizedMessage toString(List<INode> nodes);
    }
}
