/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.core.channel.IChannelReconnector;
import com.exametrika.impl.groups.core.membership.IPreparedMembershipListener;

/**
 * The {@link CoreFailureDetectionProtocol} represents a failure detection protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreFailureDetectionProtocol extends AbstractProtocol implements IWorkerFailureDetector, IPreparedMembershipListener,
    IFailureObserver
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IMembershipService membershipService;
    private IChannelReconnector channelReconnector;
    private IFailureObserver failureObserver;
    private final Set<IFailureDetectionListener> failureDetectionListeners;
    private final long failureUpdatePeriod;
    private final long failureHistoryPeriod;
    private final int maxShunCount;
    private IMembership membership;
    private INode currentCoordinator;
    private List<INode> healthyMembers = new ArrayList<INode>();
    private Set<UUID> healthyMembersIds = new HashSet<UUID>();
    private Set<INode> failedMembers = new HashSet<INode>();
    private Set<INode> leftMembers = new HashSet<INode>();
    private Map<IAddress, FailureInfo> failureHistory = new LinkedHashMap<IAddress, FailureInfo>();
    private long lastFailureUpdateTime;
    private boolean inProgress;

    public CoreFailureDetectionProtocol(String channelName, IMessageFactory messageFactory, IMembershipService membershipService, 
        Set<IFailureDetectionListener> failureDetectionListeners, long failureUpdatePeriod, long failureHistoryPeriod, int maxShunCount)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipService);
        Assert.notNull(failureDetectionListeners);
        
        this.membershipService = membershipService;
        this.failureDetectionListeners = failureDetectionListeners;
        this.failureUpdatePeriod = failureUpdatePeriod;
        this.failureHistoryPeriod = failureHistoryPeriod;
        this.maxShunCount = maxShunCount;
    }

    public void setFailureObserver(IFailureObserver failureObserver)
    {
        Assert.notNull(failureObserver);
        Assert.isNull(this.failureObserver);
        
        this.failureObserver = failureObserver;
    }
    
    public void setChannelReconnector(IChannelReconnector channelReconnector)
    {
        Assert.notNull(channelReconnector);
        Assert.isNull(this.channelReconnector);
        
        this.channelReconnector = channelReconnector;
    }
    
    public IMembershipService getMembersipService()
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
                !failureHistory.containsKey(member.getAddress()))
            {
                failedMembers.add(member);
                newFailedAddresses.add(member.getAddress());
                newFailedMembers.add(member);
                failureHistory.put(member.getAddress(), new FailureInfo(currentTime, true));
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
                failureHistory.put(member.getAddress(), new FailureInfo(currentTime, false));
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
    public void onPreparedMembershipChanged(IMembership oldMembership, IMembership newMembership, IMembershipChange membershipChange)
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
    
    @Override
    public void onTimer(long currentTime)
    {
        if (membership == null || currentCoordinator == null)
            return;
        
        if (currentTime >= lastFailureUpdateTime + failureUpdatePeriod)
        {
            if (!failedMembers.isEmpty() || !leftMembers.isEmpty())
            {
                Set<UUID> failedMemberIds = new HashSet<UUID>();
                for (INode member : failedMembers)
                    failedMemberIds.add(member.getId());
                
                Set<UUID> leftMemberIds = new HashSet<UUID>();
                for (INode member : leftMembers)
                    leftMemberIds.add(member.getId());
                
                if (membershipService.getLocalNode().equals(currentCoordinator))
                {
                    FailureUpdateMessagePart part = new FailureUpdateMessagePart(failedMemberIds, leftMemberIds);
                    for (INode member : membership.getGroup().getMembers())
                    {
                        if (!failedMembers.contains(member) && !leftMembers.contains(member) && 
                            !member.equals(membershipService.getLocalNode()))
                            send(messageFactory.create(member.getAddress(), part, 
                                MessageFlags.HIGH_PRIORITY | MessageFlags.PARALLEL));
                    }
                }
                else
                    send(messageFactory.create(currentCoordinator.getAddress(), new FailureUpdateMessagePart(
                        failedMemberIds, leftMemberIds), MessageFlags.HIGH_PRIORITY | MessageFlags.PARALLEL));
                
                lastFailureUpdateTime = timeService.getCurrentTime();
            }
        }
        
        if (!failureHistory.isEmpty())
        {
            for (Iterator<FailureInfo> it = failureHistory.values().iterator(); it.hasNext(); )
            {
                FailureInfo info = it.next();
                if (currentTime > info.time + failureHistoryPeriod)
                    it.remove();
                else
                    break;
            }
        }
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new FailureUpdateMessagePartSerializer());
    }
    
    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(FailureUpdateMessagePartSerializer.ID);
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        FailureInfo info = failureHistory.get(message.getSource());
        if (info != null)
        {
            if (info.failed && info.shunCount < maxShunCount)
            {
                send(messageFactory.create(message.getSource(), MessageFlags.SHUN | MessageFlags.HIGH_PRIORITY | MessageFlags.PARALLEL));
                info.shunCount++;
            }
            
            return;
        }
        else if (message.hasFlags(MessageFlags.SHUN))
            channelReconnector.reconnect();
        else if (message.getPart() instanceof FailureUpdateMessagePart)
        {
            FailureUpdateMessagePart part = message.getPart();
            addFailedMembers(part.getFailedMembers());
            addLeftMembers(part.getLeftMembers());
        }
        else
            receiver.receive(message);
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
    
    private static class FailureInfo
    {
        private final long time;
        private final boolean failed;
        private int shunCount;
        
        public FailureInfo(long time, boolean failed)
        {
            this.time = time;
            this.failed = failed;
        }
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
