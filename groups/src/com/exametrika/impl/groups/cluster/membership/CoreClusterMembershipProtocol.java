/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.utils.Assert;
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
        
        installing = true;
        
        ClusterMembershipDelta delta = createCoreDelta();
        send(messageFactory.create(Memberships.CORE_GROUP_ADDRESS, new ClusterMembershipMessagePart(delta)));
    }

    @Override
    protected void onInstalled(IClusterMembership newMembership, ClusterMembershipDelta coreDelta)
    {
        installing = false;
        WorkerToCoreMembership mapping = newMembership.findDomain(Memberships.CORE_DOMAIN).getElement(WorkerToCoreMembership.class);
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
        Map<String, List<IClusterMembershipElementDelta>> domainDeltas = new LinkedHashMap<String, List<IClusterMembershipElementDelta>>();
        for (int i= 0; i < membershipProviders.size(); i++)
        {
            Map<String, IClusterMembershipElement> elements = null;
            elements = new LinkedHashMap<String, IClusterMembershipElement>();
            if (oldMembership != null)
            {
                for (IDomainMembership domain : oldMembership.getDomains())
                    elements.put(domain.getName(), domain.getElements().get(i));
            }
            
            Map<String, IClusterMembershipElementDelta> deltas = membershipProviders.get(i).getDeltas(elements);
            
            for (Map.Entry<String, IClusterMembershipElementDelta> entry : deltas.entrySet())
            {
                List<IClusterMembershipElementDelta> list = domainDeltas.get(entry.getKey());
                if (list == null)
                {
                    list = new ArrayList<IClusterMembershipElementDelta>();
                    domainDeltas.put(entry.getKey(), list);
                }
                com.exametrika.common.utils.Collections.set((ArrayList)list, i, entry.getValue());
            }
        }
        
        List<IDomainMembershipDelta> domains = new ArrayList<IDomainMembershipDelta>();
        for (Map.Entry<String, List<IClusterMembershipElementDelta>> entry : domainDeltas.entrySet())
        {
            List<IClusterMembershipElementDelta> list = entry.getValue();
            for (int i = 0; i < membershipProviders.size(); i++)
            {
                if (com.exametrika.common.utils.Collections.get(list, i) == null)
                    com.exametrika.common.utils.Collections.set((ArrayList)list, i, membershipProviders.get(i).createEmptyDelta());
            }
            
            domains.add(new DomainMembershipDelta(entry.getKey(), list));
        }
        
        ClusterMembershipDelta delta;
        if (oldMembership == null)
            delta = new ClusterMembershipDelta(1, true, domains);
        else
            delta = new ClusterMembershipDelta(oldMembership.getId(), false, domains);
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
        
        for (IClusterMembershipProvider provider : membershipProviders)
        {
            if (provider.hasChanges())
                return true;
        }
        
        return false;
    }
}
