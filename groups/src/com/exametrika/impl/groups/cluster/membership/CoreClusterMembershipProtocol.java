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
import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;
import com.exametrika.impl.groups.core.flush.IFlushManager;
import com.exametrika.impl.groups.core.membership.IMembershipManager;
import com.exametrika.impl.groups.core.membership.Memberships;

/**
 * The {@link CoreClusterMembershipProtocol} represents a core node part of cluster membership protocol.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreClusterMembershipProtocol extends AbstractClusterMembershipProtocol implements IFailureDetectionListener
{
    private final IMembershipManager membershipManager;
    private final ISender workerSender;
    private final CoreCoordinatorClusterMembershipProtocol coordinatorProtocol;
    private final long membershipTimeout;
    private final IFailureObserver failureObserver;
    private IFailureDetector failureDetector;
    private IFlushManager flushManager;
    private Set<INode> workerNodes = Collections.emptySet();
    private long roundId;
    private Set<IAddress> respondingNodes;
    private long startInstallTime;
    
    public CoreClusterMembershipProtocol(String channelName, IMessageFactory messageFactory, 
        IClusterMembershipManager clusterMembershipManager, List<IClusterMembershipProvider> membershipProviders,
        IMembershipManager membershipManager, ISender workerSender,
        CoreCoordinatorClusterMembershipProtocol coordinatorProtocol, IFailureObserver failureObserver,
        long membershipTimeout)
    {
        super(channelName, messageFactory, clusterMembershipManager, membershipProviders);
        
        Assert.notNull(membershipManager);
        Assert.notNull(workerSender);
        Assert.notNull(coordinatorProtocol);
        Assert.notNull(failureObserver);
        
        this.membershipManager = membershipManager;
        this.workerSender = workerSender;
        this.coordinatorProtocol = coordinatorProtocol;
        this.membershipTimeout = membershipTimeout;
        this.failureObserver = failureObserver;
    }
    
    public void setFailureDetector(IFailureDetector failureDetector)
    {
        Assert.notNull(failureDetector);
        Assert.isNull(this.failureDetector);
        
        this.failureDetector = failureDetector;
    }
    
    public void setFlushManager(IFlushManager flushManager)
    {
        Assert.notNull(flushManager);
        Assert.isNull(this.flushManager);
        
        this.flushManager = flushManager;
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
        
        WorkerToCoreMembership mapping = newMembership.findDomain(Memberships.CORE_DOMAIN).findElement(WorkerToCoreMembership.class);
        Assert.notNull(mapping);
        Set<INode> workerNodes = mapping.findWorkerNodes(membershipManager.getLocalNode());
        if (workerNodes == null || workerNodes.isEmpty())
        {
            this.workerNodes = Collections.emptySet();
            return;
        }
        
        this.roundId = roundId;
        startInstallTime = timeService.getCurrentTime();
        respondingNodes = new HashSet<IAddress>();
        for (INode workerNode : workerNodes)
        {
            respondingNodes.add(workerNode.getAddress());
            
            ClusterMembershipDelta delta;
            if (this.workerNodes.contains(workerNode))
                delta = createWorkerDelta(workerNode.getDomain(), coreDelta);
            else
                delta = createWorkerFullDelta(workerNode.getDomain(), newMembership);
            
            workerSender.send(messageFactory.create(workerNode.getAddress(), new ClusterMembershipMessagePart(roundId, delta)));
        }
        
        this.workerNodes = workerNodes;
    }
    
    private ClusterMembershipDelta createWorkerFullDelta(String domain, IClusterMembership newMembership)
    {
        List<IDomainMembershipDelta> domains = new ArrayList<IDomainMembershipDelta>();
        for (IDomainMembership coreDomain : newMembership.getDomains())
        {
            if (coreDomain.getName().equals(Memberships.CORE_DOMAIN))
                continue;
            
            boolean publicPart = !coreDomain.getName().equals(domain);
            
            List<IClusterMembershipElementDelta> deltas = new ArrayList<IClusterMembershipElementDelta>();
            for (int i = 0; i < membershipProviders.size(); i++)
                deltas.add(membershipProviders.get(i).createWorkerDelta(coreDomain.getElements().get(i), null, true, publicPart));
            
            domains.add(new DomainMembershipDelta(coreDomain.getName(), deltas));
        }
        return new ClusterMembershipDelta(newMembership.getId(), true, domains);
    }

    private ClusterMembershipDelta createWorkerDelta(String domain, ClusterMembershipDelta coreDelta)
    {
        List<IDomainMembershipDelta> domains = new ArrayList<IDomainMembershipDelta>();
        for (IDomainMembershipDelta coreDomainDelta : coreDelta.getDomains())
        {
            if (coreDomainDelta.getName().equals(Memberships.CORE_DOMAIN))
                continue;
            
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
        return new ClusterMembershipDelta(coreDelta.getId(), coreDelta.isFull(), domains);
    }
    
    private void removeFromRespondingNodes(IAddress node, long roundId)
    {
        if (respondingNodes != null && roundId == this.roundId && respondingNodes.contains(node))
            respondingNodes.remove(node);
        if (com.exametrika.common.utils.Collections.isEmpty(respondingNodes))
            sendResponseToCoordinator();
    }

    private void sendResponseToCoordinator()
    {
        IMembership membership = membershipManager.getMembership();
        if (membership == null)
            return;
        INode coordinator = membership.getGroup().getCoordinator();
        send(messageFactory.create(coordinator.getAddress(), new ClusterMembershipResponseMessagePart(roundId)));
    }
}
