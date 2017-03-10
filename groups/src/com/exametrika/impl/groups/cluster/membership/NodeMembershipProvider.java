/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
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
    private Set<INode> discoveredNodes;
    private Set<INode> failedNodes;
    private Set<INode> leftNodes;
    
    public NodeMembershipProvider(IWorkerNodeDiscoverer nodeDiscoverer, IWorkerFailureDetector failureDetector)
    {
        Assert.notNull(nodeDiscoverer);
        Assert.notNull(failureDetector);
        
        this.nodeDiscoverer = nodeDiscoverer;
        this.failureDetector = failureDetector;
    }
    
    @Override
    public Set<String> getDomains()
    {
        discoveredNodes = nodeDiscoverer.takeDiscoveredNodes();
        failedNodes = failureDetector.takeFailedNodes();
        leftNodes = failureDetector.takeLeftNodes();
        
        Set<String> domains = new LinkedHashSet<String>();
        for (INode node : discoveredNodes)
            domains.add(node.getDomain());
        for (INode node : failedNodes)
            domains.add(node.getDomain());
        for (INode node : leftNodes)
            domains.add(node.getDomain());
        
        return domains;
    }
    
    @Override
    public Pair<IClusterMembershipElement, IClusterMembershipElementDelta> getDelta(IDomainMembership newDomainMembership,
        IDomainMembershipDelta domainMembershipDelta, IDomainMembership oldDomainMembership, IClusterMembershipElement oldMembership)
    {
        if (discoveredNodes.isEmpty() && failedNodes.isEmpty() && leftNodes.isEmpty())
            return new Pair<IClusterMembershipElement, IClusterMembershipElementDelta>(oldMembership, null);
        
        NodeMembership oldNodeMembership = (NodeMembership)oldMembership;
        Set<INode> joinedNodes = new LinkedHashSet<INode>();
        for (INode node : discoveredNodes)
        {
            if (oldNodeMembership != null && oldNodeMembership.findNode(node.getId()) != null)
                continue;
            
            joinedNodes.add(node);
        }
        
        Set<UUID> failedNodeIds = new LinkedHashSet<UUID>();
        for (INode node : failedNodes)
        {
            if (oldNodeMembership == null || oldNodeMembership.findNode(node.getId()) == null)
                continue;
            
            failedNodeIds.add(node.getId());
        }
        
        Set<UUID> leftNodeIds = new LinkedHashSet<UUID>();
        for (INode node : leftNodes)
        {
            if (oldNodeMembership == null || oldNodeMembership.findNode(node.getId()) == null)
                continue;
            
            leftNodeIds.add(node.getId());
        }
        IClusterMembershipElementDelta delta = null;
        if (!joinedNodes.isEmpty() || !failedNodeIds.isEmpty() || !leftNodeIds.isEmpty())
            delta = new NodeMembershipDelta(joinedNodes, leftNodeIds, failedNodeIds);
        
        NodeMembership newNodeMembership = oldNodeMembership;
        if (delta != null)
        {
            List<INode> nodes = new ArrayList<INode>();
            if (oldNodeMembership != null)
            {
                for (INode node : oldNodeMembership.getNodes())
                {
                    if (!failedNodeIds.contains(node.getId()) && !leftNodeIds.contains(node.getId()))
                        nodes.add(node);
                }
            }
            
            nodes.addAll(joinedNodes);
            
            newNodeMembership = new NodeMembership(nodes);
        }
        
        return new Pair<IClusterMembershipElement, IClusterMembershipElementDelta>(newNodeMembership, delta);
    }
    
    @Override
    public void clearState()
    {
        discoveredNodes = null;
        failedNodes = null;
        leftNodes = null;
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
    public IClusterMembershipElement createMembership(IDomainMembership newDomainMembership, IClusterMembershipElementDelta delta,
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
    public IClusterMembershipElementChange createChange(IDomainMembership newDomainMembership, IClusterMembershipElementDelta delta,
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
    public IClusterMembershipElementChange createChange(IDomainMembership newDomainMembership, IClusterMembershipElement newMembership,
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
