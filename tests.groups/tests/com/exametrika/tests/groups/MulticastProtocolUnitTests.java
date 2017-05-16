/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.groups.cluster.GroupOption;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupMembershipListener;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.groups.cluster.exchange.IExchangeData;
import com.exametrika.impl.groups.cluster.membership.Group;
import com.exametrika.impl.groups.cluster.membership.GroupMembership;
import com.exametrika.impl.groups.cluster.membership.GroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.IPreparedGroupMembershipListener;
import com.exametrika.impl.groups.cluster.membership.LocalNodeProvider;
import com.exametrika.impl.groups.cluster.multicast.FailureAtomicMulticastProtocol;
import com.exametrika.tests.groups.channel.TestNetwork;
import com.exametrika.tests.groups.channel.TestProtocolStack;
import com.exametrika.tests.groups.mocks.DeliveryHandlerMock;
import com.exametrika.tests.groups.mocks.FailureDetectorMock;
import com.exametrika.tests.groups.mocks.FlowControllerMock;
import com.exametrika.tests.groups.mocks.FlushMock;
import com.exametrika.tests.groups.mocks.PropertyProviderMock;

/**
 * The {@link MulticastProtocolUnitTests} are unit tests for multicast protocol.
 * 
 * @author Medvedev-A
 */
public class MulticastProtocolUnitTests
{
    private static final int COUNT = 10;
    private TestNetwork network;
    private long membershipId = 1;
    private IGroupMembership membership;
    private FlushMock flush;
    
    @Before
    public void setUp()
    {
        int maxBundlingMessageSize = 100;
        long maxBundlingPeriod = 1000;
        int maxBundleSize = 200;
        int maxTotalOrderBundlingMessageCount = 2;
        long maxUnacknowledgedPeriod = 1000;
        int maxUnacknowledgedMessageCount = 2;
        long maxIdleReceiveQueuePeriod = 10000;
        boolean durable = true;
        boolean ordered = true;
        int maxUnlockQueueCapacity = 2000;
        int minLockQueueCapacity = 5000;
        List<TestProtocolStack> nodes = new ArrayList<TestProtocolStack>();
        for (int i = 0 ; i < COUNT; i++)
        {
            String channelName = "test" + (i + 1);
            TestInfo info = new TestInfo();
            TestProtocolStack stack = TestProtocolStack.create(channelName);
            stack.getSerializationRegistry().register(new TestMessagePartSerializer());
            stack.setObject(info);
            PropertyProviderMock propertyProvider = new PropertyProviderMock();
            info.localNodeProvider = new LocalNodeProvider(stack.getLiveNodeProvider(), propertyProvider, 
                GroupMemberships.CORE_DOMAIN);
            info.membershipManager = new GroupMembershipManager(channelName, info.localNodeProvider, 
                Collections.<IPreparedGroupMembershipListener>emptySet(), 
                Collections.<IGroupMembershipListener>emptySet());
            info.failureDetector = new FailureDetectorMock();
            info.deliveryHandler = new DeliveryHandlerMock();
            info.localFlowController = new FlowControllerMock();
            info.remoteFlowController = new FlowControllerMock();
            FailureAtomicMulticastProtocol protocol = new FailureAtomicMulticastProtocol(channelName, stack.getMessageFactory(),
                info.membershipManager, info.failureDetector, maxBundlingMessageSize, maxBundlingPeriod, maxBundleSize, 
                maxTotalOrderBundlingMessageCount, maxUnacknowledgedPeriod, maxUnacknowledgedMessageCount, 
                maxIdleReceiveQueuePeriod, info.deliveryHandler, durable, ordered, maxUnlockQueueCapacity, minLockQueueCapacity, 
                stack.getSerializationRegistry(), GroupMemberships.CORE_GROUP_ADDRESS, GroupMemberships.CORE_GROUP_ID);
            protocol.setRemoteFlowController(info.remoteFlowController);
            protocol.setLocalFlowController(info.localFlowController);
            
            stack.setProtocol(protocol);
            nodes.add(stack);
        }
        
        network = new TestNetwork(nodes, System.currentTimeMillis());
        network.start();
    }
    
    @After
    public void tearDown()
    {
        network.stop();
    }
    
    @Test
    public void testSimpleSend()
    {
        TestProtocolStack stack = network.getNodes().get(1);
        sendMessages(stack, 10);
        flush();
        sendMessages(stack, 10);
        network.process(10, 200);
        checkReceived(network, 0, 20);
        checkDelivered(stack, 0, 20);
    }
    
    @Test
    public void testPullableSend()
    {
        TestProtocolStack stack = network.getNodes().get(1);
        TestFeed feed = new TestFeed(10, stack);
        stack.register(GroupMemberships.CORE_GROUP_ADDRESS, feed);
        flush();
        network.process(10, 200);
        checkReceived(network, 0, 20);
        checkDelivered(stack, 0, 20);
    }
    
    @Test
    public void testSenderFailure()
    {
        TestProtocolStack stack = network.getNodes().get(1);
        sendMessages(stack, 10);
        flush();
        sendMessages(stack, 10);
        network.process(10, 200, com.exametrika.common.utils.Collections.asSet(network.getNodes().get(8),
            network.getNodes().get(9)));
        stack.setActive(false);
        flush();
        network.process(10, 200);
        checkReceived(network, 0, 20);
    }
    
