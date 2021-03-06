/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.agent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Bytes;
import com.exametrika.common.utils.Files;
import com.exametrika.impl.groups.cluster.discovery.WellKnownAddressesDiscoveryStrategy;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.multicast.RemoteFlowId;
import com.exametrika.impl.groups.simulator.channel.SimGroupChannel;
import com.exametrika.impl.groups.simulator.channel.SimGroupChannelFactory;
import com.exametrika.impl.groups.simulator.channel.SimGroupFactoryParameters;
import com.exametrika.impl.groups.simulator.channel.SimGroupParameters;
import com.exametrika.spi.groups.cluster.state.IAsyncStateStore;
import com.exametrika.spi.groups.cluster.state.IAsyncStateTransferClient;
import com.exametrika.spi.groups.cluster.state.IAsyncStateTransferServer;
import com.exametrika.spi.groups.cluster.state.IStateStore;
import com.exametrika.spi.groups.cluster.state.IStateTransferFactory;

/**
 * The {@link SimTestFactory} is a simulation group test channel factory.
 * 
 * @author Medvedev-A
 */
@SuppressWarnings("unused")
public class SimTestFactory
{
    private static final long SEND_COUNT = Long.MAX_VALUE;
    private SimExecutor executor;
    private int count;
    private Set<String> wellKnownAddresses = new HashSet<String>();
    private SimGroupFactoryParameters factoryParameters;
    private List<SimGroupParameters> parameters = new ArrayList<SimGroupParameters>();
    private SimStateStore stateStore = new SimStateStore();
    private List<SimGroupChannel> channels = new ArrayList<SimGroupChannel>();
    private List<SimStateTransferFactory> stateTransferFactories = new ArrayList<SimStateTransferFactory>();
    private List<SimMessageSender> messageSenders = new ArrayList<SimMessageSender>();
   
    public void init(int count)
    {
        this.count = count;
        createParameters();
    }
    
    public SimGroupChannel createChannel(SimExecutor executor, int index)
    {
        this.executor = executor;
        SimGroupChannelFactory channelFactory = new SimGroupChannelFactory(executor, factoryParameters);
        SimGroupChannel channel = channelFactory.createChannel(parameters.get(index));
        executor.setGroupChannel(channel);
        channel.start();
        wellKnownAddresses.add(channel.getLiveNodeProvider().getLocalNode().getConnection(0));
        channel.getCompartment().addTimerProcessor(messageSenders.get(index));
        channels.add(channel);
        
        return channel;
    }

    private void createFactoryParameters()
    {
        boolean debug = false;//Debug.isDebug();
        factoryParameters = new SimGroupFactoryParameters(debug);
        if (!debug)
        {
            factoryParameters.heartbeatTrackPeriod = 100;
            factoryParameters.heartbeatPeriod = 100;
            factoryParameters.heartbeatStartPeriod = 300;
            factoryParameters.heartbeatFailureDetectionPeriod = 1000;
            factoryParameters.transportChannelTimeout = 1000;
        }
        factoryParameters.discoveryPeriod = 200;
        factoryParameters.groupFormationPeriod = 2000;
        factoryParameters.failureUpdatePeriod = 500;
        factoryParameters.failureHistoryPeriod = 10000;
        factoryParameters.maxShunCount = 3;
        factoryParameters.flushTimeout = 10000;
        factoryParameters.gracefulExitTimeout = 10000;
        factoryParameters.maxStateTransferPeriod = Integer.MAX_VALUE;
        factoryParameters.stateSizeThreshold = 100000;
        factoryParameters.saveSnapshotPeriod = 10000;
        factoryParameters.transferLogRecordPeriod = 1000;
        factoryParameters.transferLogMessagesCount = 2;
        factoryParameters.minLockQueueCapacity = 10000000;
        factoryParameters.dataExchangePeriod = 200;
        factoryParameters.maxBundlingMessageSize = 1000;
        factoryParameters.maxBundlingPeriod = 100;
        factoryParameters.maxBundleSize = 10000;
        factoryParameters.maxTotalOrderBundlingMessageCount = 10;
        factoryParameters.maxUnacknowledgedPeriod = 100;
        factoryParameters.maxUnacknowledgedMessageCount = 100;
        factoryParameters.maxIdleReceiveQueuePeriod = 600000;
        factoryParameters.maxUnlockQueueCapacity = 100000;
        factoryParameters.checkStatePeriod = 300000;
    }
    
