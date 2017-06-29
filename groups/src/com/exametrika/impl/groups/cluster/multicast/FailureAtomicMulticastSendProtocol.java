/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentProcessor;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IPullableSender;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.message.Message;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.groups.MessageFlags;
import com.exametrika.impl.groups.cluster.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.cluster.failuredetection.IGroupFailureDetector;
import com.exametrika.impl.groups.cluster.flush.IFlush;
import com.exametrika.impl.groups.cluster.flush.IFlushParticipant;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.IGroupMembershipManager;

/**
 * The {@link FailureAtomicMulticastSendProtocol} represents a failure atomic reliable multicast send protocol. Protocol requires
 * unicast reliable FIFO transport (like TCP).
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class FailureAtomicMulticastSendProtocol extends AbstractProtocol implements IFailureDetectionListener, 
    IFlushParticipant, ITimeService, ICompartmentProcessor, IFlowController<RemoteFlowId>
{
    private final IGroupMembershipManager membershipManager;
    private final IGroupFailureDetector failureDetector;
    private final int maxBundlingMessageSize;
    private final long maxBundlingPeriod;
    private final int maxBundleSize;
    private final long maxUnacknowledgedPeriod;
    private final boolean durable;
    private final ISerializationRegistry serializationRegistry;
    private /*TODO:final*/ SendQueue sendQueue;
    private final List<IMessage> pendingSentNewMessages = new ArrayList<IMessage>();
    private final GroupAddress groupAddress;
    private IFlush flush;
    private boolean flushGranted;
    private boolean groupFormed;
    
    public FailureAtomicMulticastSendProtocol(String channelName, IMessageFactory messageFactory, 
        IGroupMembershipManager membershipManager, IGroupFailureDetector failureDetector, 
        int maxBundlingMessageSize, long maxBundlingPeriod, int maxBundleSize, 
        long maxUnacknowledgedPeriod, IDeliveryHandler senderDeliveryHandler, boolean durable, 
        int maxUnlockQueueCapacity, int minLockQueueCapacity, ISerializationRegistry serializationRegistry,
        GroupAddress groupAddress, UUID groupId)
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
        this.durable = durable;
        this.serializationRegistry = serializationRegistry;
        this.groupAddress = groupAddress;
