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
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.cluster.discovery.IWorkerNodeDiscoverer;
import com.exametrika.impl.groups.cluster.failuredetection.IClusterFailureDetector;

/**
 * The {@link NodesMembershipProvider} is an implementation of nodes membership provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NodesMembershipProvider implements IClusterMembershipProvider
{
    private IWorkerNodeDiscoverer nodeDiscoverer;
    private final IClusterFailureDetector failureDetector;
    private Set<INode> discoveredNodes;
    private Set<INode> failedNodes;
    private Set<INode> leftNodes;
    
    public NodesMembershipProvider()
    {
        this.failureDetector = null;
    }
    
    public NodesMembershipProvider(IClusterFailureDetector failureDetector)
    {
        Assert.notNull(failureDetector);
        
        this.failureDetector = failureDetector;
    }
    
    public void setNodeDiscoverer(IWorkerNodeDiscoverer nodeDiscoverer)
    {
        Assert.notNull(nodeDiscoverer);
        Assert.isNull(this.nodeDiscoverer);
        
        this.nodeDiscoverer = nodeDiscoverer;
    }

    @Override
    public Set<String> getDomains()
    {
        discoveredNodes = nodeDiscoverer.takeDiscoveredNodes();
        failedNodes = failureDetector.getFailedNodes();
        leftNodes = failureDetector.getLeftNodes();
        
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
    public Pair<IClusterMembershipElement, IClusterMembershipElementDelta> getDelta(long membershipId, IDomainMembership newDomainMembership,
        IDomainMembershipDelta domainMembershipDelta, IDomainMembership oldDomainMembership, IClusterMembershipElement oldMembership)
    {
        if (discoveredNodes.isEmpty() && failedNodes.isEmpty() && leftNodes.isEmpty())
            return new Pair<IClusterMembershipElement, IClusterMembershipElementDelta>(oldMembership, null);
        
        NodesMembership oldNodeMembership = (NodesMembership)oldMembership;
        List<INode> joinedNodes = new ArrayList<INode>();
        for (INode node : discoveredNodes)
        {
            if (!node.getDomain().equals(newDomainMembership.getName()))
                continue;
            if (oldNodeMembership != null && oldNodeMembership.findNode(node.getId()) != null)
                continue;
            
            joinedNodes.add(node);
        }
        
        Set<UUID> failedNodeIds = new LinkedHashSet<UUID>();
        for (INode node : failedNodes)
        {
            if (!node.getDomain().equals(newDomainMembership.getName()))
                continue;
            if (oldNodeMembership == null || oldNodeMembership.findNode(node.getId()) == null)
                continue;
            
            failedNodeIds.add(node.getId());
        }
        
        Set<UUID> leftNodeIds = new LinkedHashSet<UUID>();
        for (INode node : leftNodes)
        {
            if (!node.getDomain().equals(newDomainMembership.getName()))
                continue;
            if (oldNodeMembership == null || oldNodeMembership.findNode(node.getId()) == null)
                continue;
            
            leftNodeIds.add(node.getId());
        }
        IClusterMembershipElementDelta delta = null;
        if (!joinedNodes.isEmpty() || !failedNodeIds.isEmpty() || !leftNodeIds.isEmpty())
            delta = new NodesMembershipDelta(joinedNodes, leftNodeIds, failedNodeIds);
        
        NodesMembership newNodeMembership = oldNodeMembership;
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
            
            newNodeMembership = new NodesMembership(nodes);
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
        NodesMembership nodeMembership = (NodesMembership)membership;
        return nodeMembership.getNodes().isEmpty();
    }
    
    @Override
    public IClusterMembershipElementDelta createCoreFullDelta(IClusterMembershipElement membership)
    {
        Assert.notNull(membership);
        
        NodesMembership nodeMembership = (NodesMembership)membership;
        return new NodesMembershipDelta(nodeMembership.getNodes(), java.util.Collections.<UUID>emptySet(), 
            java.util.Collections.<UUID>emptySet());
    }
    
    @Override
    public IClusterMembershipElementDelta createWorkerDelta(IClusterMembershipElement membership,
        IClusterMembershipElementDelta delta, boolean full, boolean publicPart)
    {
        if (publicPart)
            return createEmptyDelta();
        
        if (full)
            return createCoreFullDelta(membership);
        else
        {
            Assert.notNull(delta);
            return delta;
        }
    }
    
    @Override
    public IClusterMembershipElementDelta createEmptyDelta()
    {
        return new NodesMembershipDelta(java.util.Collections.<INode>emptyList(), java.util.Collections.<UUID>emptySet(), 
            java.util.Collections.<UUID>emptySet());
    }
    
    @Override
    public IClusterMembershipElement createMembership(IDomainMembership newDomainMembership, IClusterMembershipElementDelta delta,
        IClusterMembershipElement oldMembership)
    {
        Assert.notNull(delta);
        
        NodesMembershipDelta nodeDelta = (NodesMembershipDelta)delta;
        NodesMembership oldNodeMembership = (NodesMembership)oldMembership;
        if (oldMembership == null)
            return new NodesMembership(new ArrayList<INode>(nodeDelta.getJoinedNodes()));
        else
        {
            List<INode> nodes = new ArrayList<INode>();
            for (INode node : oldNodeMembership.getNodes())
            {
                if (!nodeDelta.getFailedNodes().contains(node.getId()) && !nodeDelta.getLeftNodes().contains(node.getId()))
                    nodes.add(node);
            }
            
            nodes.addAll(nodeDelta.getJoinedNodes());
            
            return new NodesMembership(nodes);
        }
    }

    @Override
    public IClusterMembershipElementChange createChange(IDomainMembership newDomainMembership, IClusterMembershipElementDelta delta,
        IClusterMembershipElement oldMembership)
    {
        Assert.notNull(delta);
        Assert.notNull(oldMembership);
        
        NodesMembershipDelta nodeDelta = (NodesMembershipDelta)delta;
        NodesMembership oldNodeMembership = (NodesMembership)oldMembership;
  
        Set<INode> failedNodes = new HashSet<INode>();
        Set<INode> leftNodes = new HashSet<INode>();
        for (INode node : oldNodeMembership.getNodes())
        {
            if (nodeDelta.getFailedNodes().contains(node.getId()))
                failedNodes.add(node);
            else if (nodeDelta.getLeftNodes().contains(node.getId()))
                leftNodes.add(node);
        }
        
        return new NodesMembershipChange(nodeDelta.getJoinedNodes(), leftNodes, failedNodes);
    }

    @Override
    public IClusterMembershipElementChange createChange(IDomainMembership newDomainMembership, IClusterMembershipElement newMembership,
        IClusterMembershipElement oldMembership)
    {
        Assert.notNull(newMembership);
        Assert.notNull(oldMembership);
        
        NodesMembership newNodeMembership = (NodesMembership)newMembership;
        NodesMembership oldNodeMembership = (NodesMembership)oldMembership;
        
        Set<INode> failedNodes = new HashSet<INode>();
        for (INode node : oldNodeMembership.getNodes())
        {
            if (newNodeMembership.findNode(node.getId()) == null)
                failedNodes.add(node);
        }
        
        List<INode> joinedNodes = new ArrayList<INode>();
        for (INode node : newNodeMembership.getNodes())
        {
            if (oldNodeMembership.findNode(node.getId()) == null)
                joinedNodes.add(node);
        }
        return new NodesMembershipChange(joinedNodes, java.util.Collections.<INode>emptySet(), failedNodes);
    }
}
