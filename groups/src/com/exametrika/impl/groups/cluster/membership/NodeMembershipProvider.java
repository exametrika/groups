/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.groups.cluster.discovery.IWorkerNodeDiscoverer;
import com.exametrika.impl.groups.cluster.failuredetection.IWorkerFailureDetector;

/**
 * The {@link NodeMembershipProvider} is an implementation of node membership provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NodeMembershipProvider implements IClusterMembershipProvider
{
    private final IWorkerNodeDiscoverer nodeDiscoverer;
    private final IWorkerFailureDetector failureDetector;
    
    public NodeMembershipProvider(IWorkerNodeDiscoverer nodeDiscoverer, IWorkerFailureDetector failureDetector)
    {
        Assert.notNull(nodeDiscoverer);
        Assert.notNull(failureDetector);
        
        this.nodeDiscoverer = nodeDiscoverer;
        this.failureDetector = failureDetector;
    }
    
    @Override
    public Map<String, IClusterMembershipElementDelta> getDeltas(Map<String, IClusterMembershipElement> membership)
    {
        Set<INode> discoveredNodes = nodeDiscoverer.takeDiscoveredNodes();
        Set<INode> failedNodes = failureDetector.takeFailedNodes();
        Set<INode> leftNodes = failureDetector.takeLeftNodes();
        
        if (discoveredNodes.isEmpty() && failedNodes.isEmpty() && leftNodes.isEmpty())
            return java.util.Collections.emptyMap();
        
        Map<String, NodeMembershipDelta> map = new LinkedHashMap<String, NodeMembershipDelta>();
        for (INode node : discoveredNodes)
        {
            NodeMembership oldNodeMembership = null;
            if (membership != null)
                oldNodeMembership = (NodeMembership)membership.get(node.getDomain());
            if (oldNodeMembership != null && oldNodeMembership.findNode(node.getId()) != null)
                continue;
            
            NodeMembershipDelta delta = map.get(node.getDomain());
            if (delta == null)
            {
                delta = new NodeMembershipDelta(new LinkedHashSet<INode>(), new LinkedHashSet<UUID>(), new LinkedHashSet<UUID>());
                map.put(node.getDomain(), delta);
            }
            
            Immutables.unwrap(delta.getJoinedNodes()).add(node);
        }
        
        for (INode node : failedNodes)
        {
            NodeMembership oldNodeMembership = null;
            if (membership != null)
                oldNodeMembership = (NodeMembership)membership.get(node.getDomain());
            if (oldNodeMembership == null || oldNodeMembership.findNode(node.getId()) == null)
                continue;
            
            NodeMembershipDelta delta = map.get(node.getDomain());
            if (delta == null)
            {
                delta = new NodeMembershipDelta(new LinkedHashSet<INode>(), new LinkedHashSet<UUID>(), new LinkedHashSet<UUID>());
                map.put(node.getDomain(), delta);
            }
            
            Immutables.unwrap(delta.getFailedNodes()).add(node.getId());
        }
        
        for (INode node : leftNodes)
        {
            NodeMembership oldNodeMembership = null;
            if (membership != null)
                oldNodeMembership = (NodeMembership)membership.get(node.getDomain());
            if (oldNodeMembership == null || oldNodeMembership.findNode(node.getId()) == null)
                continue;
            
            NodeMembershipDelta delta = map.get(node.getDomain());
            if (delta == null)
            {
                delta = new NodeMembershipDelta(new LinkedHashSet<INode>(), new LinkedHashSet<UUID>(), new LinkedHashSet<UUID>());
                map.put(node.getDomain(), delta);
            }
            
            Immutables.unwrap(delta.getLeftNodes()).add(node.getId());
        }
        
        return (Map)map;
    }
    
    @Override
    public boolean isEmptyMembership(IClusterMembershipElement membership)
    {
        NodeMembership nodeMembership = (NodeMembership)membership;
        return nodeMembership.getNodes().isEmpty();
    }
    
    @Override
    public IClusterMembershipElementDelta createWorkerDelta(IClusterMembershipElement membership,
        IClusterMembershipElementDelta delta, boolean full, boolean publicPart)
    {
        if (publicPart)
            return createEmptyDelta();
        
        if (full)
        {
            Assert.notNull(membership);
            
            NodeMembership nodeMembership = (NodeMembership)membership;
            return new NodeMembershipDelta(new LinkedHashSet<INode>(nodeMembership.getNodes()), java.util.Collections.<UUID>emptySet(), 
                java.util.Collections.<UUID>emptySet());
        }
        else
        {
            Assert.notNull(delta);
            
            return delta;
        }
    }
    
    @Override
    public IClusterMembershipElementDelta createEmptyDelta()
    {
        return new NodeMembershipDelta(java.util.Collections.<INode>emptySet(), java.util.Collections.<UUID>emptySet(), 
            java.util.Collections.<UUID>emptySet());
    }
    
    @Override
    public IClusterMembershipElement createMembership(IClusterMembershipElementDelta delta,
        IClusterMembershipElement oldMembership)
    {
        Assert.notNull(delta);
        
        NodeMembershipDelta nodeDelta = (NodeMembershipDelta)delta;
        NodeMembership oldNodeMembership = (NodeMembership)oldMembership;
        if (oldMembership == null)
            return new NodeMembership(new ArrayList<>(nodeDelta.getJoinedNodes()));
        else
        {
            List<INode> nodes = new ArrayList<INode>();
            for (INode node : oldNodeMembership.getNodes())
            {
                if (!nodeDelta.getFailedNodes().contains(node.getId()) && !nodeDelta.getLeftNodes().contains(node.getId()))
                    nodes.add(node);
            }
            
            nodes.addAll(nodeDelta.getJoinedNodes());
            
            return new NodeMembership(nodes);
        }
    }

    @Override
    public IClusterMembershipElementChange createChange(IClusterMembershipElementDelta delta,
        IClusterMembershipElement oldMembership)
    {
        Assert.notNull(delta);
        Assert.notNull(oldMembership);
        
        NodeMembershipDelta nodeDelta = (NodeMembershipDelta)delta;
        NodeMembership oldNodeMembership = (NodeMembership)oldMembership;
  
        Set<INode> failedNodes = new HashSet<INode>();
        Set<INode> leftNodes = new HashSet<INode>();
        for (INode node : oldNodeMembership.getNodes())
        {
            if (nodeDelta.getFailedNodes().contains(node.getId()))
                failedNodes.add(node);
            else if (nodeDelta.getLeftNodes().contains(node.getId()))
                leftNodes.add(node);
        }
        
        return new NodeMembershipChange(nodeDelta.getJoinedNodes(), leftNodes, failedNodes);
    }

    @Override
    public IClusterMembershipElementChange createChange(IClusterMembershipElement newMembership,
        IClusterMembershipElement oldMembership)
    {
        Assert.notNull(newMembership);
        Assert.notNull(oldMembership);
        
        NodeMembership newNodeMembership = (NodeMembership)newMembership;
        NodeMembership oldNodeMembership = (NodeMembership)oldMembership;
        
        Set<INode> failedNodes = new HashSet<INode>();
        for (INode node : oldNodeMembership.getNodes())
        {
            if (newNodeMembership.findNode(node.getId()) == null)
                failedNodes.add(node);
        }
        
        Set<INode> joinedNodes = new HashSet<INode>();
        for (INode node : newNodeMembership.getNodes())
        {
            if (oldNodeMembership.findNode(node.getId()) == null)
                joinedNodes.add(node);
        }
        return new NodeMembershipChange(joinedNodes, java.util.Collections.<INode>emptySet(), failedNodes);
    }
}
