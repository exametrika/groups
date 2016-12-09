/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.discovery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
    private final IMembershipService membershipService;
    private final IDiscoveryStrategy discoveryStrategy;
    private final IFailureDetector failureDetector;
    private final ILiveNodeProvider liveNodeProvider;
    private final long discoveryPeriod;
    private final long groupFormationPeriod;
    private final IGroupJoinStrategy groupJoinStrategy;
    private IMembership membership;
    private long membershipId;
    private IAddress coordinator;
    private List<IAddress> healthyMembers = new ArrayList<IAddress>();
    private TreeSet<INode> discoveredNodes = new TreeSet<INode>();
    private long startTime;
    private boolean started;
    private long lastDiscoveryTime;

    public DiscoveryProtocol(String channelName, IMessageFactory messageFactory, IMembershipService membershipService, 
        IFailureDetector failureDetector, IDiscoveryStrategy discoveryStrategy, 
        ILiveNodeProvider liveNodeProvider, IGroupJoinStrategy groupJoinStrategy,
        long discoveryPeriod, long groupFormationPeriod)
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
        this.groupFormationPeriod = groupFormationPeriod;
    }

    @Override
    public void stop()
    {
        membership = null;
        coordinator = null;
        discoveredNodes.clear();

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
        if (membership != null || coordinator != null)
            return false;

        if (startTime == 0 || timeService.getCurrentTime() < startTime + groupFormationPeriod)
            return false;

        if (!discoveredNodes.isEmpty() && discoveredNodes.first().compareTo(membershipService.getLocalNode()) < 0)
            return false;

        return true;
    }

    @Override
    public Set<INode> getDiscoveredNodes()
    {
        return discoveredNodes;
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
            discoveredNodes.removeAll(newMembership.getGroup().getMembers());
    }

    @Override
    public void onTimer(long currentTime)
    {
        if (!started)
            return;
        
        if (membership != null || coordinator != null || timeService.getCurrentTime() < lastDiscoveryTime + discoveryPeriod)
            return;
        
        Set<INode> nodes = new HashSet<INode>(discoveredNodes);
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

        for (INode node : discoveredNodes)
            addresses.add(node.getAddress());
        
        for (IAddress address : addresses)
            send(messageFactory.create(address, discoveryPart, MessageFlags.HIGH_PRIORITY));
        
        lastDiscoveryTime = timeService.getCurrentTime();
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
    public void cleanup(ILiveNodeProvider liveNodeProvider, long currentTime)
    {
        if (coordinator != null && !liveNodeProvider.isLive(coordinator))
            updateHealthyMembers(healthyMembers);
        
        for (Iterator<INode> it = discoveredNodes.iterator(); it.hasNext(); )
        {
            INode node = it.next();
            if (!liveNodeProvider.isLive(node.getAddress()))
            {
                it.remove();
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.nodeFailed(node));
            }
        }
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
                    if (discoveredNodes.add(node) && logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, marker, messages.nodeDiscovered(node));
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
            
            if (membership != null || membershipId >= part.getMembershipId())
                return;

            membershipId = part.getMembershipId();
            updateHealthyMembers(new ArrayList<IAddress>(part.getHealthyMembers()));

            discoveredNodes.clear();
        }
        else if (message.getPart() instanceof GroupJoinMessagePart)
        {
            GroupJoinMessagePart part = message.getPart();
            if (membership == null)
                return;
            
            if (membershipService.getLocalNode().equals(failureDetector.getCurrentCoordinator()))
            {
                if (discoveredNodes.add(part.getJoiningNode()) && logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.nodeDiscovered(part.getJoiningNode()));
            }
            else
                send(messageFactory.create(failureDetector.getCurrentCoordinator().getAddress(), part));
        }
        else
            receiver.receive(message);
    }

    private void updateHealthyMembers(List<IAddress> healthyMembers)
    {
        if (coordinator != null && !coordinator.equals(healthyMembers.get(0)))
        {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.coordinatorFailed(coordinator));
            
            healthyMembers.remove(coordinator);
            coordinator = null;
        }
        
        this.healthyMembers = healthyMembers;
        if (healthyMembers.isEmpty())
        {
            groupJoinStrategy.onGroupFailed();
            return;
        }
        else
            coordinator = healthyMembers.get(0);
        
        connectionProvider.connect(coordinator);
        groupJoinStrategy.onGroupDiscovered(healthyMembers);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.coordinatorDiscovered(coordinator));
    }

    private List<IAddress> getNodeAddresses(List<INode> nodes)
    {
        List<IAddress> addresses = new ArrayList<IAddress>(nodes.size());
        for (INode node : nodes)
            addresses.add(node.getAddress());
            
        return addresses;
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
