/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.util.HashMap;
import java.util.Map;

import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataDeserialization;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.multicast.RemoteFlowId;
import com.exametrika.tests.groups.load.TestLoadSpec.SendType;

public final class TestLoadMessageSender implements IReceiver, IDeliveryHandler, IFlowController<RemoteFlowId>, 
    ICompartmentTimerProcessor, ILifecycle
{
    private final TestLoadSpec loadSpec;
    private final int index;
    private final GroupAddress groupAddress;
    private long count;
    private boolean flowLocked;
    private IChannel channel;
    
    private TestLoadStateTransferFactory stateTransferFactory;
    private long lastDeliveredCount = -1;
    private Map<IAddress, Long> receivedMessagesMap = new HashMap<IAddress, Long>();
    private long startTime = Times.getCurrentTime();
    private long sendCount;
    private ByteArray buffer;
    private ISink sink;

    public TestLoadMessageSender(int index, TestLoadSpec loadSpec, GroupAddress groupAddress)
    {
        Assert.notNull(loadSpec);
        Assert.notNull(groupAddress);
        
        this.index = index;
        this.loadSpec = loadSpec;
        this.groupAddress = groupAddress;
        this.buffer = createBuffer(index, getBufferLength());
    }
    
    public void setChannel(IChannel channel)
    {
        Assert.notNull(channel);
        Assert.isNull(this.channel);
        
        this.channel = channel;
    }
    
    public void setStateTransferFactory(TestLoadStateTransferFactory stateTransferFactory)
    {
        Assert.notNull(stateTransferFactory);
        Assert.isNull(this.stateTransferFactory);
        
        this.stateTransferFactory = stateTransferFactory;
    }
    
    @Override
    public synchronized void onTimer(long currentTime)
    {
        if (allowSend(SendType.DIRECT))
        {
            IMessage message = channel.getMessageFactory().create(groupAddress, createPart());
            channel.send(message);
        }
    }
    
    @Override
    public synchronized void receive(IMessage message)
    {
        if (message.getPart() instanceof TestLoadMessagePart)
        {
            TestLoadMessagePart part = message.getPart();
            Long lastReceivedCount = receivedMessagesMap.get(message.getSource());
            if (lastReceivedCount == null)
                lastReceivedCount = -1l;
            
            Assert.isTrue(part.getCount() == lastReceivedCount + 1);
            receivedMessagesMap.put(message.getSource(), lastReceivedCount + 1);
            
            ByteArray buffer = stateTransferFactory.getState();
            ByteInputStream in = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
            DataDeserialization deserialization = new DataDeserialization(in);
            long receiveCounter = 0;
            long totalValue = 1;
            ByteArray data = buffer;
            if (deserialization.readBoolean())
            {
                receiveCounter = deserialization.readLong();
                totalValue = deserialization.readLong();
                data = deserialization.readByteArray();
            }
            receiveCounter++;
            totalValue *= receiveCounter * (part.getIndex() + 1) * (part.getCount() + 1);
            xor(data, buffer);
            
            ByteOutputStream out = new ByteOutputStream();
            DataSerialization serialization = new DataSerialization(out);
            serialization.writeBoolean(true);
            serialization.writeLong(receiveCounter);
            serialization.writeLong(totalValue);
            serialization.writeByteArray(data);
            
            stateTransferFactory.setState(buffer);
        }
    }

    @Override
    public void lockFlow(RemoteFlowId flow)
    {
        flowLocked = true;
    }

    @Override
    public void unlockFlow(RemoteFlowId flow)
    {
        flowLocked = false;
    }

    @Override
    public void onDelivered(IMessage message)
    {
        if (message.getPart() instanceof TestLoadMessagePart)
        {
            TestLoadMessagePart part = message.getPart();
            Assert.isTrue(part.getCount() == lastDeliveredCount + 1);
            lastDeliveredCount++;
        }
    }

    public boolean allowSend(SendType sendType)
    {
        if (loadSpec.getSendType() != sendType)
            return false;
        if (flowLocked)
            return false;
        
        switch (loadSpec.getSendSourceType())
        {
        case SINGLE_NODE:
            {
                if (index != 0)
                    return false;
            }
        case ALL_NODES:
            break;
        }
        
        switch (loadSpec.getSendFrequencyType())
        {
        case MAXIMUM:
            break;
        case SET:
            {
                long timeDelta = Times.getCurrentTime() - startTime;
                if (timeDelta > 0 && sendCount * 1000d / timeDelta > loadSpec.getSendFrequency())
                    return false;
            }
        }
        
        sendCount++;
        return true;
    }

    public IMessagePart createPart()
    {
        return new TestLoadMessagePart(index, count++, buffer);
    }

    @Override
    public void start()
    {
        if (loadSpec.getSendType() == SendType.PULLABLE)
        {
            TestLoadFeed feed = new TestLoadFeed(this);
            sink = channel.register(groupAddress, feed);
            sink.setReady(true);
        }
    }

    @Override
    public void stop()
    {
        if (sink != null)
            channel.unregister(sink);
    }
    
    private int getBufferLength()
    {
        switch (loadSpec.getMessageSizeType())
        {
        case SMALL:
            return 100;
        case MEDIUM:
            return 10000;
        case LARGE:
            return 1000000;
        case SET:
            return loadSpec.getMessageSize();
        default:
            return Assert.error();
        }
    }
    
    public static ByteArray createBuffer(int base, int length)
    {
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++)
            buffer[i] = (byte)(base + i);
        
        return new ByteArray(buffer);
    }
    
    private void xor(ByteArray target, ByteArray buffer)
    {
        Assert.isTrue(target.getLength() >= buffer.getLength());
        
        for (int i = 0; i < target.getLength(); i++)
            target.getBuffer()[target.getOffset() + i] ^= buffer.getBuffer()[buffer.getOffset() + i];
    }
}