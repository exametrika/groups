/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.exchange.IExchangeData;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.flush.IExchangeableFlushParticipant;
import com.exametrika.impl.groups.cluster.flush.IFlush;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;

/**
 * The {@link FailureAtomicMulticastReceiveProtocol} represents a failure atomic reliable multicast receive protocol. Protocol requires
 * unicast reliable FIFO transport (like TCP).
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class FailureAtomicMulticastReceiveProtocol extends AbstractProtocol implements IFailureDetectionListener, 
    IExchangeableFlushParticipant, ITimeService, IFailureAtomicMulticast
    
{
    private final IGroupMembershipManager membershipManager;
    private final IGroupFailureDetector failureDetector;
    private final long maxUnacknowledgedPeriod;
    private final int maxUnacknowledgedMessageCount;
    private final long maxIdleReceiveQueuePeriod;
    private final boolean durable;
    private final int maxUnlockQueueCapacity;
    private final int minLockQueueCapacity;
    private IFlowController<RemoteFlowId> remoteFlowController;
    private final ISerializationRegistry serializationRegistry;
    private final Map<IAddress, ReceiveQueue> receiveQueues = new LinkedHashMap<IAddress, ReceiveQueue>();
    private final MessageRetransmitProtocol retransmitProtocol;
    private final List<IMessage> pendingReceivedNewMessages = new ArrayList<IMessage>();
    private final OrderedQueue orderedQueue;
    private final TotalOrderProtocol totalOrderProtocol;
    private final GroupAddress groupAddress;
    private final UUID groupId;
    private IFlush flush;
    private boolean flushGranted;
    private final QueueCapacityController capacityController;
    
    public FailureAtomicMulticastReceiveProtocol(String channelName, IMessageFactory messageFactory, 
        IGroupMembershipManager membershipManager, IGroupFailureDetector failureDetector, long maxBundlingPeriod,
        int maxTotalOrderBundlingMessageCount, long maxUnacknowledgedPeriod, int maxUnacknowledgedMessageCount, 
        long maxIdleReceiveQueuePeriod, boolean durable, boolean ordered, int maxUnlockQueueCapacity, 
        int minLockQueueCapacity, ISerializationRegistry serializationRegistry,
        GroupAddress groupAddress, UUID groupId)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipManager);
        Assert.notNull(failureDetector);
        Assert.notNull(serializationRegistry);

        this.membershipManager = membershipManager;
        this.failureDetector = failureDetector;
        this.maxUnacknowledgedPeriod = maxUnacknowledgedPeriod;
        this.maxUnacknowledgedMessageCount = maxUnacknowledgedMessageCount;
        this.maxIdleReceiveQueuePeriod = maxIdleReceiveQueuePeriod;
        this.durable = durable;
        this.maxUnlockQueueCapacity = maxUnlockQueueCapacity;
        this.minLockQueueCapacity = minLockQueueCapacity;
        this.serializationRegistry = serializationRegistry;
        this.groupAddress = groupAddress;
        this.groupId = groupId;
        this.capacityController = new QueueCapacityController(minLockQueueCapacity, maxUnlockQueueCapacity, 
            groupAddress, groupId);

        if (durable)
            ordered = true;
        
        if (ordered)
        {
            orderedQueue = new OrderedQueue(this, maxUnlockQueueCapacity, minLockQueueCapacity, groupAddress, groupId);
            totalOrderProtocol = new TotalOrderProtocol(this, membershipManager, messageFactory, this, 
                this, maxBundlingPeriod, maxTotalOrderBundlingMessageCount);
        }
        else
        {
            orderedQueue = null;
            totalOrderProtocol = null;
        }
        
        this.retransmitProtocol = new MessageRetransmitProtocol(this, membershipManager, messageFactory, this, 
            this, receiveQueues, this, failureDetector);
    }

    public void setRemoteFlowController(IFlowController<RemoteFlowId> flowController)
    {
        Assert.notNull(flowController);
        Assert.isNull(this.remoteFlowController);
        
        this.remoteFlowController = flowController;
        this.capacityController.setFlowController(flowController);
        if (orderedQueue != null)
            orderedQueue.setFlowController(flowController);
    }
    
    @Override
    public void tryGrantFlush()
    {
        if (!flushGranted && !retransmitProtocol.isStabilizationPhase())
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
        if (totalOrderProtocol != null)
            totalOrderProtocol.setCoordinator(); 
    }
    
    @Override
    public void startFlush(IFlush flush)
    {
        boolean started = false;
        if (this.flush == null)
            started = true;
        
        this.flush = flush;
        flushGranted = false;
        retransmitProtocol.startFlush(flush);
        
        if (started)
        {
            if (totalOrderProtocol != null)
                totalOrderProtocol.startFlush();
        }
        
        tryGrantFlush();
    }

    @Override
    public void beforeProcessFlush()
    {
        retransmitProtocol.beforeProcessFlush();
        completeReceiveQueues();
    }

    @Override
    public void processFlush()
    {
        flush.grantFlush(this);
    }
    
    @Override
    public void endFlush()
    {
        retransmitProtocol.endFlush();
        if (totalOrderProtocol != null)
            totalOrderProtocol.endFlush();
        
        flush = null;
        
        for (IMessage message : pendingReceivedNewMessages)
        {
            if (failureDetector.isHealthyMember(message.getSource().getId()))
                receive(message);
        }
        
        pendingReceivedNewMessages.clear();
        capacityController.clearCapacity();
    }

    @Override
    public void onMemberFailed(INode member)
    {
        retransmitProtocol.onMemberFailed(member);
    }

    @Override
    public void onMemberLeft(INode member)
    {
        onMemberFailed(member);
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        processReceiveQueues(currentTime);
        
        if (totalOrderProtocol != null)
            totalOrderProtocol.onTimer();
    }

    @Override
    public IExchangeData getLocalData()
    {
        return retransmitProtocol.getData();
    }

    @Override
    public void setRemoteData(Map<INode, IExchangeData> data)
    {
        retransmitProtocol.setData(data);
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new FailureAtomicMessagePartSerializer());
        registry.register(new CompleteMessagePartSerializer());
        registry.register(new AcknowledgeSendMessagePartSerializer());
        registry.register(new FailureAtomicExchangeDataSerializer());
        registry.register(new RetransmitMessagePartSerializer());
        registry.register(new TotalOrderMessagePartSerializer());
        registry.register(new BundleMessagePartSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(FailureAtomicMessagePartSerializer.ID);
        registry.unregister(CompleteMessagePartSerializer.ID);
        registry.unregister(AcknowledgeSendMessagePartSerializer.ID);
        registry.unregister(FailureAtomicExchangeDataSerializer.ID);
        registry.unregister(RetransmitMessagePartSerializer.ID);
        registry.unregister(TotalOrderMessagePartSerializer.ID);
        registry.unregister(BundleMessagePartSerializer.ID);
    }
    
    @Override
    public long getCurrentTime()
    {
        return timeService.getCurrentTime();
    }
    
    @Override
    public ReceiveQueue ensureReceiveQueue(IAddress sender, long startMessageId)
    {
        ReceiveQueue receiveQueue = receiveQueues.get(sender);
        if (receiveQueue == null)
        {
            receiveQueue = new ReceiveQueue(sender, this, orderedQueue, startMessageId, durable, 
                totalOrderProtocol != null, maxUnlockQueueCapacity, minLockQueueCapacity, remoteFlowController, 
                timeService.getCurrentTime(), groupAddress, groupId);
            receiveQueues.put(sender, receiveQueue);
        }
        return receiveQueue;
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof BundleMessagePart)
        {
            BundleMessagePart part = message.getPart();
            
            IGroupMembership membership = membershipManager.getMembership();
            long membershipId = 0;
            if (membership != null)
                membershipId = membership.getId();
            
            if (part.getMembershipId() < membershipId)
                return;
            
            if (part.getMembershipId() > membershipId)
            {
                pendingReceivedNewMessages.add(message);
                capacityController.addCapacity(message.getSource(), message.getSize());
                return;
            }

            ByteArray buffer = part.getData();
            ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
            Deserialization deserialization = new Deserialization(serializationRegistry, stream);
            
            int count = Serializers.readVarInt(deserialization);
            List<IMessage> bundledMessages = new ArrayList<IMessage>();
            for (int i = 0; i < count; i++)
                bundledMessages.add(MessageSerializers.deserialize(deserialization, message.getSource(),
                    membership.getGroup().getAddress(), null));
            
            FailureAtomicMessagePart bundleFirstPart = bundledMessages.get(0).getPart();
            long bundleStartMessageId = bundleFirstPart.getMessageId();
            
            ReceiveQueue receiveQueue = ensureReceiveQueue(message.getSource(), bundleStartMessageId);
            
            for (IMessage bundledMessage : bundledMessages)
            {
                FailureAtomicMessagePart messagePart = bundledMessage.getPart();
                receiveQueue.receive(messagePart.getMessageId(), messagePart.getOrder(), bundledMessage, timeService.getCurrentTime());
            }
            
            if (receiveQueue.isAcknowledgementRequired() && ((durable && message.hasFlags(MessageFlags.NO_DELAY)) || 
                (receiveQueue.getLastReceivedMessageId() - receiveQueue.getLastAcknowledgedMessageId() > maxUnacknowledgedMessageCount)))
            {
                boolean noDelay = message.hasFlags(MessageFlags.NO_DELAY);
                send(messageFactory.create(message.getSource(), new AcknowledgeSendMessagePart(receiveQueue.getLastReceivedMessageId(), true),
                    (noDelay ? MessageFlags.NO_DELAY : 0)));
                receiveQueue.acknowledge();
            }
            
            if (flush == null && totalOrderProtocol != null && totalOrderProtocol.isCoordinator() &&
                !message.getSource().equals(membershipManager.getLocalNode().getAddress()))
                totalOrderProtocol.onReceiveMulticast(receiveQueue, message.hasFlags(MessageFlags.NO_DELAY));
            
            receiveQueue.complete(part.getCompletedMessageId());
            return;
        }
        else if (message.getPart() instanceof CompleteMessagePart)
        {
            CompleteMessagePart part = message.getPart();
            
            ReceiveQueue receiveQueue = receiveQueues.get(message.getSource());
            if (receiveQueue != null)
                receiveQueue.complete(part.getCompletedMessageId());
            
            return;
        }
        else if (retransmitProtocol.receive(message))
            return;
        else if (totalOrderProtocol != null && totalOrderProtocol.receive(message))
            return;
        else if (message.getPart() instanceof FailureAtomicMessagePart)
        {
            receive(message.removePart());
            return;
        }
        else
            receiver.receive(message);
    }
    
    private void processReceiveQueues(long currentTime)
    {
        for (Iterator<Map.Entry<IAddress, ReceiveQueue>> it = receiveQueues.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<IAddress, ReceiveQueue> entry = it.next();
            ReceiveQueue receiveQueue = entry.getValue();
            if (receiveQueue.isAcknowledgementRequired() && 
                currentTime >= receiveQueue.getFirstUnacknowledgedReceiveTime() + maxUnacknowledgedPeriod)
            {
                send(messageFactory.create(entry.getKey(), new AcknowledgeSendMessagePart(receiveQueue.getLastReceivedMessageId(), true)));
                receiveQueue.acknowledge();
            }
            else if (!receiveQueue.isAcknowledgementRequired() && receiveQueue.isEmpty() && 
                currentTime > receiveQueue.getLastReceiveTime() + maxIdleReceiveQueuePeriod)
                it.remove();
        }
    }
    
    private void completeReceiveQueues()
    {
        if (orderedQueue != null)
            orderedQueue.flushMessages();
        
        Map<IAddress, ReceiveQueue> receiveQueues = new TreeMap<IAddress, ReceiveQueue>(this.receiveQueues);
        for (Iterator<Map.Entry<IAddress, ReceiveQueue>> it = receiveQueues.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<IAddress, ReceiveQueue> entry = it.next();
            
            ReceiveQueue receiveQueue = entry.getValue();
            receiveQueue.flushMessages();
            
            if (!failureDetector.isHealthyMember(entry.getKey().getId()))
                it.remove();
            else
                receiveQueue.completeAll();
        }
        
        this.receiveQueues.keySet().retainAll(receiveQueues.keySet());
    }
}
