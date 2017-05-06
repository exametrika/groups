/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.groups.cluster.ClusterMembershipEvent;
import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipListener;
import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IDomainMembershipChange;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupChange;
import com.exametrika.common.compartment.ICompartmentProcessor;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IPullableSender;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.composite.MessageRouter;
import com.exametrika.common.utils.Assert;

/**
 * The {@link WorkerGroupMembershipProtocol} represents a worker node group membership protocol.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class WorkerGroupMembershipProtocol extends MessageRouter implements IClusterMembershipListener, ICompartmentProcessor
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Map<UUID, GroupProtocolSubStack> groupsStacks = new HashMap<UUID, GroupProtocolSubStack>();
    private final IClusterMembershipService membershipService;
    private final IGroupProtocolSubStackFactory protocolSubStackFactory;
    private final long groupSubStackRemoveDelay;
    private final int maxPendingMessageCount;
    private final Map<UUID, List<IMessage>> pendingMessages = new LinkedHashMap<UUID, List<IMessage>>();
    
    public WorkerGroupMembershipProtocol(String channelName, IMessageFactory messageFactory, 
        IClusterMembershipService membershipService, IGroupProtocolSubStackFactory protocolSubStackFactory,
        long groupSubStackRemoveDelay, int maxPendingMessageCount)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipService);
        Assert.notNull(protocolSubStackFactory);
        
        this.membershipService = membershipService;
        this.protocolSubStackFactory = protocolSubStackFactory;
        this.groupSubStackRemoveDelay = groupSubStackRemoveDelay;
        this.maxPendingMessageCount = maxPendingMessageCount;
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        super.register(registry);
        
        registry.register(new GroupMessagePartSerializer());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        super.unregister(registry);
        
        registry.unregister(GroupMessagePartSerializer.ID);
    }

    @Override
    public void onTimer(long currentTime)
    {
        for (Iterator<Map.Entry<UUID, GroupProtocolSubStack>> it = groupsStacks.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<UUID, GroupProtocolSubStack> entry = it.next();
            GroupProtocolSubStack subStack = entry.getValue();
            if (subStack.getStartRemoveTime() != 0 && currentTime > subStack.getStartRemoveTime() + groupSubStackRemoveDelay)
            {
                removeProtocol(subStack);
                it.remove();
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, subStack.getMarker(), messages.groupSubStackRemoved());
            }
        }
    }
    
    @Override
    public void process()
    {
        for (AbstractProtocol protocol : protocols)
            ((GroupProtocolSubStack)protocol).process();
    }
    
    @Override
    public void onJoined()
    {
        IClusterMembership membership = membershipService.getMembership();
        IDomainMembership domainMembership = membership.findDomain(membershipService.getLocalNode().getDomain());
        GroupsMembership groupsMembership = domainMembership.findElement(GroupsMembership.class);
        List<IGroup> nodeGroups = groupsMembership.findNodeGroups(membershipService.getLocalNode().getId());
        for (IGroup group : nodeGroups)
        {
            GroupProtocolSubStack protocolSubStack = protocolSubStackFactory.createProtocolSubStack(group);
            addProtocol(protocolSubStack);
            groupsStacks.put(group.getId(), protocolSubStack);
           
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, protocolSubStack.getMarker(), messages.groupSubStackCreated());
            
            protocolSubStack.installGroupMembership(group);
            
            processPendingMessages(group.getId(), protocolSubStack);
        }
        
        for (AbstractProtocol protocol : protocols)
            ((GroupProtocolSubStack)protocol).onJoined();
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
        for (AbstractProtocol protocol : protocols)
            ((GroupProtocolSubStack)protocol).onLeft(reason);
    }

    @Override
    public void onMembershipChanged(ClusterMembershipEvent event)
    {
        IDomainMembershipChange domainChange = event.getMembershipChange().findChangedDomain(membershipService.getLocalNode().getDomain());
        if (domainChange == null)
            return;
        IDomainMembership domainMembership = event.getNewMembership().findDomain(membershipService.getLocalNode().getDomain());
        GroupsMembership groupsMembership = domainMembership.findElement(GroupsMembership.class);
        List<IGroup> nodeGroups = groupsMembership.findNodeGroups(membershipService.getLocalNode().getId());
        GroupsMembershipChange groupsChange = domainChange.findChange(GroupsMembershipChange.class);
        for (IGroup group : nodeGroups)
        {
            IGroupChange changedGroup = null;
            GroupProtocolSubStack protocolSubStack = null;
            if (groupsChange.getNewGroups().contains(group))
            {
                Assert.checkState(!groupsStacks.containsKey(group.getId()));
                protocolSubStack = protocolSubStackFactory.createProtocolSubStack(group);
                addProtocol(protocolSubStack);
                groupsStacks.put(group.getId(), protocolSubStack);
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, protocolSubStack.getMarker(), messages.groupSubStackRemoved());
            }
            else
            {
                changedGroup = groupsChange.findChangedGroup(group.getId());
                if (changedGroup != null)
                {
                    protocolSubStack = groupsStacks.get(group.getId());
                    Assert.checkState(protocolSubStack != null);
                }
            }
            
            if (protocolSubStack != null)
            {
                if (changedGroup == null)
                {
                    protocolSubStack.installGroupMembership(group);
                    
                    processPendingMessages(group.getId(), protocolSubStack);
                }
                else
                    protocolSubStack.installGroupMembership(changedGroup);
            }
        }
        
        long currentTime = timeService.getCurrentTime();
        for (IGroup group : groupsChange.getRemovedGroups())
        {
            GroupProtocolSubStack protocol = groupsStacks.get(group.getId());
            protocol.setStartRemoveTime(currentTime);
        }
        
        for (AbstractProtocol protocol : protocols)
            ((GroupProtocolSubStack)protocol).onMembershipChanged(event);
    }

    @Override
    protected boolean doReceiveRoute(IMessage message)
    {
        if (message.getPart() instanceof GroupMessagePart)
        {
            GroupMessagePart part = message.getPart();
            message = message.removePart();
            IReceiver receiver = groupsStacks.get(part.getGroupId());
            if (receiver != null)
                receiver.receive(message);
            else
            {
                List<IMessage> list = pendingMessages.get(part.getGroupId());
                if (list == null)
                {
                    list = new ArrayList<IMessage>();
                    pendingMessages.put(part.getGroupId(), list);
                }
                
                if (list.size() < maxPendingMessageCount)
                    list.add(message);
            }
            
            return true;
        }
        else
            return false;
    }
    
    @Override
    protected boolean doSendRoute(IMessage message)
    {
        if (message.getDestination() instanceof GroupAddress)
        {
            ISender sender = groupsStacks.get(message.getDestination().getId());
            if (sender != null)
                sender.send(message);
            
            return true;
        }
        else
            return false;
    }
    
    @Override
    protected ISink doRegisterRoute(IAddress destination, IFeed feed)
    {
        if (destination instanceof GroupAddress)
        {
            IPullableSender sender = groupsStacks.get(destination.getId());
            if (sender != null)
                return sender.register(destination, feed);
            
            return null;
        }
        else
            return null;   
    }
    
    @Override
    protected boolean doUnregisterRoute(ISink sink)
    {
        if (sink.getDestination() instanceof GroupAddress)
        {
            IPullableSender sender = groupsStacks.get(sink.getDestination().getId());
            if (sender != null)
                sender.unregister(sink);
            
            return true;
        }
        else
            return false;   
    }
    
    private void processPendingMessages(UUID groupId, GroupProtocolSubStack protocolSubStack)
    {
        List<IMessage> list = pendingMessages.remove(groupId);
        if (list == null)
            return;
        
        for (IMessage message : list)
            protocolSubStack.receive(message);
    }
    
    private interface IMessages
    {
        @DefaultMessage("Group sub-stack has been created.")
        ILocalizedMessage groupSubStackCreated();
        @DefaultMessage("Group sub-stack has been removed.")
        ILocalizedMessage groupSubStackRemoved();
    }
}
