/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.List;
import java.util.UUID;

import com.exametrika.api.groups.cluster.IGroup;
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
import com.exametrika.impl.groups.cluster.membership.GroupAddress;

/**
 * The {@link AbstractFailureAtomicMulticastSendProtocol} represents a failure atomic reliable multicast send protocol. Protocol requires
 * unicast reliable FIFO transport (like TCP).
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractFailureAtomicMulticastSendProtocol extends AbstractProtocol implements ITimeService, 
    ICompartmentProcessor, IFlowController<RemoteFlowId>
{
    private final int maxBundlingMessageSize;
    private final long maxBundlingPeriod;
    private final int maxBundleSize;
    private final long maxUnacknowledgedPeriod;
    private final boolean durable;
    private final ISerializationRegistry serializationRegistry;
    protected AbstractSendQueue sendQueue;
    protected final GroupAddress groupAddress;
    protected final UUID groupId;

    public AbstractFailureAtomicMulticastSendProtocol(String channelName, IMessageFactory messageFactory, 
        int maxBundlingMessageSize, long maxBundlingPeriod, int maxBundleSize, 
        long maxUnacknowledgedPeriod, IDeliveryHandler senderDeliveryHandler, boolean durable, 
        int maxUnlockQueueCapacity, int minLockQueueCapacity, ISerializationRegistry serializationRegistry,
        GroupAddress groupAddress, UUID groupId)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(serializationRegistry);
        Assert.notNull(groupAddress);
        Assert.notNull(groupId);

        this.maxBundlingMessageSize = maxBundlingMessageSize;
        this.maxBundlingPeriod = maxBundlingPeriod;
        this.maxBundleSize = maxBundleSize;
        this.maxUnacknowledgedPeriod = maxUnacknowledgedPeriod;
        this.durable = durable;
        this.serializationRegistry = serializationRegistry;
        this.groupAddress = groupAddress;
        this.groupId = groupId;
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
            
            long order = acquireTotalOrder(message);
            
            FailureAtomicMessagePart part = new FailureAtomicMessagePart(messageId, order);
            message = message.addPart(part, true);

            sendQueue.offer(message, messageId);

            if (message.hasFlags(MessageFlags.NO_DELAY) || message.getSize() > maxBundlingMessageSize ||
                sendQueue.getQueueCapacity() > maxBundleSize || 
                timeService.getCurrentTime() > sendQueue.getBundleCreationTime() + maxBundlingPeriod)
                sendBundle(false, message.getFlags());
            
            return null;
        }
        else 
            message = acknowledgePiggyback(message);
        
        return message;
    }
    
    protected void sendBundle(boolean onStartFlush, int flags)
    {
        IGroup group = getGroup(onStartFlush);
        if (group == null)
            return;
        
        List<IMessage> bundledMessages = sendQueue.createBundle();
        if (bundledMessages == null)
            return;
        
        long minCompletedMessageId = 0;
        if (!isFlush())
            minCompletedMessageId = sendQueue.complete();
        
        ByteOutputStream stream = new ByteOutputStream(0x1000);
        ISerialization serialization = new Serialization(serializationRegistry, true, stream);
        Serializers.writeVarInt(serialization, bundledMessages.size());
        
        for (IMessage bundledMessage : bundledMessages)
            MessageSerializers.serialize(serialization, (Message)bundledMessage);
        
        BundleMessagePart bundlePart = new BundleMessagePart(group.getChangeId(), minCompletedMessageId, 
            new ByteArray(stream.getBuffer(), 0, stream.getLength()));
        
        for (INode node : group.getMembers())
        {
            if (isFailedOrLeftNode(node))
                continue;
            
            send(messageFactory.create(node.getAddress(), bundlePart, flags));
        }
    }
    
    protected void sendCompletion()
    {
        long minCompletedMessageId = sendQueue.complete();
        
        if (minCompletedMessageId != 0)
        {
            IGroup group = getGroup(true);
            Assert.checkState(group != null);
            
            CompleteMessagePart part = new CompleteMessagePart(minCompletedMessageId);
            for (INode node : group.getMembers())
            {
                if (!isHealthyNode(node))
                    continue;
                
                send(messageFactory.create(node.getAddress(), part));
            }
        }
    }
    
    protected abstract long acquireTotalOrder(IMessage message);
    protected abstract IMessage acknowledgePiggyback(IMessage message);
    protected abstract boolean isFlush();
    protected abstract IGroup getGroup(boolean onStartFlush);
    protected abstract boolean isFailedOrLeftNode(INode node);
    protected abstract boolean isHealthyNode(INode node);
}