    @Test
    public void testReceiverFailure()
    {
        TestProtocolStack stack = network.getNodes().get(1);
        sendMessages(stack, 10);
        flush();
        sendMessages(stack, 10);
        network.process(10, 200, com.exametrika.common.utils.Collections.asSet(network.getNodes().get(8),
            network.getNodes().get(9)));
        network.getNodes().get(8).setActive(false);
        network.getNodes().get(9).setActive(false);
        flush();
        network.process(10, 200);
        checkReceived(network, 0, 20);
        checkDelivered(stack, 0, 20);
    }
    
    private void sendMessages(TestProtocolStack stack, int count)
    {
        for (int i = 0; i < count; i++)
            stack.send(createMessage(stack));
    }
    
    private IMessage createMessage(TestProtocolStack stack)
    {
        TestInfo info = stack.getObject();
        return stack.getMessageFactory().create(GroupMemberships.CORE_GROUP_ADDRESS, new TestMessagePart(info.messageCount++));
    }
    
    private IGroupMembership createMembership(long membershipId, TestNetwork network)
    {
        List<INode> nodes = new ArrayList<INode>();
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            TestInfo info = stack.getObject();
            nodes.add(info.localNodeProvider.getLocalNode());
        }
        
        return new GroupMembership(membershipId, new Group(GroupMemberships.CORE_GROUP_ADDRESS, true, nodes, Enums.of(GroupOption.DURABLE)));
    }
    
    private void startFlush()
    {
        IGroupMembership newMembership = createMembership(membershipId, network);
        flush = new FlushMock();
        flush.groupForming = membership == null;
        flush.oldMembership = membership;
        flush.newMembership = newMembership;
        boolean first = true;
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            
            FailureAtomicMulticastProtocol protocol = stack.getProtocol();
            if (first)
            {
                protocol.setCoordinator();
                first = false;
            }
            
            protocol.startFlush(flush);
        }
        
        network.process(10, 200);
    }
    
    private void exchangeData()
    {
        Map<INode, IExchangeData> dataMap = new HashMap<INode, IExchangeData>();
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            
            TestInfo info = stack.getObject();
            FailureAtomicMulticastProtocol protocol = stack.getProtocol();
            IExchangeData data = protocol.getLocalData();
            dataMap.put(info.localNodeProvider.getLocalNode(), data);
        }
        
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            
            FailureAtomicMulticastProtocol protocol = stack.getProtocol();
            protocol.setRemoteData(dataMap);
        }
        
        network.process(10, 200);
    }
    
    private void beforeProcessFlush()
    {
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            
            FailureAtomicMulticastProtocol protocol = stack.getProtocol();
            protocol.beforeProcessFlush();
        }
        
        network.process(10, 200);
    }
    
    private void processFlush()
    {
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            
            FailureAtomicMulticastProtocol protocol = stack.getProtocol();
            protocol.processFlush();
        }
        
        network.process(10, 200);
    }
    
    private void endFlush()
    {
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            
            FailureAtomicMulticastProtocol protocol = stack.getProtocol();
            protocol.endFlush();
        }
        
        membership = flush.getNewMembership();
        membershipId++;
        flush = null;
        
        network.process(10, 200);
    }
    
    private void flush()
    {
        startFlush();
        exchangeData();
        beforeProcessFlush();
        processFlush();
        endFlush();
    }
    
    private void checkReceived(TestNetwork network, long startId, long endId)
    {
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            
            long id = startId;
            for (IMessage message : stack.getReceivedMessages())
            {
                TestMessagePart part = message.getPart();
                assertThat(part.value, is(id));
                id++;
            }
            
            assertThat(id, is(endId));
            stack.clearReceivedMessages();
        }
    }
    
    private void checkDelivered(TestProtocolStack stack, long startId, long endId)
    {
        TestInfo info = stack.getObject();

        long id = startId;
        for (IMessage message : info.deliveryHandler.messages)
        {
            TestMessagePart part = message.getPart();
            assertThat(part.value, is(id));
            id++;
        }
        
        assertThat(id, is(endId));
        info.deliveryHandler.messages.clear();
    }
    
    private static class TestInfo
    {
        int messageCount;
        LocalNodeProvider localNodeProvider;
        GroupMembershipManager membershipManager;
        FailureDetectorMock failureDetector;
        DeliveryHandlerMock deliveryHandler;
        FlowControllerMock localFlowController;
        FlowControllerMock remoteFlowController;
    }
    
    private class TestFeed implements IFeed
    {
        private long maxCount;
        private final TestProtocolStack stack;
        
        public TestFeed(int maxCount, TestProtocolStack stack)
        {
            this.maxCount = maxCount;
            this.stack = stack;
        }
        
        @Override
        public void feed(ISink sink)
        {
            while (maxCount > 0)
            {
                maxCount--;
                IMessage message = createMessage(stack);
                if (!sink.send(message))
                    break;
            }
        }
    }
    
    private static final class TestMessagePart implements IMessagePart
    {
        private final long value;

        public TestMessagePart(long value)
        {
            this.value = value;
        }
        
        @Override
        public int getSize()
        {
            return 8;
        }
        
        @Override
        public String toString()
        {
            return Long.toString(value);
        }
    }
    
    private static final class TestMessagePartSerializer extends AbstractSerializer
    {
        public static final UUID ID = UUID.fromString("b9ca8da1-e56d-475f-b59e-220baa7d2d19");
     
        public TestMessagePartSerializer()
        {
            super(ID, TestMessagePart.class);
        }

        @Override
        public void serialize(ISerialization serialization, Object object)
        {
            TestMessagePart part = (TestMessagePart)object;

            serialization.writeLong(part.value);
        }
        
        @Override
        public Object deserialize(IDeserialization deserialization, UUID id)
        {
            long value = deserialization.readLong();
            return new TestMessagePart(value);
        }
    }
}
