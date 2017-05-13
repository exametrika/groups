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
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.cluster.channel.IGracefulExitStrategy;
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
    implements IGracefulExitStrategy, IGroupMembershipListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
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
        List<ICoreClusterMembershipProvider> coreMembershipProviders, IGroupMembershipManager membershipManager, long trackPeriod)
    {
        super(channelName, messageFactory, clusterMembershipManager, membershipProviders, coreMembershipProviders, true);
        
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
    public boolean requestExit()
    {
        stopped = true;
        return true;
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (!canInstallMembership(currentTime))
            return;
        
        ClusterMembership oldMembership = (ClusterMembership)clusterMembershipManager.getMembership();
        ClusterMembershipDelta delta;
        if (oldMembership == null || !coreNodesFailed)
        {
            delta = createDelta(oldMembership);
            if (delta != null)
                roundId++;
        }
        else 
            delta = createCoreDelta(oldMembership);
        
        coreNodesFailed = false;
        if (delta == null)
            return;
        
        installing = true;
        respondingNodes = new HashSet<IAddress>();
        for (INode node : membershipManager.getMembership().getGroup().getMembers())
            respondingNodes.add(node.getAddress());
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.startsInstallMembership(roundId, delta, respondingNodes));
        
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
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.membershipChanged());
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
            {
                respondingNodes.remove(message.getSource());
                if (respondingNodes.isEmpty())
                    respondingNodes = null;
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.nodeResponded(roundId, message.getSource()));
            }
        }
        else
            receiver.receive(message);
    }
    
    @Override
    protected void onInstalled(long roundId, IClusterMembership newMembership, ClusterMembershipDelta coreDelta)
    {
        installing = false;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.installCompleted(roundId));
    }
    
    private ClusterMembershipDelta createDelta(ClusterMembership oldClusterMembership)
    {
        Set<String> domainNames = new LinkedHashSet<String>();
        if (oldClusterMembership != null)
        {
            for (IDomainMembership domainMembership : oldClusterMembership.getDomains())
                domainNames.add(domainMembership.getName());
        }
        
        for (int i = 0; i < membershipProviders.size(); i++)
            domainNames.addAll(membershipProviders.get(i).getDomains());
        
        long membershipId;
        if (oldClusterMembership != null)
            membershipId = oldClusterMembership.getId() + 1;
        else
            membershipId = 1;
        
        List<IDomainMembership> domains = new ArrayList<IDomainMembership>();
        List<IDomainMembershipDelta> domainDeltas = new ArrayList<IDomainMembershipDelta>();
        for (String domainName : domainNames)
        {
            IDomainMembership oldDomainMembership = null;
            if (oldClusterMembership != null)
                oldDomainMembership = oldClusterMembership.findDomain(domainName);
            
            List<IClusterMembershipElement> elements = new ArrayList<IClusterMembershipElement>();
            IDomainMembership newDomainMembership = new DomainMembership(domainName, elements);
            domains.add(newDomainMembership);
            
            List<IClusterMembershipElementDelta> deltas = new ArrayList<IClusterMembershipElementDelta>();
            DomainMembershipDelta domainMembershipDelta = new DomainMembershipDelta(domainName, deltas);
            
            boolean empty = true;
            for (int i = 0; i < membershipProviders.size(); i++)
            {
                Pair<IClusterMembershipElement, IClusterMembershipElementDelta> pair = membershipProviders.get(i).getDelta(
                    membershipId, newDomainMembership, domainMembershipDelta, oldDomainMembership,
                    oldDomainMembership != null ? oldDomainMembership.getElements().get(i) : null);
                
                elements.add(pair.getKey());
                
                IClusterMembershipElementDelta delta = null;
                if (pair.getValue() != null)
                {
                    delta = pair.getValue();
                    empty = false;
                }
                
                if (delta != null)
                    deltas.add(delta);
                else
                    deltas.add(membershipProviders.get(i).createEmptyDelta());
            }
            
            if (!empty)
                domainDeltas.add(domainMembershipDelta);
        }
        
        List<IClusterMembershipElement> coreElements = new ArrayList<IClusterMembershipElement>();
        IDomainMembership coreDomain = new DomainMembership(GroupMemberships.CORE_DOMAIN, coreElements);
        
        List<IClusterMembershipElementDelta> coreDeltas = new ArrayList<IClusterMembershipElementDelta>();
        DomainMembershipDelta coreDomainDelta = new DomainMembershipDelta(GroupMemberships.CORE_DOMAIN, coreDeltas);
       
        ClusterMembership newClusterMembership = new ClusterMembership(membershipId, domains, coreDomain);
        ClusterMembershipDelta clusterMembershipDelta = new ClusterMembershipDelta(membershipId, oldClusterMembership == null,
                domainDeltas, coreDomainDelta);
        
        boolean coreDomainEmpty = !buildCoreDomainDelta(membershipId, coreElements, coreDeltas, newClusterMembership, clusterMembershipDelta,
            oldClusterMembership);
        
        for (IClusterMembershipProvider provider : membershipProviders)
            provider.clearState();
        
        if (domainDeltas.isEmpty() && coreDomainEmpty)
            return null;
        else
            return clusterMembershipDelta;
    }
    
    private ClusterMembershipDelta createCoreDelta(ClusterMembership oldClusterMembership)
    {
        Assert.notNull(oldClusterMembership);
        
        long membershipId = oldClusterMembership.getId();
        
        List<IClusterMembershipElement> elements = new ArrayList<IClusterMembershipElement>();
        IDomainMembership coreDomain = new DomainMembership(GroupMemberships.CORE_DOMAIN, elements);
        
        List<IClusterMembershipElementDelta> deltas = new ArrayList<IClusterMembershipElementDelta>();
        DomainMembershipDelta coreDomainDelta = new DomainMembershipDelta(GroupMemberships.CORE_DOMAIN, deltas);
        
        ClusterMembership newClusterMembership = new ClusterMembership(membershipId, oldClusterMembership.getDomains(), coreDomain);
        ClusterMembershipDelta clusterMembershipDelta = new ClusterMembershipDelta(membershipId, false, 
            java.util.Collections.<IDomainMembershipDelta>emptyList(), coreDomainDelta);
        
        if (!buildCoreDomainDelta(membershipId, elements, deltas, newClusterMembership, clusterMembershipDelta,
            oldClusterMembership))
            return null;
        else
            return clusterMembershipDelta;
    }

    private boolean buildCoreDomainDelta(long membershipId, List<IClusterMembershipElement> elements,
        List<IClusterMembershipElementDelta> deltas, ClusterMembership newClusterMembership,
        ClusterMembershipDelta clusterMembershipDelta, ClusterMembership oldClusterMembership)
    {
        boolean empty = true;
        for (int i = 0; i < coreMembershipProviders.size(); i++)
        {
            IClusterMembershipElementDelta delta = null;
            Pair<IClusterMembershipElement, IClusterMembershipElementDelta> pair = coreMembershipProviders.get(i).getDelta(
                membershipId, newClusterMembership, clusterMembershipDelta, oldClusterMembership,
                oldClusterMembership != null ? oldClusterMembership.getCoreDomain().getElements().get(i) : null);
                
            elements.add(pair.getKey());
                
            if (pair.getValue() != null)
            {
                delta = pair.getValue();
                empty = false;
            }
        
            if (delta != null)
                deltas.add(delta);
            else
                deltas.add(coreMembershipProviders.get(i).createEmptyDelta());
        }
        
        return !empty;
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
    
    private interface IMessages
    {
        @DefaultMessage("Starting cluster membership install. Round-id: {0}\ndelta: {1}\nResponding nodes: {2}")
        ILocalizedMessage startsInstallMembership(long roundId, ClusterMembershipDelta delta,
            Set<IAddress> respondingNodes);
        @DefaultMessage("Cluster membership has been changed, clear responding nodes.")
        ILocalizedMessage membershipChanged();
        @DefaultMessage("Node ''{1}'' has responded, round-id: {0}.")
        ILocalizedMessage nodeResponded(long roundId, IAddress source);
        @DefaultMessage("Cluster membership install has been completed, round-id: {0}.")
        ILocalizedMessage installCompleted(long roundId);
    }
}