//TODO:        
//        this.sendQueue = new SendQueue(this, failureDetector, this, senderDeliveryHandler, durable, maxUnlockQueueCapacity, 
//            minLockQueueCapacity, messageFactory, groupAddress, groupId);
    }

    public void setLocalFlowController(IFlowController<RemoteFlowId> flowController)
    {
        Assert.notNull(flowController);
        
        this.sendQueue.setFlowController(flowController);
    }
    
    public void setCompartment(ICompartment compartment)
    {
        sendQueue.setCompartment(compartment);
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
        sendQueue.beforeProcessFlush(flush);
    }

    @Override
    public void processFlush()
    {
        flush.grantFlush(this);
    }
    
    @Override
    public void endFlush()
    {
        sendQueue.endFlush();
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
    public void onTimer(long currentTime)
    {
        if (currentTime > sendQueue.getBundleCreationTime() + maxBundlingPeriod)
            sendBundle(false, 0);
        
        if (sendQueue.isCompletionRequired() && currentTime > sendQueue.getLastCompletionSendTime() + 
            (maxBundlingPeriod + maxUnacknowledgedPeriod))
            sendCompletion();
    }

    @Override
    public void lockFlow(RemoteFlowId flow)
    {
        sendQueue.lockFlow(flow);
    }

    @Override
    public void unlockFlow(RemoteFlowId flow)
    {
        sendQueue.unlockFlow(flow);
    }
    
    @Override
    public void process()
    {
        sendQueue.process();
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new FailureAtomicMessagePartSerializer());
        registry.register(new CompleteMessagePartSerializer());
        registry.register(new AcknowledgeSendMessagePartSerializer());
        registry.register(new BundleMessagePartSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(FailureAtomicMessagePartSerializer.ID);
        registry.unregister(CompleteMessagePartSerializer.ID);
        registry.unregister(AcknowledgeSendMessagePartSerializer.ID);
        registry.unregister(BundleMessagePartSerializer.ID);
    }
    
    @Override
    public long getCurrentTime()
    {
        return timeService.getCurrentTime();
    }
    
    @Override
    public void stop()
    {
        sendQueue.stop();
        
        super.stop();
    }
    
    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        if (!groupFormed && message.getDestination() instanceof GroupAddress)
        {
            pendingSentNewMessages.add(message);
            return;
        }
        
        message = doSend(message);
        if (message != null)
            sender.send(message);
    }
    
    @Override
    protected ISink doRegister(IPullableSender pullableSender, IAddress destination, IFeed feed)
    {
        if (destination.equals(groupAddress))
            return sendQueue.register(feed);
        else
            return pullableSender.register(destination, feed);
    }
    
    @Override
    protected void doUnregister(IPullableSender pullableSender, ISink sink)
    {
        if (sink.getDestination().equals(groupAddress))
            sendQueue.unregister((MulticastSink)sink);
        else
            pullableSender.unregister(sink);
    }
    
    @Override
    protected boolean supportsPullSendModel()
    {
        return false;
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof AcknowledgeSendMessagePart)
        {
            AcknowledgeSendMessagePart part = message.getPart();
            sendQueue.acknowledge(message.getSource(), part.getLastReceivedMessageId());
            
            if (!part.isStandalone())
            {
                message = message.removePart();
                doReceive(receiver, message);
            }
            
            if (durable && message.hasFlags(MessageFlags.NO_DELAY) && sendQueue.isCompletionRequired())
                sendCompletion();
            
            return;
        }
        else
            receiver.receive(message);
    }
    
    private IMessage doSend(IMessage message)
    {
        if (message.getDestination() instanceof GroupAddress)
        {
            Assert.isNull(message.getFiles());
            
            long messageId = sendQueue.acquireMessageId();
            
            long order = 0;
// TODO:           if (totalOrderProtocol != null && totalOrderProtocol.isCoordinator() && !message.hasFlags(MessageFlags.UNORDERED))
//                order = totalOrderProtocol.acquireOrder(1);
            
            FailureAtomicMessagePart part = new FailureAtomicMessagePart(messageId, order);
            message = message.addPart(part, true);

            sendQueue.offer(message, messageId);

            if (message.hasFlags(MessageFlags.NO_DELAY) || message.getSize() > maxBundlingMessageSize ||
                sendQueue.getQueueCapacity() > maxBundleSize || 
                timeService.getCurrentTime() > sendQueue.getBundleCreationTime() + maxBundlingPeriod)
                sendBundle(false, message.getFlags());
            
            return null;
        }
//TODO:        else if (!(message.getPart() instanceof AcknowledgeSendMessagePart) && 
//            !message.hasOneOfFlags(MessageFlags.HIGH_PRIORITY | MessageFlags.PARALLEL | MessageFlags.LOW_PRIORITY))
//        {
//            ReceiveQueue receiveQueue = receiveQueues.get(message.getDestination());
//            if (receiveQueue != null && receiveQueue.isAcknowledgementRequired())
//            {
//                message = message.addPart(new AcknowledgeSendMessagePart(receiveQueue.getLastReceivedMessageId(), false));
//                receiveQueue.acknowledge();
//            }
//        }
        
        return message;
    }
    
    private void sendBundle(boolean onStartFlush, int flags)
    {
        IGroupMembership membership;
        if (onStartFlush)
            membership = membershipManager.getMembership();
        else
            membership = membershipManager.getPreparedMembership();
        if (membership == null)
            return;
        
        List<IMessage> bundledMessages = sendQueue.createBundle();
        if (bundledMessages == null)
            return;
        
        long minCompletedMessageId = 0;
        if (flush == null)
            minCompletedMessageId = sendQueue.complete();
        
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
            IGroupMembership membership = membershipManager.getMembership();
            Assert.checkState(membership != null);
            
            CompleteMessagePart part = new CompleteMessagePart(minCompletedMessageId);
            for (INode node : membership.getGroup().getMembers())
            {
                if (!failureDetector.isHealthyMember(node.getId()))
                    continue;
                
                send(messageFactory.create(node.getAddress(), part));
            }
        }
        
        if (flush != null)
            tryGrantFlush();
    }
}
