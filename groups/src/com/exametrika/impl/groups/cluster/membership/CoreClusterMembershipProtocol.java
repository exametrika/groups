/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.failuredetection.IClusterFailureDetector;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.flush.IFlushManager;

/**
 * The {@link CoreClusterMembershipProtocol} represents a core node part of cluster membership protocol.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreClusterMembershipProtocol extends AbstractClusterMembershipProtocol implements IFailureDetectionListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IGroupMembershipManager membershipManager;
    private ISender workerSender;
    private final CoreCoordinatorClusterMembershipProtocol coordinatorProtocol;
    private final long membershipTimeout;
    private IFailureObserver failureObserver;
    private IClusterFailureDetector failureDetector;
    private IFlushManager flushManager;
    private Set<INode> workerNodes = Collections.emptySet();
    private long roundId;
    private Set<IAddress> respondingNodes;
    private long startInstallTime;
    
    public CoreClusterMembershipProtocol(String channelName, IMessageFactory messageFactory, 
        IClusterMembershipManager clusterMembershipManager, List<IClusterMembershipProvider> membershipProviders,
        List<ICoreClusterMembershipProvider> coreMembershipProviders, IGroupMembershipManager membershipManager, 
        CoreCoordinatorClusterMembershipProtocol coordinatorProtocol, long membershipTimeout)
    {
        super(channelName, messageFactory, clusterMembershipManager, membershipProviders, coreMembershipProviders, true);
        
        Assert.notNull(membershipManager);
        Assert.notNull(coordinatorProtocol);
        
        this.membershipManager = membershipManager;
        this.coordinatorProtocol = coordinatorProtocol;
        this.membershipTimeout = membershipTimeout;
    }
    
    public void setFailureDetector(IClusterFailureDetector failureDetector)
    {
        Assert.notNull(failureDetector);
        Assert.isNull(this.failureDetector);
        
        this.failureDetector = failureDetector;
    }
    
    public void setFailureObserver(IFailureObserver failureObserver)
    {
        Assert.notNull(failureObserver);
        Assert.isNull(this.failureObserver);
        
        this.failureObserver = failureObserver;
    }
    
    public void setFlushManager(IFlushManager flushManager)
    {
        Assert.notNull(flushManager);
        Assert.isNull(this.flushManager);
        
        this.flushManager = flushManager;
    }
    
    public void setWorkerSender(ISender workerSender)
    {
        Assert.notNull(workerSender);
        Assert.isNull(this.workerSender);
        
        this.workerSender = workerSender;
    }
    
    @Override
    public void onMemberFailed(INode member)
    {
        removeFromRespondingNodes(member.getAddress(), roundId);
    }

    @Override
    public void onMemberLeft(INode member)
    {
        onMemberFailed(member);
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (currentTime > startInstallTime + membershipTimeout && 
            !com.exametrika.common.utils.Collections.isEmpty(respondingNodes))
        {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.nodesFailed(respondingNodes));
            
            failureObserver.onNodesFailed(respondingNodes);
            respondingNodes = null;
            sendResponseToCoordinator();
        }
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof ClusterMembershipMessagePart)
        {
            ClusterMembershipMessagePart part = message.getPart();
            installMembership(part);
        }
        else if (message.getPart() instanceof ClusterMembershipResponseMessagePart)
        {
            ClusterMembershipResponseMessagePart part = message.getPart();
            removeFromRespondingNodes(message.getSource(), part.getRoundId());
        }
        else
            receiver.receive(message);
    }

    @Override
    protected void onInstalled(long roundId, IClusterMembership newMembership, ClusterMembershipDelta coreDelta)
    {
        coordinatorProtocol.onInstalled(roundId, newMembership, coreDelta);
        
        WorkerToCoreMembership mapping = ((ClusterMembership)newMembership).getCoreDomain().findElement(WorkerToCoreMembership.class);
        Assert.notNull(mapping);
        Set<INode> workerNodes = mapping.findWorkerNodes(membershipManager.getLocalNode());
        
        if (workerNodes == null || workerNodes.isEmpty())
        {
            this.workerNodes = Collections.emptySet();
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.workerNodesCleared(roundId));
            return;
        }
        
        this.roundId = roundId;
        startInstallTime = timeService.getCurrentTime();
        respondingNodes = new HashSet<IAddress>();
        for (INode workerNode : workerNodes)
        {
            if (failureDetector.getFailedNodes().contains(workerNode) || failureDetector.getLeftNodes().contains(workerNode))
                continue;
            
            respondingNodes.add(workerNode.getAddress());
            
            ClusterMembershipDelta delta;
            if (this.workerNodes.contains(workerNode))
                delta = createWorkerDelta(workerNode.getDomain(), coreDelta);
            else
                delta = createWorkerFullDelta(workerNode.getDomain(), newMembership);
            
            workerSender.send(messageFactory.create(workerNode.getAddress(), new ClusterMembershipMessagePart(roundId, delta)));
        }
        
        this.workerNodes = workerNodes;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.startInstallMembershipOnWorkers(roundId, workerNodes));
    }
    
    private ClusterMembershipDelta createWorkerFullDelta(String domain, IClusterMembership newMembership)
    {
        List<IDomainMembershipDelta> domains = new ArrayList<IDomainMembershipDelta>();
        for (IDomainMembership coreDomain : newMembership.getDomains())
        {
            boolean publicPart = !coreDomain.getName().equals(domain);
            
            List<IClusterMembershipElementDelta> deltas = new ArrayList<IClusterMembershipElementDelta>();
            for (int i = 0; i < membershipProviders.size(); i++)
                deltas.add(membershipProviders.get(i).createWorkerDelta(coreDomain.getElements().get(i), null, true, publicPart));
            
            domains.add(new DomainMembershipDelta(coreDomain.getName(), deltas));
        }
        return new ClusterMembershipDelta(newMembership.getId(), true, domains, null);
    }

    private ClusterMembershipDelta createWorkerDelta(String domain, ClusterMembershipDelta coreDelta)
    {
        List<IDomainMembershipDelta> domains = new ArrayList<IDomainMembershipDelta>();
        for (IDomainMembershipDelta coreDomainDelta : coreDelta.getDomains())
        {
            if (coreDomainDelta.getName().equals(domain))
                domains.add(coreDomainDelta);
            else
            {
                List<IClusterMembershipElementDelta> deltas = new ArrayList<IClusterMembershipElementDelta>();
                for (int i = 0; i < membershipProviders.size(); i++)
                    deltas.add(membershipProviders.get(i).createWorkerDelta(null, coreDomainDelta.getDeltas().get(i),
                        false, true));
                
                domains.add(new DomainMembershipDelta(coreDomainDelta.getName(), deltas));
            }
        }
        return new ClusterMembershipDelta(coreDelta.getId(), coreDelta.isFull(), domains, null);
    }
    
    private void removeFromRespondingNodes(IAddress node, long roundId)
    {
        if (respondingNodes != null && roundId == this.roundId && respondingNodes.contains(node))
        {
            respondingNodes.remove(node);
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.nodeResponded(roundId, node));
        }
        if (com.exametrika.common.utils.Collections.isEmpty(respondingNodes))
            sendResponseToCoordinator();
    }

    private void sendResponseToCoordinator()
    {
        IGroupMembership membership = membershipManager.getMembership();
        if (membership == null)
            return;
        INode coordinator = membership.getGroup().getCoordinator();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.respondToCoordinator(roundId, coordinator));
        
        send(messageFactory.create(coordinator.getAddress(), new ClusterMembershipResponseMessagePart(roundId)));
    }
    
    private interface IMessages
    {
        @DefaultMessage("Worker nodes ''{0}'' have been failed.")
        ILocalizedMessage nodesFailed(Set<IAddress> respondingNodes);
        @DefaultMessage("Worker nodes are cleared, round-id: {0}.")
        ILocalizedMessage workerNodesCleared(long roundId);
        @DefaultMessage("Starting installation of cluster membership on worker nodes ''{1}'', round-id: {0}.")
        ILocalizedMessage startInstallMembershipOnWorkers(long roundId, Set<INode> workerNodes);
        @DefaultMessage("Node ''{1}'' has responded, round-id: {0}.")
        ILocalizedMessage nodeResponded(long roundId, IAddress node);
        @DefaultMessage("Completion of cluster membership installation on worker nodes has been sent to coordinator ''{1}'', round-id: {0}.")
        ILocalizedMessage respondToCoordinator(long roundId, INode coordinator);
    }
}
