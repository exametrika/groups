/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Test;

import com.exametrika.api.groups.core.IGroup;
import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipListener;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelFactory.FactoryParameters;
import com.exametrika.common.messaging.impl.ChannelFactory.Parameters;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.HeartbeatProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.INodeTrackingStrategy;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ConnectionManager;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Bytes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.core.channel.GroupChannel;
import com.exametrika.impl.groups.core.channel.IChannelReconnector;
import com.exametrika.impl.groups.core.channel.IGracefulCloseStrategy;
import com.exametrika.impl.groups.core.discovery.DiscoveryProtocol;
import com.exametrika.impl.groups.core.discovery.WellKnownAddressesDiscoveryStrategy;
import com.exametrika.impl.groups.core.exchange.DataExchangeProtocol;
import com.exametrika.impl.groups.core.exchange.IDataExchangeProvider;
import com.exametrika.impl.groups.core.failuredetection.FailureDetectionProtocol;
import com.exametrika.impl.groups.core.failuredetection.GroupNodeTrackingStrategy;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetectionListener;
import com.exametrika.impl.groups.core.flush.FlushCoordinatorProtocol;
import com.exametrika.impl.groups.core.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.core.flush.IFlush;
import com.exametrika.impl.groups.core.flush.IFlushParticipant;
import com.exametrika.impl.groups.core.membership.IMembershipManager;
import com.exametrika.impl.groups.core.membership.IPreparedMembershipListener;
import com.exametrika.impl.groups.core.membership.MembershipManager;
import com.exametrika.impl.groups.core.membership.MembershipTracker;
import com.exametrika.impl.groups.core.membership.Memberships;
import com.exametrika.impl.groups.core.multicast.FailureAtomicMulticastProtocol;
import com.exametrika.impl.groups.core.state.StateTransferClientProtocol;
import com.exametrika.impl.groups.core.state.StateTransferServerProtocol;
import com.exametrika.spi.groups.IDiscoveryStrategy;
import com.exametrika.spi.groups.IStateStore;
import com.exametrika.spi.groups.IStateTransferClient;
import com.exametrika.spi.groups.IStateTransferFactory;
import com.exametrika.spi.groups.IStateTransferServer;
import com.exametrika.tests.common.messaging.ReceiverMock;
import com.exametrika.tests.groups.MembershipManagerTests.PropertyProviderMock;

/**
 * The {@link MulticastProtocolTests} are tests for flush.
 * 
 * @author Medvedev-A
 */
public class MulticastProtocolTests
{
    private static final int COUNT = 10;
    private static final long SEND_COUNT = Long.MAX_VALUE;
    private GroupChannel[] channels = new GroupChannel[COUNT];
    private Sequencer flushSequencer = new Sequencer();
    
    @After
    public void tearDown()
    {
        for (IChannel channel : channels)
            IOs.close(channel);
    }
    
