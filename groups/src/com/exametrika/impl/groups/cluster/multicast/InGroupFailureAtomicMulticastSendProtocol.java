/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.flush.IFlush;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;

/**
 * The {@link InGroupFailureAtomicMulticastSendProtocol} represents an in-group failure atomic reliable multicast send protocol. Protocol requires
 * unicast reliable FIFO transport (like TCP).
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class InGroupFailureAtomicMulticastSendProtocol extends AbstractFailureAtomicMulticastSendProtocol implements IFailureDetectionListener, 
    IFlushParticipant
{
    private final IGroupMembershipManager membershipManager;
    private final IGroupFailureDetector failureDetector;
    private final List<IMessage> pendingSentNewMessages = new ArrayList<IMessage>();
    private final Map<IAddress, ReceiveQueue> receiveQueues;
    private final TotalOrderProtocol totalOrderProtocol;
    private IFlush flush;
    private boolean flushGranted;
    private boolean groupFormed;
    
    public InGroupFailureAtomicMulticastSendProtocol(String channelName, IMessageFactory messageFactory, 
        IGroupMembershipManager membershipManager, IGroupFailureDetector failureDetector, 
        int maxBundlingMessageSize, long maxBundlingPeriod, int maxBundleSize, 
        long maxUnacknowledgedPeriod, IDeliveryHandler senderDeliveryHandler, boolean durable, 
        int maxUnlockQueueCapacity, int minLockQueueCapacity, ISerializationRegistry serializationRegistry,
        GroupAddress groupAddress, UUID groupId, Map<IAddress, ReceiveQueue> receiveQueues, 
        TotalOrderProtocol totalOrderProtocol)
    {
        super(channelName, messageFactory, maxBundlingMessageSize, maxBundlingPeriod, maxBundleSize, maxUnacknowledgedPeriod,
            senderDeliveryHandler, durable, maxUnlockQueueCapacity, minLockQueueCapacity, serializationRegistry, 
            groupAddress, groupId);
        
        Assert.notNull(membershipManager);
        Assert.notNull(failureDetector);
        Assert.notNull(receiveQueues);
        Assert.notNull(totalOrderProtocol);

        this.membershipManager = membershipManager;
        this.failureDetector = failureDetector;
        this.receiveQueues = receiveQueues;
        this.totalOrderProtocol = totalOrderProtocol;
        this.sendQueue = new InGroupSendQueue(this, failureDetector, this, senderDeliveryHandler, durable, maxUnlockQueueCapacity, 
            minLockQueueCapacity, messageFactory, groupAddress, groupId, logger, marker);
    }

    public void tryGrantFlush()
    {
        if (!flushGranted && sendQueue.isLastOldMembershipMessageCompleted())
        {
            flush.grantFlush(this);
            flushGranted = true;
        }
    }
    
    @Override
    public boolean isFlushProcessingRequired()
    {
        return true;
    }

    @Override
    public void setCoordinator()
    {
    }
    
    @Override
    public void startFlush(IFlush flush)
    {
        boolean started = false;
        if (this.flush == null)
        {
            sendBundle(true, 0);
            sendQueue.setLastOldMembershipMessageId();
            started = true;
        }
        
        this.flush = flush;
        flushGranted = false;
        
        if (started)
        {
            if (sendQueue.isCompletionRequired())
                sendCompletion();
        }
        
        tryGrantFlush();
    }

    @Override
    public void beforeProcessFlush()
    {
        ((InGroupSendQueue)sendQueue).beforeProcessFlush(flush);
    }

    @Override
    public void processFlush()
    {
        flush.grantFlush(this);
    }
    
    @Override
    public void endFlush()
    {
        ((InGroupSendQueue)sendQueue).endFlush();
        groupFormed = true;
        
        flush = null;
        
        for (IMessage message : pendingSentNewMessages)
            send(message);
        
        pendingSentNewMessages.clear();
    }

    @Override
    public void onMemberFailed(INode member)
    {
        sendQueue.onMemberFailed(member);
    }

    @Override
    public void onMemberLeft(INode member)
    {
        onMemberFailed(member);
    }
   
    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        if (!groupFormed && message.getDestination() instanceof GroupAddress)
        {
            pendingSentNewMessages.add(message);
            return;
        }
        
        super.doSend(sender, message);
    }
    
    @Override
    protected void sendCompletion()
    {
        super.sendCompletion();
        
        if (flush != null)
            tryGrantFlush();
    }
   
    @Override
    protected long acquireTotalOrder(IMessage message)
    {
        long order = 0;
        if (totalOrderProtocol != null && totalOrderProtocol.isCoordinator() && !message.hasFlags(MessageFlags.UNORDERED))
             order = totalOrderProtocol.acquireOrder(1);
        return order;
    }
    
    @Override
    protected IMessage acknowledgePiggyback(IMessage message)
    {
        if (!(message.getPart() instanceof AcknowledgeSendMessagePart) && 
                !message.hasOneOfFlags(MessageFlags.HIGH_PRIORITY | MessageFlags.PARALLEL | MessageFlags.LOW_PRIORITY))
        {
            ReceiveQueue receiveQueue = receiveQueues.get(message.getDestination());
            if (receiveQueue != null && receiveQueue.isAcknowledgementRequired())
            {
                message = message.addPart(new AcknowledgeSendMessagePart(receiveQueue.getLastReceivedMessageId(), false));
                receiveQueue.acknowledge();
            }
        }
        return message;
    }
    
    @Override
    protected boolean isFlush()
    {
        return flush != null;
    }
    
    @Override
    protected IGroup getGroup(boolean onStartFlush)
    {
        IGroupMembership membership;
        if (onStartFlush)
            membership = membershipManager.getMembership();
        else
            membership = membershipManager.getPreparedMembership();
        
        if (membership != null)
            return membership.getGroup();
        else
            return null;
    }
    
    @Override
    protected boolean isHealthyNode(INode node)
    {
        return failureDetector.isHealthyMember(node.getId());
    }
    
    @Override
    protected boolean isFailedOrLeftNode(INode node)
    {
        return failureDetector.getFailedMembers().contains(node) || failureDetector.getLeftMembers().contains(node);
    }
}
