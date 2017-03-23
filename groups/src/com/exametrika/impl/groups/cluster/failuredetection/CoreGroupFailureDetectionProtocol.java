/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.failuredetection;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroupMembershipService;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.channel.IChannelReconnector;

/**
 * The {@link CoreGroupFailureDetectionProtocol} represents a core group failure detection protocol.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CoreGroupFailureDetectionProtocol extends GroupFailureDetectionProtocol
{
    private IChannelReconnector channelReconnector;
    private final long failureUpdatePeriod;
    private final long failureHistoryPeriod;
    private final int maxShunCount;
    private Map<IAddress, FailureInfo> failureHistory = new LinkedHashMap<IAddress, FailureInfo>();
    private long lastFailureUpdateTime;

    public CoreGroupFailureDetectionProtocol(String channelName, IMessageFactory messageFactory, IGroupMembershipService membershipService, 
        Set<IFailureDetectionListener> failureDetectionListeners, long failureUpdatePeriod, long failureHistoryPeriod, int maxShunCount)
    {
        super(channelName, messageFactory, membershipService, failureDetectionListeners);
        
        this.failureUpdatePeriod = failureUpdatePeriod;
        this.failureHistoryPeriod = failureHistoryPeriod;
        this.maxShunCount = maxShunCount;
    }

    public void setChannelReconnector(IChannelReconnector channelReconnector)
    {
        Assert.notNull(channelReconnector);
        Assert.isNull(this.channelReconnector);
        
        this.channelReconnector = channelReconnector;
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

    @Override
    protected void addHistory(long currentTime, INode member, boolean failed)
    {
        failureHistory.put(member.getAddress(), new FailureInfo(currentTime, failed));
    }

    @Override
    protected boolean isHistoryContains(INode member)
    {
        return failureHistory.containsKey(member.getAddress());
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
}
