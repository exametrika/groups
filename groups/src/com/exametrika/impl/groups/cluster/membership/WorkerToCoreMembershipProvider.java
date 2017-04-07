/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IClusterMembershipElementChange;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Pair;

/**
 * The {@link WorkerToCoreMembershipProvider} is an implementation of worker to core node membership provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class WorkerToCoreMembershipProvider implements IClusterMembershipProvider
{
    private final IGroupMembershipService membershipService;
    private final IWorkerToCoreMappingStarategy mappingStarategy;

    public WorkerToCoreMembershipProvider(IGroupMembershipService membershipService, IWorkerToCoreMappingStarategy mappingStarategy)
    {
        Assert.notNull(membershipService);
        Assert.notNull(mappingStarategy);
        
        this.membershipService = membershipService;
        this.mappingStarategy = mappingStarategy;
    }
    
    @Override
    public boolean isCoreGroupOnly()
    {
        return true;
    }
    
    @Override
    public Set<String> getDomains()
    {
        return Collections.asSet(GroupMemberships.CORE_DOMAIN);
    }

    @Override
    public Pair<IClusterMembershipElement, IClusterMembershipElementDelta> getDelta(
        IDomainMembership newDomainMembership, IDomainMembershipDelta domainMembershipDelta, IDomainMembership oldDomainMembership,
        IClusterMembershipElement oldMembership)
    {
        IGroupMembership newCoreMembership = membershipService.getMembership();
        Assert.notNull(newCoreMembership);
        
        WorkerToCoreMembership oldMappingMembership = (WorkerToCoreMembership)oldMembership;
        List<INode> joinedCoreNodes = new ArrayList<INode>();
        Set<UUID> leftCoreNodes = new LinkedHashSet<UUID>();
        Set<UUID> failedCoreNodes = new LinkedHashSet<UUID>();
        if (oldMappingMembership == null)
            joinedCoreNodes.addAll(newCoreMembership.getGroup().getMembers());
        else
        {
            for (INode node : newCoreMembership.getGroup().getMembers())
            {
                if (oldMappingMembership.findCoreNode(node.getId()) == null)
                    joinedCoreNodes.add(node);
            }
            
            for (INode node : oldMappingMembership.getCoreNodes())
            {
                if (newCoreMembership.getGroup().findMember(node.getId()) == null)
                    failedCoreNodes.add(node.getId());
            }
        }
        
        NodesMembership nodeMembership = newDomainMembership.findElement(NodesMembership.class);
        Assert.notNull(nodeMembership);
        
        NodesMembershipDelta nodeMembershipDelta = domainMembershipDelta.findDelta(NodesMembershipDelta.class);
        Assert.notNull(nodeMembershipDelta);
        
        WorkerToCoreMembershipDelta delta = null;
        WorkerToCoreMembership newMembership = oldMappingMembership;
        if (!nodeMembershipDelta.getJoinedNodes().isEmpty() || !joinedCoreNodes.isEmpty() || !failedCoreNodes.isEmpty() ||
            !leftCoreNodes.isEmpty())
        {
            Map<INode, INode> oldCoreByWorkerMap = null;
            if (oldMappingMembership != null)
            {
                oldCoreByWorkerMap = new LinkedHashMap<INode, INode>();
                for (Map.Entry<INode, INode> entry : oldMappingMembership.getCoreByWorkerMap().entrySet())
                {
                    INode workerNode = entry.getKey();
                    INode coreNode = entry.getValue();
                    if (nodeMembership.findNode(workerNode.getId()) != null &&
                        newCoreMembership.getGroup().findMember(coreNode.getId()) != null)
                        oldCoreByWorkerMap.put(workerNode, coreNode);
                }
            }
            
            Map<INode, INode> newCoreByWorkerMap = mappingStarategy.mapWorkers(newCoreMembership.getGroup().getMembers(),
                nodeMembership.getNodes(), oldCoreByWorkerMap);
            Map<UUID, UUID> newCoreByWorkerMapDelta = new LinkedHashMap<UUID, UUID>();
            for (Map.Entry<INode, INode> entry : newCoreByWorkerMap.entrySet())
            {
                if (oldCoreByWorkerMap == null)
                    newCoreByWorkerMapDelta.put(entry.getKey().getId(), entry.getValue().getId());
                else 
                {
                    INode coreNode = oldCoreByWorkerMap.get(entry.getKey());
                    if (!entry.getValue().equals(coreNode))
                        newCoreByWorkerMapDelta.put(entry.getKey().getId(), entry.getValue().getId());
                }
            }
            delta = new WorkerToCoreMembershipDelta(joinedCoreNodes, leftCoreNodes, failedCoreNodes, newCoreByWorkerMapDelta);
            newMembership = new WorkerToCoreMembership(newCoreMembership.getGroup().getMembers(), newCoreByWorkerMap);
        }

        return new Pair<IClusterMembershipElement, IClusterMembershipElementDelta>(newMembership, delta);
    }

    @Override
    public IClusterMembershipElementDelta createEmptyDelta()
    {
        return new WorkerToCoreMembershipDelta(java.util.Collections.<INode>emptyList(), java.util.Collections.<UUID>emptySet(),
            java.util.Collections.<UUID>emptySet(), java.util.Collections.<UUID, UUID>emptyMap());
    }

    @Override
    public boolean isEmptyMembership(IClusterMembershipElement membership)
    {
        WorkerToCoreMembership mappingMembership = (WorkerToCoreMembership)membership;
        return mappingMembership.getCoreByWorkerMap().isEmpty();
    }

    @Override
    public IClusterMembershipElementDelta createCoreFullDelta(IClusterMembershipElement membership)
    {
        WorkerToCoreMembership mappingMembership = (WorkerToCoreMembership)membership;
        Map<UUID, UUID> coreByWorkerMapDelta = new LinkedHashMap<UUID, UUID>();
        for (Map.Entry<INode, INode> entry : mappingMembership.getCoreByWorkerMap().entrySet())
            coreByWorkerMapDelta.put(entry.getKey().getId(), entry.getValue().getId());
           
        return new WorkerToCoreMembershipDelta(mappingMembership.getCoreNodes(), java.util.Collections.<UUID>emptySet(),
            java.util.Collections.<UUID>emptySet(), coreByWorkerMapDelta);
    }
    
    @Override
    public IClusterMembershipElementDelta createWorkerDelta(IClusterMembershipElement membership,
        IClusterMembershipElementDelta delta, boolean full, boolean publicPart)
    {
        Assert.supports(false);
        return null;
    }

    @Override
    public IClusterMembershipElement createMembership(IDomainMembership newDomainMembership,
        IClusterMembershipElementDelta delta, IClusterMembershipElement oldMembership)
    {
        NodesMembership nodeMembership = newDomainMembership.findElement(NodesMembership.class);
        WorkerToCoreMembership oldMappingMembership = (WorkerToCoreMembership)oldMembership;
        WorkerToCoreMembershipDelta mappingDelta = (WorkerToCoreMembershipDelta)delta;
        List<INode> coreNodes = new ArrayList<INode>();
        Map<INode, INode> coreByWorkerMap = new LinkedHashMap<INode, INode>();
        if (oldMembership != null)
        {
            for (INode coreNode : oldMappingMembership.getCoreNodes())
            {
                if (!mappingDelta.getFailedCoreNodes().contains(coreNode.getId()) && 
                    !mappingDelta.getLeftCoreNodes().contains(coreNode.getId()))
                    coreNodes.add(coreNode);
            }
            
        }
        
        coreNodes.addAll(mappingDelta.getJoinedCoreNodes());
        Map<UUID, INode> coreNodesMap = new HashMap<UUID, INode>();
        for (INode node : coreNodes)
            coreNodesMap.put(node.getId(), node);
        
        if (oldMembership != null)
        {
            for (Map.Entry<INode, INode> entry : oldMappingMembership.getCoreByWorkerMap().entrySet())
            {
                INode workerNode = entry.getKey();
                INode coreNode = entry.getValue();
                if (nodeMembership.findNode(workerNode.getId()) == null)
                    continue;
                if (coreNodesMap.get(coreNode.getId()) == null)
                    continue;
                if (mappingDelta.getNewCoreByWorkerMap().containsKey(workerNode.getId()))
                    continue;
                
                coreByWorkerMap.put(workerNode, coreNode);
            }
        }
        
        for (Map.Entry<UUID, UUID> entry : mappingDelta.getNewCoreByWorkerMap().entrySet())
        {
            UUID workerId = entry.getKey();
            UUID coreId = entry.getValue();
            INode workerNode = nodeMembership.findNode(workerId);
            Assert.notNull(workerNode);
           
            INode coreNode = coreNodesMap.get(coreId);
            Assert.notNull(coreNode);
            coreByWorkerMap.put(workerNode, coreNode);
        }
        
        return new WorkerToCoreMembership(coreNodes, coreByWorkerMap);
    }

    @Override
    public IClusterMembershipElementChange createChange(IDomainMembership newDomainMembership,
        IClusterMembershipElementDelta delta, IClusterMembershipElement oldMembership)
    {
        NodesMembership nodeMembership = newDomainMembership.findElement(NodesMembership.class);
        WorkerToCoreMembership oldMappingMembership = (WorkerToCoreMembership)oldMembership;
        WorkerToCoreMembershipDelta mappingDelta = (WorkerToCoreMembershipDelta)delta;
        WorkerToCoreMembership mappingMembership = newDomainMembership.findElement(WorkerToCoreMembership.class);
        
        Set<INode> failedCoreNodes = new LinkedHashSet<INode>();
        for (UUID id : mappingDelta.getFailedCoreNodes())
        {
            INode node = oldMappingMembership.findCoreNode(id);
            Assert.notNull(node);
            failedCoreNodes.add(node);
        }
        Set<INode> leftCoreNodes = new LinkedHashSet<INode>();
        for (UUID id : mappingDelta.getLeftCoreNodes())
        {
            INode node = oldMappingMembership.findCoreNode(id);
            Assert.notNull(node);
            leftCoreNodes.add(node);
        }
        
        Map<INode, INode> newCoreByWorkerMap = new LinkedHashMap<INode, INode>();
        for (Map.Entry<UUID, UUID> entry : mappingDelta.getNewCoreByWorkerMap().entrySet())
        {
            INode workerNode = nodeMembership.findNode(entry.getKey());
            Assert.notNull(workerNode);
            INode coreNode = mappingMembership.findCoreNode(entry.getValue());
            Assert.notNull(coreNode);
            
            newCoreByWorkerMap.put(workerNode, coreNode);
        }

        return new WorkerToCoreMembershipChange(mappingDelta.getJoinedCoreNodes(), leftCoreNodes, failedCoreNodes, newCoreByWorkerMap);
    }

    @Override
    public IClusterMembershipElementChange createChange(IDomainMembership newDomainMembership,
        IClusterMembershipElement newMembership, IClusterMembershipElement oldMembership)
    {
        WorkerToCoreMembership oldMappingMembership = (WorkerToCoreMembership)oldMembership;
        WorkerToCoreMembership newMappingMembership = (WorkerToCoreMembership)newMembership;
        List<INode> joinedCoreNodes = new ArrayList<INode>();
        Set<INode> leftCoreNodes = new LinkedHashSet<INode>();
        Set<INode> failedCoreNodes = new LinkedHashSet<INode>();
       
        for (INode node : newMappingMembership.getCoreNodes())
        {
            if (oldMappingMembership.findCoreNode(node.getId()) == null)
                joinedCoreNodes.add(node);
        }
        
        for (INode node : oldMappingMembership.getCoreNodes())
        {
            if (newMappingMembership.findCoreNode(node.getId()) == null)
                failedCoreNodes.add(node);
        }
        
        Map<INode, INode> newCoreByWorkerMap = new LinkedHashMap<INode, INode>();
        for (Map.Entry<INode, INode> entry : newMappingMembership.getCoreByWorkerMap().entrySet())
        {
            INode workerNode = entry.getKey();
            INode coreNode = entry.getValue();
            if (!coreNode.equals(oldMappingMembership.getCoreByWorkerMap().get(workerNode)))
                newCoreByWorkerMap.put(workerNode, coreNode);
        }
        
        return new WorkerToCoreMembershipChange(joinedCoreNodes, leftCoreNodes, failedCoreNodes, newCoreByWorkerMap);
    }
}
