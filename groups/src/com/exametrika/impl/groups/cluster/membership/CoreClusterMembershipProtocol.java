/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.core.channel.IGracefulCloseStrategy;
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
public final class CoreClusterMembershipProtocol extends AbstractClusterMembershipProtocol implements IGracefulCloseStrategy
{
    private final IMembershipManager membershipManager;
    private final ISender workerSender;
    private final long trackPeriod;
    private IFailureDetector failureDetector;
    private IFlushManager flushManager;
    private long lastTrackTime;
    private boolean stopped;
    private boolean installing;
    private Set<INode> workerNodes = Collections.emptySet();
    
    public CoreClusterMembershipProtocol(String channelName, IMessageFactory messageFactory, 
        IClusterMembershipManager clusterMembershipManager, List<IClusterMembershipProvider> membershipProviders,
        IMembershipManager membershipManager, ISender workerSender, long trackPeriod)
    {
        super(channelName, messageFactory, clusterMembershipManager, membershipProviders);
        
        Assert.notNull(membershipManager);
        Assert.notNull(workerSender);
        
        this.membershipManager = membershipManager;
        this.workerSender = workerSender;
        this.trackPeriod = trackPeriod;
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
    public boolean requestClose()
    {
        stopped = true;
        return true;
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (!canInstallMembership(currentTime))
            return;
        
        ClusterMembershipDelta delta = createCoreDelta();
        if (delta == null)
            return;
        
        installing = true;
        send(messageFactory.create(Memberships.CORE_GROUP_ADDRESS, new ClusterMembershipMessagePart(delta)));
    }

    @Override
    protected void onInstalled(IClusterMembership newMembership, ClusterMembershipDelta coreDelta)
    {
        installing = false;
        WorkerToCoreMembership mapping = newMembership.findDomain(Memberships.CORE_DOMAIN).findElement(WorkerToCoreMembership.class);
        Assert.notNull(mapping);
        Set<INode> workerNodes = mapping.findWorkerNodes(membershipManager.getLocalNode());
        if (workerNodes == null || workerNodes.isEmpty())
        {
            this.workerNodes = Collections.emptySet();
            return;
        }
        
        for (INode workerNode : workerNodes)
        {
            ClusterMembershipDelta delta;
            if (this.workerNodes.contains(workerNode))
                delta = createWorkerDelta(workerNode.getDomain(), coreDelta);
            else
                delta = createWorkerFullDelta(workerNode.getDomain(), newMembership);
            
            workerSender.send(messageFactory.create(workerNode.getAddress(), new ClusterMembershipMessagePart(delta)));
        }
        
        this.workerNodes = workerNodes;
    }
    
    private ClusterMembershipDelta createCoreDelta()
    {
        IClusterMembership oldMembership = clusterMembershipManager.getMembership();
        
        Set<String> domainNames = new LinkedHashSet<String>();
        if (oldMembership != null)
        {
            for (IDomainMembership domainMembership : oldMembership.getDomains())
                domainNames.add(domainMembership.getName());
        }
        
        for (int i = 0; i < membershipProviders.size(); i++)
            domainNames.addAll(membershipProviders.get(i).getDomains());
        
        List<IDomainMembershipDelta> domainDeltas = new ArrayList<IDomainMembershipDelta>();
        for (String domainName : domainNames)
        {
            IDomainMembership oldDomainMembership = null;
            if (oldMembership != null)
                oldDomainMembership = oldMembership.findDomain(domainName);
            
            List<IClusterMembershipElement> elements = new ArrayList<IClusterMembershipElement>();
            IDomainMembership newDomainMembership = new DomainMembership(domainName, elements);
            
            boolean empty = true;
            List<IClusterMembershipElementDelta> deltas = new ArrayList<IClusterMembershipElementDelta>();
            DomainMembershipDelta domainMembershipDelta = new DomainMembershipDelta(domainName, deltas);
            for (int i = 0; i < membershipProviders.size(); i++)
            {
                Pair<IClusterMembershipElement, IClusterMembershipElementDelta> pair = membershipProviders.get(i).getDelta(
                    newDomainMembership, domainMembershipDelta, oldDomainMembership, oldDomainMembership != null ? oldDomainMembership.getElements().get(i) : null);
                
                elements.add(pair.getKey());
                
                if (pair.getValue() != null)
                {
                    deltas.add(pair.getValue());
                    empty = false;
                }
                else
                    deltas.add(membershipProviders.get(i).createEmptyDelta());
            }
            
            if (!empty)
                domainDeltas.add(domainMembershipDelta);
        }
        
        if (domainDeltas.isEmpty())
            return null;
        
        ClusterMembershipDelta delta;
        if (oldMembership == null)
            delta = new ClusterMembershipDelta(1, true, domainDeltas);
        else
            delta = new ClusterMembershipDelta(oldMembership.getId(), false, domainDeltas);
        return delta;
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

    private boolean canInstallMembership(long currentTime)
    {
        if (stopped)
            return false;
        
        if (installing)
            return false;
        
        if (lastTrackTime != 0 && currentTime < lastTrackTime + trackPeriod)
            return false;

        lastTrackTime = currentTime;

        if (flushManager.isFlushInProgress())
            return false;

        IMembership membership = membershipManager.getMembership();
        if (membership == null || !membership.getGroup().getCoordinator().equals(membershipManager.getLocalNode()))
            return false;
        
        if (!failureDetector.getFailedMembers().isEmpty() || !failureDetector.getLeftMembers().isEmpty())
            return false;
        
        return true;
    }
}
