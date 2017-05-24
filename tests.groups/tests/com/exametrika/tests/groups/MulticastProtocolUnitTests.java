/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.exametrika.common.utils.Times;
import com.exametrika.impl.groups.cluster.exchange.IExchangeData;
import com.exametrika.impl.groups.cluster.membership.Group;
import com.exametrika.impl.groups.cluster.membership.GroupAddressSerializer;
import com.exametrika.impl.groups.cluster.membership.GroupChange;
import com.exametrika.impl.groups.cluster.membership.GroupMembership;
import com.exametrika.impl.groups.cluster.membership.GroupMembershipChange;
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
        Times.setTest(0);
        createNetwork(true);
    }
   
    @After
    public void tearDown()
    {
        network.stop();
        Times.clearTest();
    }
    
    @Test
    public void testSimpleSend()
    {
        TestProtocolStack stack = network.getNodes().get(1);
        flush();
        sendMessages(stack, 10);
        process(10, 200);
        checkReceived(network, stack, 0, 10);
        checkDelivered(stack, 0, 10);
    }
    
    @Test
    public void testNonDurableSend()
    {
        createNetwork(false);
        TestProtocolStack stack = network.getNodes().get(1);
        flush();
        sendMessages(stack, 10);
        process(10, 200);
        checkReceived(network, stack, 0, 10);
    }
    
    @Test
    public void testSimpleSendCoordinator()
    {
        TestProtocolStack stack = network.getNodes().get(0);
        flush();
        sendMessages(stack, 10);
        process(10, 200);
        checkReceived(network, stack, 0, 10);
        checkDelivered(stack, 0, 10);
    }
    
    @Test
    public void testPullableSend()
    {
        TestProtocolStack stack = network.getNodes().get(1);
        flush();
        TestFeed feed = new TestFeed(10, stack);
        stack.register(GroupMemberships.CORE_GROUP_ADDRESS, feed);
        process(10, 200);
        checkReceived(network, stack, 0, 5);
        checkDelivered(stack, 0, 5);
    }
    
    @Test
    public void testSendInNewMembership()
    {
        TestProtocolStack stack = network.getNodes().get(1);
        flush();
        sendMessages(stack, 10);
        startFlush();
        sendMessages(stack, 10);
        process(10, 200);
        flush();
        checkReceived(network, stack, 0, 20);
        checkDelivered(stack, 0, 20);
    }
    
    @Test
    public void testSenderFailure()
    {
        TestProtocolStack stack = network.getNodes().get(1);
        flush();
        sendMessages(stack, 10);
        process(10, 200, stack, com.exametrika.common.utils.Collections.asSet(network.getNodes().get(8),
            network.getNodes().get(9)));
        stack.setActive(false);
        updateFailureDetectors();
        flush();
        process(10, 200);
        checkReceived(network, stack, 0, 10);
    }
    
    @Test
    public void testReceiverFailure()
    {
        TestProtocolStack stack = network.getNodes().get(1);
        flush();
        sendMessages(stack, 10);
        process(10, 200, null, com.exametrika.common.utils.Collections.asSet(network.getNodes().get(8),
            network.getNodes().get(9)));
        network.getNodes().get(8).setActive(false);
        network.getNodes().get(9).setActive(false);
        updateFailureDetectors();
        flush();
        process(10, 200);
        checkReceived(network, stack, 0, 10);
        checkDelivered(stack, 0, 10);
    }
    
    @Test
    public void testReceiverOnFlushFailure()
    {
        TestProtocolStack stack = network.getNodes().get(1);
        flush();
        sendMessages(stack, 10);
        startFlush();
        network.getNodes().get(8).setActive(false);
        network.getNodes().get(9).setActive(false);
        updateFailureDetectors();
        process(10, 200);
        flush();
        process(10, 200);
        checkReceived(network, stack, 0, 10);
        checkDelivered(stack, 0, 10);
    }
    
    @Test
    public void testNonDurableFlushSend()
    {
        createNetwork(false);
        TestProtocolStack stack = network.getNodes().get(1);
        flush();
        sendMessages(stack, 10);
        process(10, 200, null, com.exametrika.common.utils.Collections.asSet(network.getNodes().get(8),
            network.getNodes().get(9)));
        network.getNodes().get(8).setActive(false);
        network.getNodes().get(9).setActive(false);
        updateFailureDetectors();
        flush();
        process(10, 200);
        checkReceived(network, stack, 0, 10);
    }
    
    @Test
    public void testTotalOrder()
    {
        TestProtocolStack stack1 = network.getNodes().get(1);
        TestProtocolStack stack2 = network.getNodes().get(2);
        flush();
        sendMessages(stack1, 10);
        sendMessages(stack2, 10);
        process(10, 200);
        checkOrder(network);
        checkReceived(network, stack1, 0, 10);
        checkReceived(network, stack2, 0, 10);
        checkDelivered(stack1, 0, 10);
        checkDelivered(stack2, 0, 10);
        clearReceived(network);
    }
    
    @Test
    public void testFlowControl()
    {
        testSimpleSend();
        TestProtocolStack sender = network.getNodes().get(1);
        checkLocalFlowControl(sender);
        checkRemoteFlowControl(network, sender);
    }
    
    private void createNetwork(boolean durable)
    {
        int maxBundlingMessageSize = 100;
        long maxBundlingPeriod = 100;
        int maxBundleSize = 200;
        int maxTotalOrderBundlingMessageCount = 2;
        long maxUnacknowledgedPeriod = 100;
        int maxUnacknowledgedMessageCount = 2;
        long maxIdleReceiveQueuePeriod = 10000;
        boolean ordered = durable;
        int maxUnlockQueueCapacity = 100;
        int minLockQueueCapacity = 200;
        List<TestProtocolStack> nodes = new ArrayList<TestProtocolStack>();
        for (int i = 0; i < COUNT; i++)
        {
            String channelName = "test" + (i + 1);
            TestInfo info = new TestInfo();
            TestProtocolStack stack = TestProtocolStack.create(channelName);
            stack.getSerializationRegistry().register(new TestMessagePartSerializer());
            stack.getSerializationRegistry().register(new GroupAddressSerializer());
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
        updateFailureDetectors();
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
    
    private void updateFailureDetectors()
    {
        List<INode> healthy = new ArrayList<INode>();
        Set<INode> failed = new HashSet<INode>();
        for (TestProtocolStack stack : network.getNodes())
        {
            TestInfo info = stack.getObject();
            if (stack.isActive())
                healthy.add(info.localNodeProvider.getLocalNode());
            else
                failed.add(info.localNodeProvider.getLocalNode());
        }
        
        for (TestProtocolStack stack : network.getNodes())
        {
            TestInfo info = stack.getObject();
            info.failureDetector.currentCoordinator = healthy.get(0);
            info.failureDetector.failedNodes = failed;
            info.failureDetector.healthyNodes = healthy;
            FailureAtomicMulticastProtocol protocol = stack.getProtocol();
            for (INode node : failed)
                protocol.onMemberFailed(node);
        }
    }
    
    private void startFlush()
    {
        IGroupMembership newMembership = createMembership(membershipId, network);
        flush = new FlushMock();
        flush.groupForming = membership == null;
        flush.oldMembership = membership;
        flush.newMembership = newMembership;
        prepareMembership(newMembership);
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
        
        process(10, 200);
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
        
        process(10, 200);
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
        
        process(10, 200);
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
        
        process(10, 200);
    }
    
    private void endFlush()
    {
        commitMembership();
        
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
        
        process(10, 200);
    }
    
    private void checkGrantFlush()
    {
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            
            FailureAtomicMulticastProtocol protocol = stack.getProtocol();
            assertTrue(flush.granted.contains(protocol));
        }
        flush.granted.clear();
    }
    
    private void flush()
    {
        startFlush();
        exchangeData();
        checkGrantFlush();
        beforeProcessFlush();
        processFlush();
        checkGrantFlush();
        endFlush();
    }
    
    private void prepareMembership(IGroupMembership membership)
    {
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            TestInfo info = stack.getObject();
            if (info.membershipManager.getPreparedMembership() != null && 
                info.membershipManager.getPreparedMembership().getId() == membership.getId())
                continue;
            
            if (membership.getId() == 1)
                info.membershipManager.prepareInstallMembership(membership);
            else
            {
                GroupChange groupChange = new GroupChange(membership.getGroup(), membership.getGroup(), 
                    Collections.<INode>emptyList(), Collections.<INode>emptySet(), Collections.<INode>emptySet());
                GroupMembershipChange membershipChange = new GroupMembershipChange(groupChange);
                info.membershipManager.prepareChangeMembership(membership, membershipChange);
            }
        }
    }
    
    private void commitMembership()
    {
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            
            TestInfo info = stack.getObject();
            info.membershipManager.commitMembership();
        }
    }
    
    private void checkReceived(TestNetwork network, TestProtocolStack sender, long startId, long endId)
    {
        TestInfo info = sender.getObject();
        INode senderNode = info.localNodeProvider.getLocalNode();
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            
            long id = startId;
            for (IMessage message : stack.getReceivedMessages())
            {
                if (!message.getSource().equals(senderNode.getAddress()))
                    continue;
                
                TestMessagePart part = message.getPart();
                assertThat(part.value, is(id));
                id++;
            }
            
            assertThat(id, is(endId));
        }
    }
    
    private void checkOrder(TestNetwork network)
    {
        int messageCount = -1;
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            
            if (messageCount == -1)
                messageCount = stack.getReceivedMessages().size();
            else
                assertThat(stack.getReceivedMessages().size(), is(messageCount));
        }
        
        for (int i = 0; i < messageCount; i++)
        {
            long id = -1;
            for (TestProtocolStack stack : network.getNodes())
            {
                if (!stack.isActive())
                    continue;
                
                IMessage message = stack.getReceivedMessages().get(i);
                TestMessagePart part = message.getPart();
                if (id == -1)
                    id = part.value;
                else
                    assertThat(part.value, is(id));
            }
        }
    }
    
    private void clearReceived(TestNetwork network)
    {
        for (TestProtocolStack stack : network.getNodes())
            stack.clearReceivedMessages();
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
    
    private void checkLocalFlowControl(TestProtocolStack stack)
    {
        TestInfo info = stack.getObject();
        assertTrue(!info.localFlowController.lockedFlows.isEmpty());
        assertThat(info.localFlowController.unlockedFlows.size(), is(info.localFlowController.lockedFlows.size()));
        assertThat(info.localFlowController.lockedFlows.get(0).getFlowId(), is(GroupMemberships.CORE_GROUP_ID));
        assertThat(info.localFlowController.unlockedFlows.get(0).getFlowId(), is(GroupMemberships.CORE_GROUP_ID));
    }
    
    private void checkRemoteFlowControl(TestNetwork network, TestProtocolStack sender)
    {
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            
            TestInfo info = stack.getObject();
            assertTrue(!info.remoteFlowController.lockedFlows.isEmpty());
            assertThat(info.remoteFlowController.unlockedFlows.size(), is(info.remoteFlowController.lockedFlows.size()));
            assertThat(info.remoteFlowController.lockedFlows.get(0).getFlowId(), is(GroupMemberships.CORE_GROUP_ID));
            assertThat(info.remoteFlowController.unlockedFlows.get(0).getFlowId(), is(GroupMemberships.CORE_GROUP_ID));
        }
    }
    
    private void process(int roundCount, long timeIncrement)
    {
        process(roundCount, timeIncrement, null, Collections.<TestProtocolStack>emptySet());
    }
    
    public void process(int roundCount, long timeIncrement, TestProtocolStack ignoredDestination, Set<TestProtocolStack> ignoredNodes)
    {
        long currentTime = Times.getCurrentTime() + timeIncrement;
        for (TestProtocolStack stack : network.getNodes())
        {
            if (!stack.isActive())
                continue;
            
            FailureAtomicMulticastProtocol protocol = stack.getProtocol();
            protocol.process();
        }
        network.onTimer(currentTime);
        network.process(roundCount, timeIncrement, ignoredDestination, ignoredNodes);   
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
