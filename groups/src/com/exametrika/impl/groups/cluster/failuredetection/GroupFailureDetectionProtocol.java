/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipChange;
import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;

/**
 * The {@link GroupFailureDetectionProtocol} represents a group failure detection protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class GroupFailureDetectionProtocol extends AbstractProtocol implements IGroupFailureDetector, IPreparedGroupMembershipListener,
    IFailureObserver
{
    private static final IMessages messages = Messages.get(IMessages.class);
    protected final IGroupMembershipService membershipService;
    private IFailureObserver failureObserver;
    private final Set<IFailureDetectionListener> failureDetectionListeners;
    protected IGroupMembership membership;
    protected INode currentCoordinator;
    protected List<INode> healthyMembers = new ArrayList<INode>();
    protected Set<UUID> healthyMembersIds = new HashSet<UUID>();
    protected Set<INode> failedMembers = new HashSet<INode>();
    protected Set<INode> leftMembers = new HashSet<INode>();
    private boolean inProgress;

    public GroupFailureDetectionProtocol(String channelName, IMessageFactory messageFactory, IGroupMembershipService membershipService, 
        Set<IFailureDetectionListener> failureDetectionListeners)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipService);
        Assert.notNull(failureDetectionListeners);
        
        this.membershipService = membershipService;
        this.failureDetectionListeners = failureDetectionListeners;
    }

    public void setFailureObserver(IFailureObserver failureObserver)
    {
        Assert.notNull(failureObserver);
        Assert.isNull(this.failureObserver);
        
        this.failureObserver = failureObserver;
    }
    
    public IGroupMembershipService getMembersipService()
    {
        return membershipService;
    }
    
    @Override
    public INode getCurrentCoordinator()
    {
        return currentCoordinator;
    }

    @Override
    public List<INode> getHealthyMembers()
    {
        return healthyMembers;
    }
    
    @Override
    public boolean isHealthyMember(UUID member)
    {
        return healthyMembersIds.contains(member);
    }
    
    @Override
    public Set<INode> getFailedMembers()
    {
        return failedMembers;
    }

    @Override
    public Set<INode> getLeftMembers()
    {
        return leftMembers;
    }

    @Override
    public void addFailedMembers(Set<UUID> memberIds)
    {
        Assert.notNull(memberIds);
        
        if (membership == null)
            return;

        long currentTime = timeService.getCurrentTime();
        Set<IAddress> newFailedAddresses = new LinkedHashSet<IAddress>();
        Set<INode> newFailedMembers = new LinkedHashSet<INode>();
        for (UUID id : memberIds)
        {
            INode member = membership.getGroup().findMember(id);
            if (member != null && !failedMembers.contains(member) && !leftMembers.contains(member) && 
                !isHistoryContains(member))
            {
                failedMembers.add(member);
                newFailedAddresses.add(member.getAddress());
                newFailedMembers.add(member);
                addHistory(currentTime, member, true);
            }
        }

        if (newFailedAddresses.isEmpty())
            return;
        
        updateCurrentCoordinator();
        
        if (!inProgress)
        {
            inProgress = true;
            
            failureObserver.onNodesFailed(newFailedAddresses);
            
            inProgress = false;
        }
        
        for (INode member : newFailedMembers)
            onMemberFailed(member);
    }

    @Override
    public void addLeftMembers(Set<UUID> memberIds)
    {
        if (membership == null)
            return;

        long currentTime = timeService.getCurrentTime();
        Set<IAddress> newLeftAddresses = new LinkedHashSet<IAddress>();
        Set<INode> newLeftMembers = new LinkedHashSet<INode>();
        for (UUID id : memberIds)
        {
            INode member = membership.getGroup().findMember(id);
            if (member != null && !leftMembers.contains(member))
            {
                newLeftAddresses.add(member.getAddress());
                newLeftMembers.add(member);
                leftMembers.add(member);
                failedMembers.remove(member);
                addHistory(currentTime, member, false);
            }
        }

        if (newLeftAddresses.isEmpty())
            return;
        
        updateCurrentCoordinator();
        
        if (!inProgress)
        {
            inProgress = true;
            
            failureObserver.onNodesLeft(newLeftAddresses);
            
            inProgress = false;
        }
        
        for (INode member : newLeftMembers)
            onMemberLeft(member);
    }

    @Override
    public void onNodesFailed(Set<IAddress> nodes)
    {
        if (inProgress)
            return;
        
        Set<UUID> memberIds = new HashSet<UUID>();
        for (IAddress address : nodes)
            memberIds.add(address.getId());
        
        inProgress = true;
        
        addFailedMembers(memberIds);
        
        inProgress = false;
    }

    @Override
    public void onNodesLeft(Set<IAddress> nodes)
    {
        if (inProgress)
            return;
        
        Set<UUID> memberIds = new HashSet<UUID>();
        for (IAddress address : nodes)
            memberIds.add(address.getId());
        
        inProgress = true;
        
        addLeftMembers(memberIds);
        
        inProgress = false;
    }

    @Override
    public void onPreparedMembershipChanged(IGroupMembership oldMembership, IGroupMembership newMembership, IGroupMembershipChange membershipChange)
    {
        Assert.notNull(newMembership);
        
        failedMembers.retainAll(newMembership.getGroup().getMembers());
        leftMembers.retainAll(newMembership.getGroup().getMembers());
        this.membership = newMembership;

        updateCurrentCoordinator();
    }

    @Override
    public void start()
    {
        super.start();
        
        Assert.checkState(failureObserver != null);
    }
    
    protected void addHistory(long currentTime, INode member, boolean failed)
    {
    }

    protected boolean isHistoryContains(INode member)
    {
        return false;
    }

    private void onMemberFailed(INode member)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.nodeFailed(member));
        
        for (IFailureDetectionListener listener : failureDetectionListeners)
        {
            try
            {
                listener.onMemberFailed(member);
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception from other listeners
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, marker, e);
            }
        }
    }
    
    private void onMemberLeft(INode member)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.nodeLeft(member));
        
        for (IFailureDetectionListener listener : failureDetectionListeners)
        {
            try
            {
                listener.onMemberLeft(member);
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception from other listeners
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, marker, e);
            }
        }
    }
    
    private void updateCurrentCoordinator()
    {
        if (membership == null)
            return;

        if (currentCoordinator == null || failedMembers.contains(currentCoordinator) || leftMembers.contains(currentCoordinator))
        {
            currentCoordinator = null;
            for (INode member : membership.getGroup().getMembers())
            {
                if (!failedMembers.contains(member) && !leftMembers.contains(member))
                {
                    currentCoordinator = member;
                    break;
                }
            }
    
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.currentCoordinatorUpdated(currentCoordinator));
        }
        
        healthyMembers.clear();
        healthyMembersIds.clear();
        for (INode member : membership.getGroup().getMembers())
        {
            if (!failedMembers.contains(member) && !leftMembers.contains(member))
            {
                healthyMembers.add(member);
                healthyMembersIds.add(member.getId());
            }
        }

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.healthyMembersUpdated(healthyMembers));
        
        if (currentCoordinator == null)
        {
            Assert.checkState(healthyMembers.isEmpty());
            currentCoordinator = membershipService.getLocalNode();
            return;
        }
        
        if (membershipService.getLocalNode().equals(currentCoordinator))
        {
            for (INode member : healthyMembers)
                connectionProvider.connect(member.getAddress());
        }
        else
            connectionProvider.connect(currentCoordinator.getAddress());
    }
    
    private interface IMessages
    {
        @DefaultMessage("Node ''{0}'' has been failed.")
        ILocalizedMessage nodeFailed(INode node);
        @DefaultMessage("Node ''{0}'' has been left.")
        ILocalizedMessage nodeLeft(INode node);
        @DefaultMessage("Current coordinator has been set to ''{0}''.")
        ILocalizedMessage currentCoordinatorUpdated(INode currentCoordinator);  
        @DefaultMessage("Healthy members have been set to ''{0}''.")
        ILocalizedMessage healthyMembersUpdated(List<INode> currentCoordinator);
    }
}
