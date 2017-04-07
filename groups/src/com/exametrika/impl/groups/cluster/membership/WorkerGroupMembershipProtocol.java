/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.routing.MessageRouter;
import com.exametrika.common.utils.Assert;

/**
 * The {@link WorkerGroupMembershipProtocol} represents a worker node group membership protocol.
 * process.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class WorkerGroupMembershipProtocol extends MessageRouter implements IClusterMembershipListener
{
    private final Map<UUID, GroupProtocolSubStack> groupsStacks = new HashMap<UUID, GroupProtocolSubStack>();
    private final IClusterMembershipService membershipService;
    private final IGroupProtocolSubStackFactory protocolSubStackFactory;
    private final long groupSubStackRemoveDelay;
    
    public WorkerGroupMembershipProtocol(String channelName, IMessageFactory messageFactory, 
        IClusterMembershipService membershipService, IGroupProtocolSubStackFactory protocolSubStackFactory,
        long groupSubStackRemoveDelay)
    {
        super(channelName, messageFactory, Collections.<AbstractProtocol>emptyList());
        
        Assert.notNull(membershipService);
        Assert.notNull(protocolSubStackFactory);
        
        this.membershipService = membershipService;
        this.protocolSubStackFactory = protocolSubStackFactory;
        this.groupSubStackRemoveDelay = groupSubStackRemoveDelay;
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
            }
        }
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
           
            protocolSubStack.installGroupMembership(group);
        }
    }

    @Override
    public void onLeft(LeaveReason reason)
    {
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
                    protocolSubStack.installGroupMembership(group);
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
    }

    @Override
    protected boolean doRoute(IMessage message)
    {
        if (message.getPart() instanceof GroupMessagePart)
        {
            GroupMessagePart part = message.getPart();
            IReceiver receiver = groupsStacks.get(part.getGroupId());
            if (receiver != null)
            {
                receiver.receive(message.removePart());
                return true;
            }
        }

        return false;
    }
}
