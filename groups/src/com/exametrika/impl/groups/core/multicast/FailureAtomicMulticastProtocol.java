/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.impl.message.Message;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.core.exchange.IExchangeData;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;
import com.exametrika.impl.groups.core.flush.IExchangeableFlushParticipant;
import com.exametrika.impl.groups.core.flush.IFlush;
import com.exametrika.impl.groups.core.membership.GroupAddress;
import com.exametrika.impl.groups.core.membership.IMembershipManager;

/**
 * The {@link FailureAtomicMulticastProtocol} represents a failure atomic reliable multicast protocol. Protocol requires
 * unicast reliable FIFO transport (like TCP).
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class FailureAtomicMulticastProtocol extends AbstractProtocol implements IFailureDetectionListener, 
    IExchangeableFlushParticipant, ITimeService
{
    private final IMembershipManager membershipManager;
    private final IFailureDetector failureDetector;
    private final int maxBundlingMessageSize;
    private final long maxBundlingPeriod;
    private final int maxBundleSize;
    private final long maxUnacknowledgedPeriod;
    private final int maxUnacknowledgedMessageCount;
    private final long maxIdleReceiveQueuePeriod;
    private final boolean durable;
    private final int maxUnlockQueueCapacity;
    private final int minLockQueueCapacity;
    private IFlowController<IAddress> flowController;
    private final ISerializationRegistry serializationRegistry;
    private Map<IAddress, ReceiveQueue> receiveQueues = new LinkedHashMap<IAddress, ReceiveQueue>();
    private final SendQueue sendQueue;
    private final MessageRetransmitProtocol retransmitProtocol;
    private final List<IMessage> pendingNewMessages = new ArrayList<IMessage>();
    private final OrderedQueue orderedQueue;
    private final TotalOrderProtocol totalOrderProtocol;
    private IFlush flush;
    private boolean flushGranted;
    private int pendingQueueCapacity;
    private boolean flowLocked;
    
    public FailureAtomicMulticastProtocol(String channelName, IMessageFactory messageFactory, 
        IMembershipManager membershipManager, IFailureDetector failureDetector, 
        int maxBundlingMessageSize, long maxBundlingPeriod, int maxBundleSize, int maxTotalOrderBundlingMessageCount,
        long maxUnacknowledgedPeriod, int maxUnacknowledgedMessageCount, long maxIdleReceiveQueuePeriod, 
        IDeliveryHandler senderDeliveryHandler, boolean durable, boolean ordered, 
        int maxUnlockQueueCapacity, int minLockQueueCapacity, ISerializationRegistry serializationRegistry)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(membershipManager);
        Assert.notNull(failureDetector);
        Assert.notNull(serializationRegistry);

        this.membershipManager = membershipManager;
        this.failureDetector = failureDetector;
        this.maxBundlingMessageSize = maxBundlingMessageSize;
        this.maxBundlingPeriod = maxBundlingPeriod;
        this.maxBundleSize = maxBundleSize;
        this.maxUnacknowledgedPeriod = maxUnacknowledgedPeriod;
        this.maxUnacknowledgedMessageCount = maxUnacknowledgedMessageCount;
        this.maxIdleReceiveQueuePeriod = maxIdleReceiveQueuePeriod;
        this.durable = durable;
        this.maxUnlockQueueCapacity = maxUnlockQueueCapacity;
        this.minLockQueueCapacity = minLockQueueCapacity;
        this.serializationRegistry = serializationRegistry;

        if (durable)
            ordered = true;
        
        this.sendQueue = new SendQueue(failureDetector, this, senderDeliveryHandler, durable, maxUnlockQueueCapacity, 
            minLockQueueCapacity);
        
        if (ordered)
        {
            orderedQueue = new OrderedQueue(this, maxUnlockQueueCapacity, minLockQueueCapacity);
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

    public void setFlowController(IFlowController<IAddress> flowController)
    {
        Assert.notNull(flowController);
        Assert.isNull(this.flowController);
        
        this.flowController = flowController;
        this.sendQueue.setFlowController(flowController);
        this.retransmitProtocol.setFlowController(flowController);
        if (orderedQueue != null)
        {
            orderedQueue.setFlowController(flowController);
            totalOrderProtocol.setFlowController(flowController);
        }
    }
    
    public void tryGrantFlush()
    {
        if (!flushGranted && !retransmitProtocol.isStabilizationPhase() && sendQueue.isLastOldMembershipMessageCompleted())
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
        sendBundle(0);
        sendQueue.setLastOldMembershipMessageId();
        
        this.flush = flush;
        flushGranted = false;
        retransmitProtocol.startFlush(flush);
        
        if (totalOrderProtocol != null)
            totalOrderProtocol.startFlush();
        
        if (sendQueue.isCompletionRequired())
            sendCompletion();
    }

    @Override
    public void beforeProcessFlush()
    {
        retransmitProtocol.beforeProcessFlush();
        sendQueue.beforeProcessFlush(flush);
        
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
        
        for (IMessage message : pendingNewMessages)
        {
            if (failureDetector.isHealthyMember(message.getSource().getId()))
                receive(message);
        }
        
        pendingNewMessages.clear();
        
        if (flowLocked)
        {
            flowController.unlockFlow(null);
            flowLocked = false;
            pendingQueueCapacity = 0;
        }
    }

    @Override
    public void onMemberFailed(INode member)
    {
        sendQueue.onMemberFailed(member);
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
        if (currentTime > sendQueue.getBundleCreationTime() + maxBundlingPeriod)
            sendBundle(0);
        
        if (sendQueue.isCompletionRequired() && currentTime > sendQueue.getLastCompletionSendTime() + 
            (maxBundlingPeriod + maxUnacknowledgedPeriod))
            sendCompletion();
        
        processReceiveQueues(currentTime);
        
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
    
    public ReceiveQueue ensureReceiveQueue(IAddress sender, long startMessageId)
    {
        ReceiveQueue receiveQueue = receiveQueues.get(sender);
        if (receiveQueue == null)
        {
            receiveQueue = new ReceiveQueue(sender, this, orderedQueue, startMessageId, durable, 
                totalOrderProtocol != null, maxUnlockQueueCapacity, minLockQueueCapacity, flowController, timeService.getCurrentTime());
            receiveQueues.put(sender, receiveQueue);
        }
        return receiveQueue;
    }
    
    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        if (message.getDestination() instanceof GroupAddress)
        {
            Assert.isNull(message.getFiles());
            
            long messageId = sendQueue.acquireMessageId();
            
            long order = 0;
            if (totalOrderProtocol != null && totalOrderProtocol.isCoordinator() && !message.hasFlags(MessageFlags.UNORDERED))
                order = totalOrderProtocol.acquireOrder(1);
            
            FailureAtomicMessagePart part = new FailureAtomicMessagePart(messageId, order);
            message = message.addPart(part, true);

            sendQueue.offer(message);

            if (message.hasFlags(MessageFlags.NO_DELAY) || message.getSize() > maxBundlingMessageSize ||
                sendQueue.getQueueCapacity() > maxBundleSize || 
                timeService.getCurrentTime() > sendQueue.getBundleCreationTime() + maxBundlingPeriod)
                sendBundle(message.getFlags());
        }
        else if (!(message.getPart() instanceof AcknowledgeSendMessagePart))
        {
            ReceiveQueue receiveQueue = receiveQueues.get(message.getDestination());
            if (receiveQueue != null && receiveQueue.isAcknowledgementRequired())
            {
                message = message.addPart(new AcknowledgeSendMessagePart(receiveQueue.getLastReceivedMessageId()));
                receiveQueue.acknowledge();
            }
            
            sender.send(message);
        }
        else
            sender.send(message);
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof BundleMessagePart)
        {
            BundleMessagePart part = message.getPart();
            
            IMembership membership = membershipManager.getMembership();//TODO:
            Assert.checkState(membership != null);
            
            if (part.getMembershipId() < membership.getId())
                return;
            
            if (part.getMembershipId() > membership.getId())
            {
                pendingNewMessages.add(message);
                pendingQueueCapacity += message.getSize();
                
                if (!flowLocked && pendingQueueCapacity >= minLockQueueCapacity)
                {
                    flowLocked = true;
                    flowController.lockFlow(message.getSource());
                }
                
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
                send(messageFactory.create(message.getSource(), new AcknowledgeSendMessagePart(receiveQueue.getLastReceivedMessageId()),
                    MessageFlags.HIGH_PRIORITY | (noDelay ? MessageFlags.NO_DELAY : 0)));
                receiveQueue.acknowledge();
            }
            
            if (flush == null && totalOrderProtocol != null && totalOrderProtocol.isCoordinator() &&
                !message.getSource().equals(membershipManager.getLocalNode().getAddress()))
                totalOrderProtocol.onReceiveMulticast(receiveQueue, message.hasFlags(MessageFlags.NO_DELAY));
            
            receiveQueue.complete(part.getCompletedMessageId());
            return;
        }
        else if (message.getPart() instanceof AcknowledgeSendMessagePart)
        {
            AcknowledgeSendMessagePart part = message.getPart();
            sendQueue.acknowledge(message.getSource(), part.getLastReceivedMessageId());
            
            message = message.removePart();
            if (message != null)
                doReceive(receiver, message);
            
            if (durable && message.hasFlags(MessageFlags.NO_DELAY) && sendQueue.isCompletionRequired())
                sendCompletion();
            
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
            message = message.removePart();
            receiver.receive(message);
        }
        else
            receiver.receive(message);
    }
    
    private void sendBundle(int flags)
    {
        List<IMessage> bundledMessages = sendQueue.createBundle();
        if (bundledMessages == null)
            return;
        
        long minCompletedMessageId = sendQueue.complete();

        IMembership membership = membershipManager.getPreparedMembership();
        Assert.checkState(membership != null);
        
        ByteOutputStream stream = new ByteOutputStream(0x1000);
        ISerialization serialization = new Serialization(serializationRegistry, true, stream);
        Serializers.writeVarInt(serialization, bundledMessages.size());
        
        for (IMessage bundledMessage : bundledMessages)
            MessageSerializers.serialize(serialization, (Message)bundledMessage);
        
        BundleMessagePart bundlePart = new BundleMessagePart(membership.getId(), minCompletedMessageId, 
            new ByteArray(stream.getBuffer(), 0, stream.getLength()));
        
        for (INode node : membership.getGroup().getMembers())
        {
            if (failureDetector.getFailedMembers().contains(node) || failureDetector.getLeftMembers().contains(node))
                continue;
            
            send(messageFactory.create(node.getAddress(), bundlePart, flags));
        }
    }
    
    private void sendCompletion()
    {
        long minCompletedMessageId = sendQueue.complete();
        
        if (minCompletedMessageId != 0)
        {
            IMembership membership = membershipManager.getMembership();//TODO:
            Assert.checkState(membership != null);
            
            CompleteMessagePart part = new CompleteMessagePart(minCompletedMessageId);
            for (INode node : membership.getGroup().getMembers())
            {
                if (!failureDetector.isHealthyMember(node.getId()))
                    continue;
                
                send(messageFactory.create(node.getAddress(), part, MessageFlags.HIGH_PRIORITY));
            }
        }
        
        if (flush != null)
            tryGrantFlush();
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
                send(messageFactory.create(entry.getKey(), new AcknowledgeSendMessagePart(receiveQueue.getLastReceivedMessageId()),
                    MessageFlags.HIGH_PRIORITY));
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
        
        this.receiveQueues = new LinkedHashMap<IAddress, ReceiveQueue>(receiveQueues);
    }
}
