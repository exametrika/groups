/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.discovery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.exametrika.api.groups.cluster.GroupMembershipEvent;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.api.groups.cluster.INode;
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
import com.exametrika.common.messaging.impl.protocols.failuredetection.ICleanupManager;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;
import com.exametrika.spi.groups.cluster.discovery.IDiscoveryStrategy;

/**
 * The {@link CoreGroupDiscoveryProtocol} represents a protocol that discovers core group entry points and initiates group joining
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreGroupDiscoveryProtocol extends AbstractProtocol implements ICoreNodeDiscoverer, IPreparedGroupMembershipListener,
    IGroupMembershipListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IGroupMembershipService membershipService;
    private final IDiscoveryStrategy discoveryStrategy;
    private final IGroupFailureDetector failureDetector;
    private final ILiveNodeProvider liveNodeProvider;
    private final long discoveryPeriod;
    private final long groupFormationPeriod;
    private IGroupJoinStrategy groupJoinStrategy;
    private IGroupMembership membership;
    private long membershipId;
    private IAddress coordinator;
    private List<IAddress> healthyMembers = new ArrayList<IAddress>();
    private TreeSet<INode> discoveredNodes = new TreeSet<INode>();
    private long startTime;
    private long lastDiscoveryTime;
    private boolean started;
    private boolean stopped;
    private boolean joined;

    public CoreGroupDiscoveryProtocol(String channelName, IMessageFactory messageFactory, IGroupMembershipService membershipService, 
        IGroupFailureDetector failureDetector, IDiscoveryStrategy discoveryStrategy, 
        ILiveNodeProvider liveNodeProvider, long discoveryPeriod, long groupFormationPeriod)
    {
        super(channelName, messageFactory);

        Assert.notNull(membershipService);
        Assert.notNull(failureDetector);
        Assert.notNull(discoveryStrategy);
        Assert.notNull(liveNodeProvider);

        this.membershipService = membershipService;
        this.failureDetector = failureDetector;
        this.discoveryStrategy = discoveryStrategy;
        this.liveNodeProvider = liveNodeProvider;
        this.discoveryPeriod = discoveryPeriod;
        this.groupFormationPeriod = groupFormationPeriod;
    }

    public void setGroupJoinStrategy(IGroupJoinStrategy groupJoinStrategy)
    {
        Assert.notNull(groupJoinStrategy);
        Assert.isNull(this.groupJoinStrategy);
        
        this.groupJoinStrategy = groupJoinStrategy;
    }
    
    @Override
    public void stop()
    {
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
    public void onPreparedMembershipChanged(IGroupMembership oldMembership, IGroupMembership newMembership, IGroupMembershipChange membershipChange)
    {
        membership = newMembership;

        coordinator = null;
        healthyMembers.clear();
        discoveredNodes.removeAll(newMembership.getGroup().getMembers());
    }

    @Override
    public void onJoined()
    {
        joined = true;
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
    }

    @Override
    public void onMembershipChanged(GroupMembershipEvent event)
    {
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
                for (INode node : discoveredNodes)
                {
                    if (liveNodeProvider.isLive(node.getAddress()))
                        nodes.add(node);
                }
                
                nodes.add(membershipService.getLocalNode());
                DiscoveryMessagePart discoveryPart = new DiscoveryMessagePart(nodes, true);
                
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
                
                lastDiscoveryTime = currentTime;
            }
        }
        else
        {
            if (currentTime > lastDiscoveryTime + discoveryPeriod)
            {
                DiscoveryMessagePart discoveryPart = new DiscoveryMessagePart(
                    java.util.Collections.singleton(membershipService.getLocalNode()), true);
                
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
        }
    }

    @Override
    public void cleanup(ICleanupManager cleanupManager, ILiveNodeProvider liveNodeProvider, long currentTime)
    {
        if (coordinator == null)
        {
            for (Iterator<INode> it = discoveredNodes.iterator(); it.hasNext(); )
            {
                INode node = it.next();
                if (cleanupManager.canCleanup(node.getAddress()))
                {
                    it.remove();
                    
                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, marker, messages.nodeFailed(node));
                }
            }
        }
        else
        {
            if (cleanupManager.canCleanup(coordinator))
            {
                healthyMembers.remove(coordinator);
                updateHealthyMembers(healthyMembers);
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
        if (message.getPart() instanceof DiscoveryMessagePart && ((DiscoveryMessagePart)message.getPart()).isCore())
        {
            DiscoveryMessagePart part = message.getPart();

            for (INode node : part.getDiscoveredNodes())
            {
                if (membership == null)
                {
                    if (coordinator == null && !discoveredNodes.contains(node) && !membershipService.getLocalNode().equals(node))
                    {
                        discoveredNodes.add(node);
                        connectionProvider.connect(node.getAddress());
                        
                        if (logger.isLogEnabled(LogLevel.DEBUG))
                            logger.log(LogLevel.DEBUG, marker, messages.nodeDiscovered(node));
                    }
                }
                else if (joined)
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
            if (membership == null || !joined)
                return;
            
            GroupJoinMessagePart part = message.getPart();
            INode joiningNode = part.getJoiningNode();
            if (membershipService.getLocalNode().equals(failureDetector.getCurrentCoordinator()))
            {
                if (!discoveredNodes.contains(joiningNode) && membership.getGroup().findMember(joiningNode.getId()) == null)
                {
                    discoveredNodes.add(joiningNode);
                    connectionProvider.connect(joiningNode.getAddress());
                    
                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, marker, messages.nodeDiscovered(joiningNode));
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
        Assert.checkState(membership == null);
        
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
