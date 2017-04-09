/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.ClusterMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembership;

/**
 * The {@link CoreClusterFailureDetectionProtocol} represents a core node part of cluster failure detection protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreClusterFailureDetectionProtocol extends AbstractProtocol implements IClusterMembershipListener, 
    IFailureObserver
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IClusterMembershipService membershipService;
    private final IGroupFailureDetector failureDetector;
    private final Set<IFailureDetectionListener> failureDetectionListeners;
    private final long failureUpdatePeriod;
    private Set<IAddress> workerNodes;
    private Map<UUID, INode> workerNodesMap;
    private Set<UUID> failedNodes = new LinkedHashSet<UUID>();
    private Set<UUID> leftNodes = new LinkedHashSet<UUID>();
    private INode currentCoordinator;
    private long lastFailureUpdateTime;
    private boolean modified;

    public CoreClusterFailureDetectionProtocol(String channelName, IMessageFactory messageFactory, IClusterMembershipService membershipService,
        IGroupFailureDetector failureDetector, Set<IFailureDetectionListener> failureDetectionListeners, long failureUpdatePeriod)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipService);
        Assert.notNull(failureDetector);
        Assert.notNull(failureDetectionListeners);
        
        this.membershipService = membershipService;
        this.failureDetector = failureDetector;
        this.failureDetectionListeners = failureDetectionListeners;
        this.failureUpdatePeriod = failureUpdatePeriod;
    }

    public Set<IAddress> getWorkerNodes()
    {
        if (workerNodes != null)
            return workerNodes;
        else
            return java.util.Collections.emptySet();
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (currentTime >= lastFailureUpdateTime + failureUpdatePeriod)
        {
            INode currentCoordinator = failureDetector.getCurrentCoordinator();
            if (currentCoordinator == null)
                return;
            
            if ((!currentCoordinator.equals(this.currentCoordinator) || modified) && 
                (!failedNodes.isEmpty() || !leftNodes.isEmpty()))
            {
                send(messageFactory.create(currentCoordinator.getAddress(), new FailureUpdateMessagePart(
                    failedNodes, leftNodes), MessageFlags.HIGH_PRIORITY | MessageFlags.PARALLEL));
                
                lastFailureUpdateTime = timeService.getCurrentTime();
                modified = false;
                this.currentCoordinator = currentCoordinator;
            }
        }
    }
    
    @Override
    public void onNodesFailed(Set<IAddress> nodes)
    {
        if (workerNodesMap == null)
            return;
        
        for (IAddress node : nodes)
        {
            if (workerNodesMap.containsKey(node.getId()) && !failedNodes.contains(node.getId()) &&
                !leftNodes.contains(node.getId()))
            {
                failedNodes.add(node.getId());
                onNodeFailed(workerNodesMap.get(node.getId()));
                modified = true;
            }
        }
    }

    @Override
    public void onNodesLeft(Set<IAddress> nodes)
    {
        if (workerNodesMap == null)
            return;
        
        for (IAddress node : nodes)
        {
            if (workerNodesMap.containsKey(node.getId()) && !leftNodes.contains(node.getId()))
            {
                leftNodes.add(node.getId());
                failedNodes.remove(node.getId());
                onNodeLeft(workerNodesMap.get(node.getId()));
                modified = true;
            }
        }
    }

    @Override
    public void onJoined()
    {
        IClusterMembership membership = membershipService.getMembership();
        updateWorkerNodes(membership);
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
    }

    @Override
    public void onMembershipChanged(ClusterMembershipEvent event)
    {
        updateWorkerNodes(event.getNewMembership());
    }
    
    private void updateWorkerNodes(IClusterMembership membership)
    {
        IDomainMembership domainMembership = membership.findDomain(GroupMemberships.CORE_DOMAIN);
        WorkerToCoreMembership mappingMembership = domainMembership.findElement(WorkerToCoreMembership.class);
        Set<INode> workerNodes = mappingMembership.findWorkerNodes(membershipService.getLocalNode());
        this.workerNodes.clear();
        this.workerNodesMap.clear();
        if (workerNodes != null)
        {
            for (INode node : workerNodes)
            {
                this.workerNodes.add(node.getAddress());
                this.workerNodesMap.put(node.getId(), node);
            }
        }
        
        if (failedNodes != null)
            failedNodes.retainAll(this.workerNodesMap.keySet());
        if (leftNodes != null)
            leftNodes.retainAll(this.workerNodesMap.keySet());
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.workerNodesChanged(workerNodes));
    }
    
    private void onNodeFailed(INode node)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.nodeFailed(node));
        
        for (IFailureDetectionListener listener : failureDetectionListeners)
        {
            try
            {
                listener.onMemberFailed(node);
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception from other listeners
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, marker, e);
            }
        }
    }
    
    private void onNodeLeft(INode node)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.nodeLeft(node));
        
        for (IFailureDetectionListener listener : failureDetectionListeners)
        {
            try
            {
                listener.onMemberLeft(node);
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception from other listeners
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, marker, e);
            }
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Node ''{0}'' has been failed.")
        ILocalizedMessage nodeFailed(INode node);
        @DefaultMessage("Node ''{0}'' has been left.")
        ILocalizedMessage nodeLeft(INode node);
        @DefaultMessage("Worker nodes have been changed: {0}.")
        ILocalizedMessage workerNodesChanged(Set<INode> workerNodes);
    }
}
