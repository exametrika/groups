/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.GroupMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipElement;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.cluster.channel.IGracefulCloseStrategy;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.flush.IFlushManager;

/**
 * The {@link CoreCoordinatorClusterMembershipProtocol} represents a core coordinator part of cluster membership protocol.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreCoordinatorClusterMembershipProtocol extends AbstractClusterMembershipProtocol 
    implements IGracefulCloseStrategy, IGroupMembershipListener
{
    private final IGroupMembershipManager membershipManager;
    private final long trackPeriod;
    private IGroupFailureDetector failureDetector;
    private IFlushManager flushManager;
    private long lastTrackTime;
    private boolean stopped;
    private boolean installing;
    private long roundId;
    private Set<IAddress> respondingNodes;
    private boolean coreNodesFailed;
    
    public CoreCoordinatorClusterMembershipProtocol(String channelName, IMessageFactory messageFactory, 
        IClusterMembershipManager clusterMembershipManager, List<IClusterMembershipProvider> membershipProviders,
        IGroupMembershipManager membershipManager, long trackPeriod)
    {
        super(channelName, messageFactory, clusterMembershipManager, membershipProviders);
        
        Assert.notNull(membershipManager);
        
        this.membershipManager = membershipManager;
        this.trackPeriod = trackPeriod;
    }
    
    public void setFailureDetector(IGroupFailureDetector failureDetector)
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
        
        IClusterMembership oldMembership = clusterMembershipManager.getMembership();
        ClusterMembershipDelta delta;
        if (oldMembership == null || !coreNodesFailed)
        {
            delta = createCoreDelta(oldMembership, false);
            if (delta != null)
                roundId++;
        }
        else 
            delta = createCoreDelta(oldMembership, true);
        
        coreNodesFailed = false;
        if (delta == null)
            return;
        
        installing = true;
        respondingNodes = new HashSet<IAddress>();
        for (INode node : membershipManager.getMembership().getGroup().getMembers())
            respondingNodes.add(node.getAddress());
        
        send(messageFactory.create(GroupMemberships.CORE_GROUP_ADDRESS, new ClusterMembershipMessagePart(roundId, delta)));
    }

    @Override
    public void onJoined()
    {
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
    }

    @Override
    public void onMembershipChanged(GroupMembershipEvent event)
    {
        if ((!event.getMembershipChange().getGroup().getFailedMembers().isEmpty() || 
            !event.getMembershipChange().getGroup().getLeftMembers().isEmpty()) &&
            event.getNewMembership().getGroup().getCoordinator().equals(membershipManager.getLocalNode()))
        {
            respondingNodes = null;
            coreNodesFailed = true;
        }
    }
    
    @Override
    public void stop()
    {
        stopped = true;
        super.stop();
    }
    
    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof ClusterMembershipResponseMessagePart)
        {
            ClusterMembershipResponseMessagePart part = message.getPart();
            if (respondingNodes != null && part.getRoundId() == roundId && respondingNodes.contains(message.getSource()))
                respondingNodes.remove(message.getSource());
        }
        else
            receiver.receive(message);
    }
    
    @Override
    protected void onInstalled(long roundId, IClusterMembership newMembership, ClusterMembershipDelta coreDelta)
    {
        installing = false;
    }
    
    private ClusterMembershipDelta createCoreDelta(IClusterMembership oldMembership, boolean coreGroupOnly)
    {
        Set<String> domainNames = new LinkedHashSet<String>();
        if (!coreGroupOnly)
        {
            if (oldMembership != null)
            {
                for (IDomainMembership domainMembership : oldMembership.getDomains())
                    domainNames.add(domainMembership.getName());
            }
            
            for (int i = 0; i < membershipProviders.size(); i++)
                domainNames.addAll(membershipProviders.get(i).getDomains());
        }
        else
            domainNames.add(GroupMemberships.CORE_DOMAIN);
        
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
                IClusterMembershipElementDelta delta = null;
                if (!coreGroupOnly || membershipProviders.get(i).isCoreGroupOnly())
                {
                    Pair<IClusterMembershipElement, IClusterMembershipElementDelta> pair = membershipProviders.get(i).getDelta(
                        newDomainMembership, domainMembershipDelta, oldDomainMembership, oldDomainMembership != null ? oldDomainMembership.getElements().get(i) : null);
                    
                    elements.add(pair.getKey());
                    
                    if (pair.getValue() != null)
                    {
                        delta = pair.getValue();
                        empty = false;
                    }
                }
                
                if (delta != null)
                    deltas.add(delta);
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
            delta = new ClusterMembershipDelta(oldMembership.getId() + (coreGroupOnly ? 0 : 1), false, domainDeltas);
        return delta;
    }
    
    private boolean canInstallMembership(long currentTime)
    {
        if (stopped)
            return false;
        
        if (installing)
            return false;
        
        if (flushManager.isFlushInProgress())
            return false;

        if (lastTrackTime != 0 && currentTime < lastTrackTime + trackPeriod)
            return false;

        lastTrackTime = currentTime;

        IGroupMembership membership = membershipManager.getMembership();
        if (membership == null || !membership.getGroup().getCoordinator().equals(membershipManager.getLocalNode()))
            return false;
        
        if (!failureDetector.getFailedMembers().isEmpty() || !failureDetector.getLeftMembers().isEmpty())
            return false;
        
        if (!Collections.isEmpty(respondingNodes))
            return false;
        
        return true;
    }
}