    private void createParameters()
    {
        createFactoryParameters();
        for (int i = 0; i < count; i++)
        {
            SimMessageSender sender = new SimMessageSender(i);
            messageSenders.add(sender);
            
            SimStateTransferFactory stateTransferFactory = new SimStateTransferFactory(stateStore);
            stateTransferFactories.add(stateTransferFactory);
            
            SimGroupParameters parameters = new SimGroupParameters();
            parameters.channelName = "node" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = sender;
            parameters.discoveryStrategy = new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses);
            parameters.stateTransferFactory = stateTransferFactory;
            parameters.deliveryHandler = sender;
            parameters.localFlowController = sender;
            parameters.serializationRegistrars.add(new SimBufferMessagePartSerializer());
            
            this.parameters.add(parameters);
        }
    }
    
    private ByteArray createBuffer(int base, int length)
    {
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++)
            buffer[i] = (byte)(base + i);
        
        return new ByteArray(buffer);
    }
    
    private class SimStateTransferServer implements IAsyncStateTransferServer
    {
        SimStateTransferFactory factory;
        
        public SimStateTransferServer(SimStateTransferFactory factory)
        {
            this.factory = factory;
        }
        
        @Override
        public MessageType classifyMessage(IMessage message)
        {
            if (message.getPart() instanceof SimBufferMessagePart)
                return MessageType.STATE_WRITE;
            else
                return MessageType.NON_STATE;
        }

        @Override
        public void saveSnapshot(boolean full, File file)
        {
            if (factory.state != null)
                Files.writeBytes(file, factory.state);
        }
    }
    
    private class SimStateTransferClient implements IAsyncStateTransferClient
    {
        SimStateTransferFactory factory;
        
        public SimStateTransferClient(SimStateTransferFactory factory)
        {
            this.factory = factory;
        }
        
        @Override
        public void loadSnapshot(boolean full, File file)
        {
            factory.state = Files.readBytes(file);
        }
    }
    
    private class SimStateTransferFactory implements IStateTransferFactory
    {
        private ByteArray state;
        private IAsyncStateStore stateStore;
        
        public SimStateTransferFactory(IAsyncStateStore stateStore)
        {
            this.stateStore = stateStore;
        }
        
        @Override
        public IAsyncStateTransferServer createServer(UUID groupId)
        {
            return new SimStateTransferServer(this);
        }

        @Override
        public IAsyncStateTransferClient createClient(UUID groupId)
        {
            return new SimStateTransferClient(this);
        }

        @Override
        public IStateStore createStore(UUID groupId)
        {
            return stateStore;
        }
    }
    
    private class SimStateStore implements IAsyncStateStore
    {
        private ByteArray buffer = createBuffer(17, 100000);
        
        @Override
        public boolean load(UUID id, File state)
        {
            if (id.equals(GroupMemberships.CORE_GROUP_ID))
                Files.writeBytes(state, buffer);
            else
                Assert.error();
            
            return true;
        }

        @Override
        public void save(UUID id, File state)
        {
        }
    }
    
    private static final class SimBufferMessagePart implements IMessagePart
    {
        private final int index;
        private final long value;

        public SimBufferMessagePart(int index, long value)
        {
            this.index = index;
            this.value = value;
        }
        
        @Override
        public int getSize()
        {
            return 12;
        }
        
        @Override
        public String toString()
        {
            return Integer.toString(index) + ":" + Long.toString(value);
        }
    }
    
    private static final class SimBufferMessagePartSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("00d8d2b8-5bf1-4797-8726-6d8d6185d07d");
     
        public SimBufferMessagePartSerializer()
        {
            super(ID, SimBufferMessagePart.class);
        }

        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            SimBufferMessagePart part = (SimBufferMessagePart)object;

            serialization.writeInt(part.index);
            serialization.writeLong(part.value);
        }
        
        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            int index = deserialization.readInt();
            long value = deserialization.readLong();
            return new SimBufferMessagePart(index, value);
        }
    }
    
    private class SimMessageSender implements IReceiver, IDeliveryHandler, IFlowController<RemoteFlowId>, ICompartmentTimerProcessor
    {
        public boolean sendBeforeGroup;
        public boolean send;
        private int index;
        private long count;
        private boolean flowLocked;
        private RemoteFlowId flow;
        private int receivedCount;

        public SimMessageSender(int index)
        {
            this.index = index;
            this.send = index == 0;
        }
        
        @Override
        public void onTimer(long currentTime)
        {
            if (!send)
                return;
            
            SimGroupChannel channel = channels.get(index);
            if (sendBeforeGroup)
            {
                if (!flowLocked && count < SEND_COUNT)
                    channel.send(channel.getMessageFactory().create(GroupMemberships.CORE_GROUP_ADDRESS, 
                        new SimBufferMessagePart(index, count++)));
            }
            else if (!flowLocked && channel.getMembershipService().getMembership() != null && count < SEND_COUNT)
                channel.send(channel.getMessageFactory().create(GroupMemberships.CORE_GROUP_ADDRESS, 
                    new SimBufferMessagePart(index, count++)));
        }
        
        @Override
        public void receive(IMessage message)
        {
            if (message.getPart() instanceof SimBufferMessagePart)
            {
                SimBufferMessagePart part = message.getPart();
                ByteArray buffer = stateTransferFactories.get(index).state;
                long counter = Bytes.readLong(buffer.getBuffer(), buffer.getOffset());
                Bytes.writeLong(buffer.getBuffer(), buffer.getOffset(), counter + (part.index + 1) * part.value);
                receivedCount++;
            }
        }

        @Override
        public void lockFlow(RemoteFlowId flow)
        {
            flowLocked = true;
            this.flow = flow;
        }

        @Override
        public void unlockFlow(RemoteFlowId flow)
        {
            flowLocked = false;
        }

        @Override
        public void onDelivered(IMessage message)
        {
        }
    }
    
    private static class SimFeed implements IFeed
    {
        private long count;
        
        @Override
        public void feed(ISink sink)
        {
            while (true)
            {
                IMessage message = sink.getMessageFactory().create(sink.getDestination(), new SimBufferMessagePart(0, count++));
                if (!sink.send(message))
                    break;
            }
        }
    }
}
