/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.ClusterMembershipEvent;
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
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.membership.ClusterMembership;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembership;

/**
 * The {@link CoreClusterFailureDetectionProtocol} represents a core node part of cluster failure detection protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreClusterFailureDetectionProtocol extends AbstractProtocol implements IClusterMembershipListener, 
    IFailureObserver, IClusterFailureDetector
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IClusterMembershipService membershipService;
    private IGroupFailureDetector failureDetector;
    private final Set<IFailureDetectionListener> failureDetectionListeners;
    private final long failureUpdatePeriod;
    private ISender bridgeSender;
    private Set<IAddress> workerNodes;
    private Map<UUID, INode> workerNodesMap;
    private Set<INode> failedNodes = new LinkedHashSet<INode>();
    private Set<INode> leftNodes = new LinkedHashSet<INode>();
    private INode currentCoordinator;
    private long lastFailureUpdateTime;
    private boolean modified;

    public CoreClusterFailureDetectionProtocol(String channelName, IMessageFactory messageFactory, IClusterMembershipService membershipService,
        Set<IFailureDetectionListener> failureDetectionListeners, long failureUpdatePeriod)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipService);
        Assert.notNull(failureDetectionListeners);
        
        this.membershipService = membershipService;
        this.failureDetectionListeners = failureDetectionListeners;
        this.failureUpdatePeriod = failureUpdatePeriod;
    }

    public void setFailureDetector(IGroupFailureDetector failureDetector)
    {
        Assert.notNull(failureDetector);
        Assert.isNull(this.failureDetector);
        
        this.failureDetector = failureDetector;
    }
    
    public void setBridgeSender(ISender bridgeSender)
    {
        Assert.notNull(bridgeSender);
        Assert.isNull(this.bridgeSender);
        
        this.bridgeSender = bridgeSender;
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
                bridgeSender.send(messageFactory.create(currentCoordinator.getAddress(), new FailureUpdateMessagePart(
                    buildNodeIds(failedNodes), buildNodeIds(leftNodes), false), MessageFlags.HIGH_PRIORITY | MessageFlags.PARALLEL));
                
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
        
        for (IAddress address : nodes)
        {
            INode node = workerNodesMap.get(address.getId());
            if (node != null && !failedNodes.contains(node) && !leftNodes.contains(node))
            {
                failedNodes.add(node);
                onNodeFailed(node);
                modified = true;
            }
        }
    }

    @Override
    public void onNodesLeft(Set<IAddress> nodes)
    {
        if (workerNodesMap == null)
            return;
        
        for (IAddress address : nodes)
        {
            INode node = workerNodesMap.get(address.getId());
            if (node != null && !leftNodes.contains(node))
            {
                leftNodes.add(node);
                failedNodes.remove(node);
                onNodeLeft(node);
                modified = true;
            }
        }
    }

    @Override
    public void onJoined()
    {
        ClusterMembership membership = (ClusterMembership)membershipService.getMembership();
        updateWorkerNodes(membership);
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
    }

    @Override
    public void onMembershipChanged(ClusterMembershipEvent event)
    {
        updateWorkerNodes((ClusterMembership)event.getNewMembership());
    }
    
    private void updateWorkerNodes(ClusterMembership membership)
    {
        IDomainMembership domainMembership = membership.getCoreDomain();
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
            failedNodes.retainAll(this.workerNodesMap.values());
        if (leftNodes != null)
            leftNodes.retainAll(this.workerNodesMap.values());
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.workerNodesChanged(workerNodes));
    }
    
    private Set<UUID> buildNodeIds(Set<INode> nodes)
    {
        Set<UUID> set = new HashSet<UUID>();
        for (INode node : nodes)
            set.add(node.getId());
        
        return set;
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

    @Override
    public Set<INode> getFailedNodes()
    {
        return failedNodes;
    }

    @Override
    public Set<INode> getLeftNodes()
    {
        return leftNodes;
    }
}
