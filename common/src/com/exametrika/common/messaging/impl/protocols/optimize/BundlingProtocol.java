/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.optimize;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.impl.MessageFlags;
import com.exametrika.common.messaging.impl.message.Message;
import com.exametrika.common.messaging.impl.message.MessageSerializers;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ICleanupManager;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Serializers;

/**
 * The {@link BundlingProtocol} is a bundling protocol. Protocol requires unicast reliable FIFO transport (like TCP).
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class BundlingProtocol extends AbstractProtocol
{
    private final ISerializationRegistry serializationRegistry;
    private final int maxBundlingMessageSize;
    private final long maxBundlingPeriod;
    private final int maxBundleSize;
    private final long sendQueueIdlePeriod;
    private final boolean receiveMessageList;
    private final Map<IAddress, SendQueue> sendQueues = new LinkedHashMap<IAddress, SendQueue>();
    
    public BundlingProtocol(String channelName, IMessageFactory messageFactory, ISerializationRegistry serializationRegistry,
        int maxBundlingMessageSize, long maxBundlingPeriod, int maxBundleSize, long sendQueueIdlePeriod, boolean receiveMessageList)
    {
        super(channelName, messageFactory);
        
        Assert.notNull(serializationRegistry);
        
        this.serializationRegistry = serializationRegistry;
        this.maxBundlingMessageSize = maxBundlingMessageSize;
        this.maxBundlingPeriod = maxBundlingPeriod;
        this.maxBundleSize = maxBundleSize;
        this.sendQueueIdlePeriod = sendQueueIdlePeriod;
        this.receiveMessageList = receiveMessageList;
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        for (Iterator<Map.Entry<IAddress, SendQueue>> it = sendQueues.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<IAddress, SendQueue> entry = it.next();
            IAddress destination = entry.getKey();
            SendQueue sendQueue = entry.getValue();
            
            if (currentTime > sendQueue.bundleCreationTime + maxBundlingPeriod && !sendQueue.isEmpty())
            {
                IMessage message = createBundle(destination, sendQueue);
                getSender().send(message);
            }
            else if (currentTime > sendQueue.bundleCreationTime + sendQueueIdlePeriod)
            {
                Assert.checkState(sendQueue.isEmpty());
                it.remove();
            }
        }
    }

    @Override
    public void register(ISerializationRegistry registry)
    {
        registry.register(new BundleMessagePartSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
        registry.unregister(BundleMessagePartSerializer.ID);
    }
    
    @Override
    public void cleanup(ICleanupManager cleanupManager, ILiveNodeProvider liveNodeProvider, long currentTime)
    {
        for (Iterator<Map.Entry<IAddress, SendQueue>> it = sendQueues.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<IAddress, SendQueue> entry = it.next();
            if (cleanupManager.canCleanup(entry.getKey()))
                it.remove();
        }
    }
    
    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        if (message.hasFlags(MessageFlags.PARALLEL))
        {
            sender.send(message);
            return;
        }
        
        SendQueue sendQueue = sendQueues.get(message.getDestination());
        
        if (message.hasOneOfFlags(MessageFlags.NO_DELAY | MessageFlags.HIGH_PRIORITY) || 
            message.getFiles() != null || message.getSize() > maxBundlingMessageSize)
        {
            if (sendQueue != null && !sendQueue.isEmpty())
            {
                IMessage bundle = createBundle(message.getDestination(), sendQueue);
                sender.send(bundle);
            }
            
            sender.send(message);
        }
        else if (sendQueue != null && !sendQueue.isEmpty() && (sendQueue.size + message.getSize() > maxBundleSize || 
                timeService.getCurrentTime() > sendQueue.bundleCreationTime + maxBundlingPeriod))
            {
                sendQueue.offer(message, timeService.getCurrentTime());

                IMessage bundle = createBundle(message.getDestination(), sendQueue);
                sender.send(bundle);
            }
        else
        {
            if (sendQueue == null)
            {
                sendQueue = new SendQueue();
                sendQueues.put(message.getDestination(), sendQueue);
            }
            
            sendQueue.offer(message, timeService.getCurrentTime());
        }
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        if (message.getPart() instanceof BundleMessagePart)
        {
            BundleMessagePart part = message.getPart();
            
            ByteArray buffer = part.getData();
            ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
            Deserialization deserialization = new Deserialization(serializationRegistry, stream);
            
            int count = Serializers.readVarInt(deserialization);
            List<IMessage> bundledMessages = new ArrayList<IMessage>(count);
            for (int i = 0; i < count; i++)
                bundledMessages.add(MessageSerializers.deserialize(deserialization, message.getSource(),
                    message.getDestination(), null));
            
            if (receiveMessageList)
                receiver.receive(message.removePart().addPart(new MessageListPart(bundledMessages)));
            else
            {
                for (IMessage bundledMessage : bundledMessages)
                    receiver.receive(bundledMessage);
            }
        }
        else
            receiver.receive(message);
    }
    
    private IMessage createBundle(IAddress destination, SendQueue sendQueue)
    {
        List<IMessage> bundledMessages = sendQueue.createBundle();
        
        ByteOutputStream stream = new ByteOutputStream(0x1000);
        ISerialization serialization = new Serialization(serializationRegistry, true, stream);
        Serializers.writeVarInt(serialization, bundledMessages.size());
        
        for (IMessage bundledMessage : bundledMessages)
            MessageSerializers.serialize(serialization, (Message)bundledMessage);
        
        BundleMessagePart bundlePart = new BundleMessagePart(new ByteArray(stream.getBuffer(), 0, stream.getLength()));
        
        return messageFactory.create(destination, bundlePart);
    }
    
    private static class SendQueue
    {
        private List<IMessage> queue = new ArrayList<IMessage>(128);
        private long bundleCreationTime;
        private int size;

        public boolean isEmpty()
        {
            return queue.isEmpty();
        }
        
        public void offer(IMessage message, long currentTime)
        {
            if (queue.isEmpty())
                bundleCreationTime = currentTime;
            
            queue.add(message);
            size += message.getSize();
        }
        
        public List<IMessage> createBundle()
        {
            Assert.checkState(!queue.isEmpty());
            
            List<IMessage> messages = queue;
            queue = new ArrayList<IMessage>(128);
            size = 0;
            bundleCreationTime = 0;
            
            return messages;
        }
    }
}
