/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.exchange.IExchangeData;
import com.exametrika.impl.groups.cluster.exchange.IFeedbackProvider;
import com.exametrika.impl.groups.cluster.membership.NodesMembership;

/**
 * The {@link NodeFeedbackProvider} is a node feedback provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */

public final class NodeFeedbackProvider implements IFeedbackProvider, INodeFeedbackService
{
    public static final UUID ID = UUID.fromString("ee03314e-af06-401c-b5d2-a7a47d9046c7");
    private final Map<UUID, NodeInfo> nodeInfos = new LinkedHashMap<UUID, NodeInfo>();
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new NodeFeedbackDataSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(NodeFeedbackDataSerializer.ID);
    }

    @Override
    public UUID getId()
    {
        return ID;
    }

    @Override
    public IExchangeData getData(boolean force)
    {
        List<INodeState> states = null;
        for (Map.Entry<UUID, NodeInfo> entry : nodeInfos.entrySet())
        {
            if (force || entry.getValue().modified)
            {
                if (states == null)
                    states = new ArrayList<INodeState>();
                
                states.add(entry.getValue().state);
            }
        }
        
        if (states != null)
            return new NodeFeedbackData(states);
        else
            return null;
    }

    @Override
    public void setData(IExchangeData data)
    {
        Assert.notNull(data);
        
        NodeFeedbackData feedbackData = (NodeFeedbackData)data;
        for (INodeState state : feedbackData.getStates())
            updateNodeState(state);
    }

    @Override
    public void onClusterMembershipChanged(IClusterMembership membership)
    {
        Assert.notNull(membership);
        
        for (Iterator<Map.Entry<UUID, NodeInfo>> it = nodeInfos.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<UUID, NodeInfo> entry = it.next();
            INodeState state = entry.getValue().state;
            IDomainMembership domainMembership = membership.findDomain(state.getDomain());
            if (domainMembership != null)
            {
                NodesMembership nodesMembership = domainMembership.findElement(NodesMembership.class);
                if (nodesMembership.findNode(state.getId()) != null)
                    continue;
            }
            
            it.remove();
        }
    }

    @Override
    public Set<INodeState> getNodeStates()
    {
        Set<INodeState> states = new LinkedHashSet<INodeState>();
        for (NodeInfo info : nodeInfos.values())
            states.add(info.state);
        
        return states;
    }

    @Override
    public INodeState findNodeState(UUID id)
    {
        Assert.notNull(id);
        
        NodeInfo info = nodeInfos.get(id);
        if (info != null)
            return info.state;
        else
            return null;
    }

    @Override
    public void updateNodeState(INodeState state)
    {
        Assert.notNull(state);
        
        NodeInfo info = new NodeInfo();
        info.state = state;
        info.modified = true;
        
        nodeInfos.put(state.getId(), info);
    }
    
    private static class NodeInfo
    {
        private INodeState state;
        private boolean modified;
    }
}
