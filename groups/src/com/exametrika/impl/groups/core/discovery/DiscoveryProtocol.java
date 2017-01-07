/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.discovery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipChange;
import com.exametrika.api.groups.core.IMembershipService;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;
import com.exametrika.impl.groups.core.membership.IPreparedMembershipListener;
import com.exametrika.spi.groups.IDiscoveryStrategy;

/**
 * The {@link DiscoveryProtocol} represents a protocol that discovers core group entry points and initiates group joining
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class DiscoveryProtocol extends AbstractProtocol implements INodeDiscoverer, IPreparedMembershipListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final int SENDS_COUNT_THRESHOLD = 3;
    private static final int FAILURES_COUNT_THRESHOLD = 3;
    private final IMembershipService membershipService;
    private final IDiscoveryStrategy discoveryStrategy;
    private final IFailureDetector failureDetector;
    private final ILiveNodeProvider liveNodeProvider;
    private final long discoveryPeriod;
    private final long discoveryCleanupPeriod;
    private final long groupFormationPeriod;
    private final IGroupJoinStrategy groupJoinStrategy;
    private IMembership membership;
    private long membershipId;
    private IAddress coordinator;
    private List<IAddress> healthyMembers = new ArrayList<IAddress>();
    private TreeMap<INode, DiscoveryInfo> discoveredNodes = new TreeMap<INode, DiscoveryInfo>();
    private long startTime;
    private boolean started;
    private long lastDiscoveryTime;
    private long lastDiscoveryCleanupTime;
    private long coordinatorAssignTime;
    private boolean stopped;

    public DiscoveryProtocol(String channelName, IMessageFactory messageFactory, IMembershipService membershipService, 
        IFailureDetector failureDetector, IDiscoveryStrategy discoveryStrategy, 
        ILiveNodeProvider liveNodeProvider, IGroupJoinStrategy groupJoinStrategy,
        long discoveryPeriod, long discoveryCleanupPeriod, long groupFormationPeriod)
    {
        super(channelName, messageFactory);

        Assert.notNull(membershipService);
        Assert.notNull(failureDetector);
        Assert.notNull(discoveryStrategy);
        Assert.notNull(liveNodeProvider);
        Assert.notNull(groupJoinStrategy);

        this.membershipService = membershipService;
        this.failureDetector = failureDetector;
        this.discoveryStrategy = discoveryStrategy;
        this.liveNodeProvider = liveNodeProvider;
        this.groupJoinStrategy = groupJoinStrategy;
        this.discoveryPeriod = discoveryPeriod;
        this.discoveryCleanupPeriod = discoveryCleanupPeriod;
        this.groupFormationPeriod = groupFormationPeriod;
    }

    @Override
    public void stop()
    {
        membership = null;
        coordinator = null;
        discoveredNodes.clear();
        stopped = true;

        super.stop();
    }

    @Override
    public void startDiscovery()
    {
        started = true;
        startTime = timeService.getCurrentTime();
    }
    
    @Override
    public boolean canFormGroup()
    {
        if (membership != null || coordinator != null || stopped)
            return false;

        if (startTime == 0 || timeService.getCurrentTime() < startTime + groupFormationPeriod)
            return false;

        if (!discoveredNodes.isEmpty() && discoveredNodes.firstKey().compareTo(membershipService.getLocalNode()) < 0)
            return false;

        return true;
    }

    @Override
    public Set<INode> getDiscoveredNodes()
    {
        return discoveredNodes.keySet();
    }

    @Override
    public void onPreparedMembershipChanged(IMembership oldMembership, IMembership newMembership, IMembershipChange membershipChange)
    {
        membership = newMembership;

        coordinator = null;
        healthyMembers.clear();
        if (oldMembership == null)
            discoveredNodes.clear();
        else
            discoveredNodes.keySet().removeAll(newMembership.getGroup().getMembers());
    }

    @Override
    public void onTimer(long currentTime)
    {
        if (!started)
            return;
        
        if (membership != null)
            return;
        
        if (coordinator == null)
        {
            if (currentTime > lastDiscoveryTime + discoveryPeriod)
            {
                Set<INode> nodes = new HashSet<INode>();
                for (Map.Entry<INode, DiscoveryInfo> entry : discoveredNodes.entrySet())
                {
                    if (entry.getValue().failuresCount == 0)
                        nodes.add(entry.getKey());
                }
                nodes.add(membershipService.getLocalNode());
                DiscoveryMessagePart discoveryPart = new DiscoveryMessagePart(nodes);
                
                Set<IAddress> addresses = new HashSet<IAddress>();
                for (String entryPoint : discoveryStrategy.getEntryPoints())
                {
                    entryPoint = connectionProvider.canonicalize(entryPoint);
                    connectionProvider.connect(entryPoint);
                    IAddress address = liveNodeProvider.findByConnection(entryPoint);
                    if (address != null)
                        addresses.add(address);
                }
        
                for (Map.Entry<INode, DiscoveryInfo> entry : discoveredNodes.entrySet())
                {
                    entry.getValue().sendsCount++;
                    addresses.add(entry.getKey().getAddress());
                }
                
                for (IAddress address : addresses)
                    send(messageFactory.create(address, discoveryPart, MessageFlags.HIGH_PRIORITY));
                
                lastDiscoveryTime = currentTime;
            }
            
            if (currentTime > lastDiscoveryCleanupTime + discoveryCleanupPeriod)
            {
                for (Iterator<Map.Entry<INode, DiscoveryInfo>> it = discoveredNodes.entrySet().iterator(); it.hasNext(); )
                {
                    Map.Entry<INode, DiscoveryInfo> entry = it.next();
                    INode node = entry.getKey();
                    DiscoveryInfo info = entry.getValue();
                    if (!liveNodeProvider.isLive(node.getAddress()))
                    {
                        if (info.sendsCount >= SENDS_COUNT_THRESHOLD)
                        {
                            info.failuresCount++;
                            if (info.failuresCount >= FAILURES_COUNT_THRESHOLD)
                            {
                                it.remove();
                                
                                if (logger.isLogEnabled(LogLevel.DEBUG))
                                    logger.log(LogLevel.DEBUG, marker, messages.nodeFailed(node));
                            }
                        }
                    }
                    else
                        info.failuresCount = 0;
                }
                
                lastDiscoveryCleanupTime = currentTime;
            }
        }
        else
        {
            if (currentTime > lastDiscoveryTime + discoveryPeriod)
            {
                DiscoveryMessagePart discoveryPart = new DiscoveryMessagePart(java.util.Collections.singleton(membershipService.getLocalNode()));
                
                Set<IAddress> addresses = new HashSet<IAddress>(healthyMembers);
                for (String entryPoint : discoveryStrategy.getEntryPoints())
                {
                    entryPoint = connectionProvider.canonicalize(entryPoint);
                    connectionProvider.connect(entryPoint);
                    IAddress address = liveNodeProvider.findByConnection(entryPoint);
                    if (address != null)
                        addresses.add(address);
                }
        
                for (IAddress address : addresses)
                    send(messageFactory.create(address, discoveryPart, MessageFlags.HIGH_PRIORITY));
                
                lastDiscoveryTime = currentTime;
            }
            
            if (currentTime > lastDiscoveryCleanupTime + discoveryCleanupPeriod)
            {
                if (currentTime > coordinatorAssignTime + discoveryCleanupPeriod * FAILURES_COUNT_THRESHOLD &&
                    !liveNodeProvider.isLive(coordinator))
                {
                    healthyMembers.remove(coordinator);
                    updateHealthyMembers(healthyMembers);
                }
                
                lastDiscoveryCleanupTime = currentTime;
            }
        }
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new DiscoveryMessagePartSerializer());
        registry.register(new MembershipResponseMessagePartSerializer());
        registry.register(new GroupJoinMessagePartSerializer());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(DiscoveryMessagePartSerializer.ID);
        registry.unregister(MembershipResponseMessagePartSerializer.ID);
        registry.unregister(GroupJoinMessagePartSerializer.ID);
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof DiscoveryMessagePart)
        {
            DiscoveryMessagePart part = message.getPart();

            for (INode node : part.getDiscoveredNodes())
            {
                if (membership == null)
                {
                    if (coordinator == null && !discoveredNodes.containsKey(node))
                    {
                        discoveredNodes.put(node, new DiscoveryInfo());
                        if (logger.isLogEnabled(LogLevel.DEBUG))
                            logger.log(LogLevel.DEBUG, marker, messages.nodeDiscovered(node));
                    }
                }
                else
                {
                    send(messageFactory.create(node.getAddress(), new MembershipResponseMessagePart(membership.getId(),
                        getNodeAddresses(failureDetector.getHealthyMembers()))));
                }
            }
        }
        else if (message.getPart() instanceof MembershipResponseMessagePart)
        {
            MembershipResponseMessagePart part = message.getPart();
            
            if (membership != null || membershipId > part.getMembershipId())
                return;

            membershipId = part.getMembershipId();
            updateHealthyMembers(new ArrayList<IAddress>(part.getHealthyMembers()));
        }
        else if (message.getPart() instanceof GroupJoinMessagePart)
        {
            GroupJoinMessagePart part = message.getPart();
            if (membership == null)
                return;
            
            if (membershipService.getLocalNode().equals(failureDetector.getCurrentCoordinator()))
            {
                if (!discoveredNodes.containsKey(part.getJoiningNode()))
                {
                    discoveredNodes.put(part.getJoiningNode(), new DiscoveryInfo());
                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, marker, messages.nodeDiscovered(part.getJoiningNode()));
                }
            }
            else
                send(messageFactory.create(failureDetector.getCurrentCoordinator().getAddress(), part));
        }
        else
            receiver.receive(message);
    }

    private void updateHealthyMembers(List<IAddress> healthyMembers)
    {
        IAddress oldCoordinator = coordinator;
        if (coordinator != null && (healthyMembers.isEmpty() || !coordinator.equals(healthyMembers.get(0))))
        {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.coordinatorFailed(coordinator));
            
            healthyMembers.remove(coordinator);
            coordinator = null;
        }
        
        this.healthyMembers = healthyMembers;
        discoveredNodes.clear();
        if (healthyMembers.isEmpty())
        {
            groupJoinStrategy.onGroupFailed();
            return;
        }
        else
            coordinator = healthyMembers.get(0);
        
        if (!coordinator.equals(oldCoordinator))
        {
            connectionProvider.connect(coordinator);
            groupJoinStrategy.onGroupDiscovered(healthyMembers);
            coordinatorAssignTime = timeService.getCurrentTime();
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.coordinatorDiscovered(coordinator));
        }
    }

    private List<IAddress> getNodeAddresses(List<INode> nodes)
    {
        List<IAddress> addresses = new ArrayList<IAddress>(nodes.size());
        for (INode node : nodes)
            addresses.add(node.getAddress());
            
        return addresses;
    }
    
    private static class DiscoveryInfo
    {
        int sendsCount;
        int failuresCount;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Node ''{0}'' has been discovered.")
        ILocalizedMessage nodeDiscovered(INode node);
        @DefaultMessage("Node ''{0}'' has been failed.")
        ILocalizedMessage nodeFailed(INode node);
        @DefaultMessage("Coordinator ''{0}'' has been discovered.")
        ILocalizedMessage coordinatorDiscovered(IAddress coordinator);
        @DefaultMessage("Coordinator ''{0}'' has been failed.")
        ILocalizedMessage coordinatorFailed(IAddress coordinator);
    }
}