    @Test
    public void testMulticast() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet());
         
        Threads.sleep(10000);
        
        checkMembership(channelFactory, Collections.<Integer>asSet());
    }

    @Test
    public void testChangeMembership() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet(0, 1));
        
        channels[0].start();
        channels[1].start();
        IOs.close(channels[COUNT - 2]);
        IOs.close(channels[COUNT - 1]);
        
        Threads.sleep(10000);
        
        checkMembership(channelFactory, Collections.<Integer>asSet(COUNT - 2, COUNT - 1));
    }
    
    @Test
    public void testCoordinatorFailureBeforeFlush() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet(0, 1));

        channels[0].start();
        channels[1].start();
        
        IGroup group = channelFactory.messageSenders.get(2).membership.getGroup();
        int index = group.getMembers().indexOf(group.getCoordinator());
        Threads.sleep(1000);
        IOs.close(channels[index]);
        
        Threads.sleep(10000);
        
        checkMembership(channelFactory, Collections.<Integer>asSet(index));
    }
    
    @Test
    public void testCoordinatorFailureAfterFlush() throws Exception
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        createGroup(wellKnownAddresses, channelFactory, Collections.<Integer>asSet(0, 1));
         
        Threads.sleep(10000);
         
        checkMembership(channelFactory, Collections.<Integer>asSet(0, 1));

        failOnFlush(channelFactory);
        
        channels[0].start();
        channels[1].start();
        
        flushSequencer.waitAll(COUNT - 2, 5000, 0);
        IGroup group = channelFactory.messageSenders.get(2).membership.getGroup();
        int index = group.getMembers().indexOf(group.getCoordinator());
        
        IOs.close(channels[index]);
        
        Threads.sleep(10000);
        
        checkMembership(channelFactory, Collections.<Integer>asSet(index));
    }
    
    private void createGroup(Set<String> wellKnownAddresses, TestChannelFactory channelFactory, Set<Integer> skipIndexes)
    {
        for (int i = 0; i < COUNT; i++)
        {
            Parameters parameters = new Parameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            if (!skipIndexes.contains(i))
            {
                channel.start();
                wellKnownAddresses.add(channel.getLiveNodeProvider().getLocalNode().getConnection());
            }
            channels[i] = (GroupChannel)channel;
        }
    }

    private void checkMembership(TestChannelFactory channelFactory, Set<Integer> skipIndexes)
    {
        IMembership membership = null;
        ByteArray state = null;
        for (int i = 0; i < COUNT; i++)
        {
            if (skipIndexes.contains(i))
                continue;
            
            IMembership nodeMembership = channels[i].getMembershipService().getMembership();
            if (membership == null)
                membership = nodeMembership;
            
            assertThat(membership, is(nodeMembership));
            assertThat(membership.getGroup(), is(nodeMembership.getGroup()));
            assertThat(membership.getGroup().getMembers(), is(nodeMembership.getGroup().getMembers()));
            assertThat(membership.getGroup().isPrimary(), is(nodeMembership.getGroup().isPrimary()));
            assertThat(membership.getGroup().getName(), is(nodeMembership.getGroup().getName()));
            assertThat(membership.getGroup().getAddress(), is(nodeMembership.getGroup().getAddress()));
            assertThat(membership.getGroup().getCoordinator(), is(nodeMembership.getGroup().getCoordinator()));
            assertThat(membership.getGroup().findMember(channels[i].getMembershipService().getLocalNode().getId()), 
                is(channels[i].getMembershipService().getLocalNode()));
            
            ByteArray nodeState = channelFactory.stateTransferFactories.get(i).state;
            if (state == null)
                state = nodeState;
            else
                assertThat(nodeState, is(state));
        }
        
        assertThat(membership.getGroup().getMembers().size(), is(COUNT - skipIndexes.size()));
    }
    
    private ByteArray createBuffer(int base, int length)
    {
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++)
            buffer[i] = (byte)(base + i);
        
        return new ByteArray(buffer);
    }
    
    private void failOnFlush(TestChannelFactory channelFactory)
    {
        for (TestMessageSender sender : channelFactory.messageSenders)
            sender.failOnFlush = true;
    }
    
    private class TestStateTransferServer implements IStateTransferServer
    {
        TestStateTransferFactory factory;
        
        public TestStateTransferServer(TestStateTransferFactory factory)
        {
            this.factory = factory;
        }
        
        @Override
        public MessageType classifyMessage(IMessagePart part)
        {
            if (part instanceof TestBufferMessagePart)
                return MessageType.STATE_WRITE;
            else
                return MessageType.NON_STATE;
        }

        @Override
        public void saveSnapshot(File file)
        {
            if (factory.state != null)
                Files.writeBytes(file, factory.state);
        }
    }
    
    private class TestStateTransferClient implements IStateTransferClient
    {
        TestStateTransferFactory factory;
        
        public TestStateTransferClient(TestStateTransferFactory factory)
        {
            this.factory = factory;
        }
        
        @Override
        public void loadSnapshot(File file)
        {
            factory.state = Files.readBytes(file);
        }
    }
    
    private class TestStateTransferFactory implements IStateTransferFactory
    {
        private ByteArray state;
        
        @Override
        public IStateTransferServer createServer()
        {
            return new TestStateTransferServer(this);
        }

        @Override
        public IStateTransferClient createClient()
        {
            return new TestStateTransferClient(this);
        }
    }
    
    private class TestStateStore implements IStateStore
    {
        private ByteArray buffer = createBuffer(17, 100000);
        
        @Override
        public void load(UUID id, File state)
        {
            if (id.equals(Memberships.CORE_GROUP_ID))
                Files.writeBytes(state, buffer);
            else
                Assert.error();
        }

        @Override
        public void save(UUID id, File state)
        {
        }
    }
    
    private static final class TestBufferMessagePart implements IMessagePart
    {
        private final int index;
        private final long value;

        public TestBufferMessagePart(int index, long value)
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
    
    private static final class TestBufferMessagePartSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("b9ca8da1-e56d-475f-b59e-220baa7d2d19");
     
        public TestBufferMessagePartSerializer()
        {
            super(ID, TestBufferMessagePart.class);
        }

        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            TestBufferMessagePart part = (TestBufferMessagePart)object;

            serialization.writeInt(part.index);
            serialization.writeLong(part.value);
        }
        
        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            int index = deserialization.readInt();
            long value = deserialization.readLong();
            return new TestBufferMessagePart(index, value);
        }
    }
    
    private class TestMessageSender extends AbstractProtocol implements IFlushParticipant
    {
        public boolean failOnFlush;
        private final TestStateTransferFactory stateTransferFactory;
        private IMembership membership;
        private int index;
        private long count;

        public TestMessageSender(int index, String channelName, IMessageFactory messageFactory, TestStateTransferFactory stateTransferFactory)
        {
            super(channelName, messageFactory);
            
            this.index = index;
            this.stateTransferFactory = stateTransferFactory;
        }
        
        @Override
        public boolean isFlushProcessingRequired()
        {
            return false;
        }

        @Override
        public void setCoordinator()
        {
        }

        @Override
        public void startFlush(IFlush flush)
        {
            this.membership = flush.getNewMembership();
            flush.grantFlush(this);
            if (failOnFlush)
                flushSequencer.allowSingle();
        }

        @Override
        public void beforeProcessFlush()
        {
        }

        @Override
        public void processFlush()
        {
            Assert.error();
        }

        @Override
        public void endFlush()
        {
        }
        
        @Override
        public void onTimer(long currentTime)
        {
            if (membership != null && count < SEND_COUNT)
                getSender().send(getMessageFactory().create(membership.getGroup().getAddress(), 
                    new TestBufferMessagePart(index, count++)));
        }
        
        @Override
        public void register(ISerializationRegistry registry)
        {
            registry.register(new TestBufferMessagePartSerializer());
        }

        @Override
        public void unregister(ISerializationRegistry registry)
        {
            registry.unregister(TestBufferMessagePartSerializer.ID);
        }
        
        @Override
        protected void doReceive(IReceiver receiver, IMessage message)
        {
            if (message.getPart() instanceof TestBufferMessagePart)
            {
                TestBufferMessagePart part = message.getPart();
                ByteArray buffer = stateTransferFactory.state;
                long counter = Bytes.readLong(buffer.getBuffer(), buffer.getOffset());
                Bytes.writeLong(buffer.getBuffer(), buffer.getOffset(), counter + (part.index + 1) * part.value);
            }
            else
                receiver.receive(message);
        }
    }
    
    private static FactoryParameters getFactoryParameters()
    {
        boolean debug = Debug.isDebug();
        
        FactoryParameters factoryParameters = new FactoryParameters(debug);
        if (!debug)
        {
            factoryParameters.heartbeatTrackPeriod = 100;
            factoryParameters.heartbeatPeriod = 100;
            factoryParameters.heartbeatStartPeriod = 300;
            factoryParameters.heartbeatFailureDetectionPeriod = 1000;
            factoryParameters.transportChannelTimeout = 1000;
        }
        
        return factoryParameters;
    }
    
    public static class TestDeliveryHandler implements IDeliveryHandler
    {
        public boolean delivered;

        @Override
        public void onDelivered(IMessage message)
        {
            delivered = true;
        }
    }
    
    private class TestChannelFactory extends ChannelFactory
    {
        private final IDiscoveryStrategy discoveryStrategy;
        private final long discoveryPeriod = 200;
        private final long groupFormationPeriod = 2000;
        private long failureUpdatePeriod = 500;
        private long failureHistoryPeriod = 10000;
        private int maxShunCount = 3;
        private long flushTimeout = 10000;
        private long gracefulCloseTimeout = 10000;
        private long maxStateTransferPeriod = Integer.MAX_VALUE;
        private long stateSizeThreshold = 100000;
        private long saveSnapshotPeriod = 1000;
        private long transferLogRecordPeriod = 1000;
        private int transferLogMessagesCount = 2;
        private int minLockQueueCapacity = 10000000;
        private long dataExchangePeriod = 200;
        private List<TestStateTransferFactory> stateTransferFactories = new ArrayList<TestStateTransferFactory>();
        private MembershipTracker membershipTracker;
        private MembershipManager membershipManager;
        private List<IGracefulCloseStrategy> gracefulCloseStrategies = new ArrayList<IGracefulCloseStrategy>();
        private TestStateStore stateStore = new TestStateStore();
        private List<TestMessageSender> messageSenders = new ArrayList<TestMessageSender>();
        private List<StateTransferClientProtocol> clientProtocols = new ArrayList<StateTransferClientProtocol>();
        private int maxBundlingMessageSize;
        private long maxBundlingPeriod;
        private int maxBundleSize;
        private int maxTotalOrderBundlingMessageCount;
        private long maxUnacknowledgedPeriod;
        private int maxUnacknowledgedMessageCount;
        private long maxIdleReceiveQueuePeriod;
        private int maxUnlockQueueCapacity;
        private List<IDeliveryHandler> deliveryHandlers = new ArrayList<IDeliveryHandler>();
        
        public TestChannelFactory(IDiscoveryStrategy discoveryStrategy)
        {
            super(getFactoryParameters());
            this.discoveryStrategy = discoveryStrategy;
            
            boolean debug = Debug.isDebug();
            int timeMultiplier = !debug ? 1 : 1000;
            flushTimeout *= timeMultiplier;
            gracefulCloseTimeout *= timeMultiplier;
        }

        @Override
        protected INodeTrackingStrategy createNodeTrackingStrategy()
        {
            return new GroupNodeTrackingStrategy();
        }
        
        @Override
        protected void createProtocols(Parameters parameters, String channelName, IMessageFactory messageFactory, 
            ISerializationRegistry serializationRegistry, ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers, 
            List<AbstractProtocol> protocols)
        {
            Set<IPreparedMembershipListener> preparedMembershipListeners = new HashSet<IPreparedMembershipListener>();
            Set<IMembershipListener> membershipListeners = new HashSet<IMembershipListener>();
            membershipManager = new MembershipManager("test", liveNodeProvider, new PropertyProviderMock(), 
                preparedMembershipListeners, membershipListeners);

            Set<IFailureDetectionListener> failureDetectionListeners = new HashSet<IFailureDetectionListener>();
            FailureDetectionProtocol failureDetectionProtocol = new FailureDetectionProtocol(channelName, messageFactory, membershipManager, 
                failureDetectionListeners, failureUpdatePeriod, failureHistoryPeriod, maxShunCount);
            preparedMembershipListeners.add(failureDetectionProtocol);
            failureObservers.add(failureDetectionProtocol);
            
            DiscoveryProtocol discoveryProtocol = new DiscoveryProtocol(channelName, messageFactory, membershipManager, 
                failureDetectionProtocol, discoveryStrategy, liveNodeProvider, discoveryPeriod, 
                groupFormationPeriod);
            preparedMembershipListeners.add(discoveryProtocol);
            membershipListeners.add(discoveryProtocol);
            membershipManager.setNodeDiscoverer(discoveryProtocol);
            
            TestStateTransferFactory stateTransferFactory = new TestStateTransferFactory();
            stateTransferFactories.add(stateTransferFactory);
            StateTransferClientProtocol stateTransferClientProtocol = new StateTransferClientProtocol(channelName,
                messageFactory, membershipManager, stateTransferFactory, stateStore, serializationRegistry, 
                maxStateTransferPeriod, stateSizeThreshold);
            protocols.add(stateTransferClientProtocol);
            discoveryProtocol.setGroupJoinStrategy(stateTransferClientProtocol);
            failureDetectionListeners.add(stateTransferClientProtocol);
            clientProtocols.add(stateTransferClientProtocol);
            
            StateTransferServerProtocol stateTransferServerProtocol = new StateTransferServerProtocol(channelName, 
                messageFactory, membershipManager, failureDetectionProtocol, stateTransferFactory, stateStore, serializationRegistry, 
                saveSnapshotPeriod, transferLogRecordPeriod, transferLogMessagesCount, minLockQueueCapacity);
            protocols.add(stateTransferServerProtocol);
            
            TestMessageSender testSender = new TestMessageSender(messageSenders.size(), channelName, messageFactory, stateTransferFactory);
            protocols.add(testSender);
            messageSenders.add(testSender);
            
            TestDeliveryHandler deliveryHandler = new TestDeliveryHandler();
            deliveryHandlers.add(deliveryHandler);
            FailureAtomicMulticastProtocol multicastProtocol = new FailureAtomicMulticastProtocol(channelName, 
                messageFactory, membershipManager, failureDetectionProtocol, maxBundlingMessageSize, maxBundlingPeriod, 
                maxBundleSize, maxTotalOrderBundlingMessageCount, maxUnacknowledgedPeriod, maxUnacknowledgedMessageCount, 
                maxIdleReceiveQueuePeriod, deliveryHandler, true, true, maxUnlockQueueCapacity, minLockQueueCapacity, 
                serializationRegistry);
            protocols.add(multicastProtocol);
            failureDetectionListeners.add(multicastProtocol);
            
            FlushParticipantProtocol flushParticipantProtocol = new FlushParticipantProtocol(channelName, messageFactory, 
                Arrays.<IFlushParticipant>asList(stateTransferClientProtocol, stateTransferServerProtocol, multicastProtocol,
                    testSender), membershipManager, failureDetectionProtocol);
            protocols.add(flushParticipantProtocol);
            FlushCoordinatorProtocol flushCoordinatorProtocol = new FlushCoordinatorProtocol(channelName, messageFactory, 
                membershipManager, failureDetectionProtocol, flushTimeout, flushParticipantProtocol);
            failureDetectionListeners.add(flushCoordinatorProtocol);
            protocols.add(flushCoordinatorProtocol);

            DataExchangeProtocol dataExchangeProtocol = new DataExchangeProtocol(channelName, messageFactory, membershipManager,
                failureDetectionProtocol, Arrays.<IDataExchangeProvider>asList(), dataExchangePeriod);
            membershipListeners.add(dataExchangeProtocol);
            protocols.add(dataExchangeProtocol);
            failureDetectionListeners.add(dataExchangeProtocol);
            
            protocols.add(discoveryProtocol);
            protocols.add(failureDetectionProtocol);
            
            membershipTracker = new MembershipTracker(1000, membershipManager, discoveryProtocol, 
                failureDetectionProtocol, flushCoordinatorProtocol, null);
            
            gracefulCloseStrategies.add(flushCoordinatorProtocol);
            gracefulCloseStrategies.add(flushParticipantProtocol);
            gracefulCloseStrategies.add(membershipTracker);
        }
        
        @Override
        protected void wireProtocols(Channel channel, TcpTransport transport, ProtocolStack protocolStack)
        {
            FailureDetectionProtocol failureDetectionProtocol = protocolStack.find(FailureDetectionProtocol.class);
            failureDetectionProtocol.setFailureObserver(transport);
            failureDetectionProtocol.setChannelReconnector((IChannelReconnector)channel);
            channel.getCompartment().addProcessor(membershipTracker);
            
            GroupNodeTrackingStrategy strategy = (GroupNodeTrackingStrategy)protocolStack.find(HeartbeatProtocol.class).getNodeTrackingStrategy();
            strategy.setFailureDetector(failureDetectionProtocol);
            strategy.setMembershipManager((IMembershipManager)failureDetectionProtocol.getMembersipService());
            
            FailureAtomicMulticastProtocol multicastProtocol = protocolStack.find(FailureAtomicMulticastProtocol.class);
            multicastProtocol.setFlowController(transport);
            
            StateTransferClientProtocol stateTransferClientProtocol = protocolStack.find(StateTransferClientProtocol.class);
            stateTransferClientProtocol.setChannelReconnector((IChannelReconnector)channel);
            stateTransferClientProtocol.setCompartment(channel.getCompartment());
            
            StateTransferServerProtocol stateTransferServerProtocol = protocolStack.find(StateTransferServerProtocol.class);
            stateTransferServerProtocol.setCompartment(channel.getCompartment());
            stateTransferServerProtocol.setFlowController(transport);
        }
        
        @Override
        protected Channel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
            MessageFactory messageFactory, ProtocolStack protocolStack, TcpTransport transport,
            ConnectionManager connectionManager, ICompartment compartment)
        {
            return new GroupChannel(channelName, liveNodeManager, channelObserver, protocolStack, transport, messageFactory, 
                connectionManager, compartment, membershipManager, gracefulCloseStrategies, gracefulCloseTimeout);
        }
    }
}
