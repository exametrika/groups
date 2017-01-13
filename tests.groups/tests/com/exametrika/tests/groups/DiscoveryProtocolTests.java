/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Test;

import com.exametrika.api.groups.core.IMembership;
import com.exametrika.api.groups.core.IMembershipListener;
import com.exametrika.api.groups.core.IMembershipService;
import com.exametrika.api.groups.core.INode;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelFactory.FactoryParameters;
import com.exametrika.common.messaging.impl.ChannelFactory.Parameters;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.groups.core.discovery.DiscoveryProtocol;
import com.exametrika.impl.groups.core.discovery.GroupJoinMessagePart;
import com.exametrika.impl.groups.core.discovery.IGroupJoinStrategy;
import com.exametrika.impl.groups.core.discovery.WellKnownAddressesDiscoveryStrategy;
import com.exametrika.impl.groups.core.failuredetection.IFailureDetector;
import com.exametrika.impl.groups.core.membership.Group;
import com.exametrika.impl.groups.core.membership.Membership;
import com.exametrika.impl.groups.core.membership.MembershipSerializationRegistrar;
import com.exametrika.impl.groups.core.membership.Node;
import com.exametrika.spi.groups.IDiscoveryStrategy;
import com.exametrika.tests.common.messaging.ReceiverMock;

/**
 * The {@link DiscoveryProtocolTests} are tests for {@link DiscoveryProtocol}.
 * 
 * @see DiscoveryProtocol
 * @author Medvedev-A
 */
public class DiscoveryProtocolTests
{
    private static final int COUNT = 10;
    private IChannel[] channels = new IChannel[COUNT];
    
    @After
    public void tearDown()
    {
        for (IChannel channel : channels)
            IOs.close(channel);
    }
    
    @Test
    public void testGroupFormation()
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        for (int i = 0; i < COUNT; i++)
        {
            Parameters parameters = new Parameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            channel.start();
            wellKnownAddresses.add(channel.getLiveNodeProvider().getLocalNode().getConnection());
            channels[i] = channel;
        }
        
        for (DiscoveryProtocol protocol : channelFactory.protocols)
            protocol.startDiscovery();
        
        Threads.sleep(channelFactory.groupFormationPeriod + 500);
        
        Set<INode> discoveredNodes = null;
        for (int i = 0; i < COUNT; i++)
        {
            INode local = channelFactory.membershipServices.get(i).getLocalNode();
            
            DiscoveryProtocol protocol = channelFactory.protocols.get(i);
            Set<INode> nodes = new TreeSet(protocol.getDiscoveredNodes());
            assertTrue(!nodes.contains(local));
            nodes.add(local);
            if (discoveredNodes == null)
                discoveredNodes = nodes;
            else
                assertThat(discoveredNodes, is(nodes));
            
            INode first = discoveredNodes.iterator().next();
            assertThat(protocol.canFormGroup(), is(first.equals(local)));
        }
        
        assertThat(discoveredNodes.size(), is(COUNT));
    }
    
    @Test
    public void testGroupFormationWithChanges()
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        FactoryParameters factoryParameters = new FactoryParameters();
        factoryParameters.nodeCleanupPeriod = 1000;
        TestChannelFactory channelFactory = new TestChannelFactory(factoryParameters, new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        for (int i = 0; i < COUNT; i++)
        {
            Parameters parameters = new Parameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            if (i < COUNT - 2)
            {
                channel.start();
                wellKnownAddresses.add(channel.getLiveNodeProvider().getLocalNode().getConnection());
            }
            channels[i] = channel;
        }
        
        for (DiscoveryProtocol protocol : channelFactory.protocols)
            protocol.startDiscovery();
        
        Threads.sleep(channelFactory.groupFormationPeriod + 2000);
        
        IOs.close(channels[0]);
        IOs.close(channels[1]);
        channels[COUNT - 2].start();
        wellKnownAddresses.add(channels[COUNT - 2].getLiveNodeProvider().getLocalNode().getConnection());
        channels[COUNT - 1].start();
        wellKnownAddresses.add(channels[COUNT - 1].getLiveNodeProvider().getLocalNode().getConnection());
        
        Threads.sleep(channelFactory.groupFormationPeriod + 2000);
        
        Set<INode> discoveredNodes = null;
        for (int i = 2; i < COUNT; i++)
        {
            INode local = channelFactory.membershipServices.get(i).getLocalNode();
            
            DiscoveryProtocol protocol = channelFactory.protocols.get(i);
            Set<INode> nodes = new TreeSet(protocol.getDiscoveredNodes());
            assertTrue(!nodes.contains(local));
            nodes.add(local);
            if (discoveredNodes == null)
                discoveredNodes = nodes;
            else
                assertThat(discoveredNodes, is(nodes));
            
            INode first = discoveredNodes.iterator().next();
            assertThat(protocol.canFormGroup(), is(first.equals(local)));
        }
        
        assertThat(discoveredNodes.size(), is(COUNT - 2));
        assertTrue(!discoveredNodes.contains(channelFactory.membershipServices.get(0).getLocalNode()));
        assertTrue(!discoveredNodes.contains(channelFactory.membershipServices.get(1).getLocalNode()));
    }

    @Test
    public void testGroupFormationSingleNode()
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        Parameters parameters = new Parameters();
        parameters.channelName = "test0";
        parameters.clientPart = true;
        parameters.serverPart = true;
        parameters.receiver = new ReceiverMock();
        IChannel channel = channelFactory.createChannel(parameters);
        channel.start();
        wellKnownAddresses.add(channel.getLiveNodeProvider().getLocalNode().getConnection());
        channels[0] = channel;

        DiscoveryProtocol protocol = channelFactory.protocols.get(0); 
        protocol.startDiscovery();
        
        Threads.sleep(channelFactory.groupFormationPeriod + 500);
        
        assertThat(protocol.canFormGroup(), is(true));
        assertThat(protocol.getDiscoveredNodes().isEmpty(), is(true));
    }
    
    @Test
    public void testGroupJoin()
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        for (int i = 0; i < COUNT; i++)
        {
            Parameters parameters = new Parameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            if (i < 2)
                channel.start();
            channels[i] = channel;
        }
        
        IMembership membership = new Membership(1, new Group(UUID.randomUUID(), "test", true, Arrays.asList(
            channelFactory.membershipServices.get(0).getLocalNode(), channelFactory.membershipServices.get(1).getLocalNode())));
        channelFactory.failureDetectors.get(0).currentCoordinator = membership.getGroup().getCoordinator();
        channelFactory.failureDetectors.get(0).healthyMembers = membership.getGroup().getMembers();
        channelFactory.failureDetectors.get(1).currentCoordinator = membership.getGroup().getCoordinator();
        channelFactory.failureDetectors.get(1).healthyMembers = membership.getGroup().getMembers();

        wellKnownAddresses.add(membership.getGroup().getCoordinator().getAddress().getConnection());
        
        channelFactory.protocols.get(0).onPreparedMembershipChanged(null, membership, null);
        channelFactory.protocols.get(1).onPreparedMembershipChanged(null, membership, null);
        
        for (int i = 2; i < COUNT; i++)
            channels[i].start();

        for (DiscoveryProtocol protocol : channelFactory.protocols)
            protocol.startDiscovery();
        
        Threads.sleep(channelFactory.groupFormationPeriod + 500);
        
        Set<INode> discoveredNodes = null;
        for (int i = 0; i < COUNT; i++)
        {
            DiscoveryProtocol protocol = channelFactory.protocols.get(i);
            assertThat(protocol.canFormGroup(), is(false));
            if (i > 0)
                assertThat(protocol.getDiscoveredNodes(), is(Collections.<INode>emptySet()));
            else
                discoveredNodes = protocol.getDiscoveredNodes();
            
            if (i > 1)
                assertTrue(discoveredNodes.contains(channelFactory.membershipServices.get(i).getLocalNode()));
        }
        
        assertThat(discoveredNodes.size(), is(COUNT - 2));
    }
    
    @Test
    public void testGroupJoinWithRedirect()
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        for (int i = 0; i < COUNT; i++)
        {
            Parameters parameters = new Parameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            if (i < 2)
                channel.start();
            channels[i] = channel;
        }
        
        IMembership membership = new Membership(1, new Group(UUID.randomUUID(), "test", true, Arrays.asList(
            channelFactory.membershipServices.get(0).getLocalNode(), channelFactory.membershipServices.get(1).getLocalNode())));
        channelFactory.failureDetectors.get(0).currentCoordinator = membership.getGroup().getCoordinator();
        channelFactory.failureDetectors.get(0).healthyMembers = membership.getGroup().getMembers();
        channelFactory.failureDetectors.get(1).currentCoordinator = membership.getGroup().getCoordinator();
        channelFactory.failureDetectors.get(1).healthyMembers = membership.getGroup().getMembers();

        wellKnownAddresses.add(membership.getGroup().getMembers().get(1).getAddress().getConnection());
        
        channelFactory.protocols.get(0).onPreparedMembershipChanged(null, membership, null);
        channelFactory.protocols.get(1).onPreparedMembershipChanged(null, membership, null);
        
        for (int i = 2; i < COUNT; i++)
            channels[i].start();

        for (DiscoveryProtocol protocol : channelFactory.protocols)
            protocol.startDiscovery();
        
        Threads.sleep(channelFactory.groupFormationPeriod + 500);
        
        Set<INode> discoveredNodes = null;
        for (int i = 0; i < COUNT; i++)
        {
            DiscoveryProtocol protocol = channelFactory.protocols.get(i);
            assertThat(protocol.canFormGroup(), is(false));
            if (i > 0)
                assertThat(protocol.getDiscoveredNodes(), is(Collections.<INode>emptySet()));
            else
                discoveredNodes = protocol.getDiscoveredNodes();
            
            if (i > 1)
                assertTrue(discoveredNodes.contains(channelFactory.membershipServices.get(i).getLocalNode()));
        }
        
        assertThat(discoveredNodes.size(), is(COUNT - 2));
    }

    @Test
    public void testGroupJoinWithCoordinatorFailure()
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        TestChannelFactory channelFactory = new TestChannelFactory(new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        for (int i = 0; i < COUNT; i++)
        {
            Parameters parameters = new Parameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            if (i < 2)
            {
                channel.start();
                wellKnownAddresses.add(channel.getLiveNodeProvider().getLocalNode().getConnection());
            }
            channels[i] = channel;
        }

        
        IMembership membership = new Membership(1, new Group(UUID.randomUUID(), "test", true, Arrays.asList(
            channelFactory.membershipServices.get(0).getLocalNode(), channelFactory.membershipServices.get(1).getLocalNode())));
        channelFactory.failureDetectors.get(0).currentCoordinator = membership.getGroup().getCoordinator();
        channelFactory.failureDetectors.get(0).healthyMembers = membership.getGroup().getMembers();
        channelFactory.failureDetectors.get(1).currentCoordinator = membership.getGroup().getCoordinator();
        channelFactory.failureDetectors.get(1).healthyMembers = membership.getGroup().getMembers();

        channelFactory.protocols.get(0).onPreparedMembershipChanged(null, membership, null);
        channelFactory.protocols.get(1).onPreparedMembershipChanged(null, membership, null);
        
        for (int i = 2; i < COUNT; i++)
        {
            channels[i].start();
            wellKnownAddresses.add(channels[i].getLiveNodeProvider().getLocalNode().getConnection());
        }

        for (DiscoveryProtocol protocol : channelFactory.protocols)
            protocol.startDiscovery();
        
        Threads.sleep(channelFactory.groupFormationPeriod + 500);
        
        channelFactory.failureDetectors.get(1).currentCoordinator = channelFactory.membershipServices.get(1).getLocalNode();
        channelFactory.failureDetectors.get(1).healthyMembers = Arrays.asList(channelFactory.membershipServices.get(1).getLocalNode());
        IOs.close(channels[0]);
        
        Threads.sleep(channelFactory.groupFormationPeriod + 500);
        
        Set<INode> discoveredNodes = null;
        for (int i = 0; i < COUNT; i++)
        {
            DiscoveryProtocol protocol = channelFactory.protocols.get(i);
            assertThat(protocol.canFormGroup(), is(false));
            if (i > 1)
                assertThat(protocol.getDiscoveredNodes(), is(Collections.<INode>emptySet()));
            else if (i == 1)
                discoveredNodes = protocol.getDiscoveredNodes();
            
            if (i > 1)
                assertTrue(discoveredNodes.contains(channelFactory.membershipServices.get(i).getLocalNode()));
        }
        
        assertThat(discoveredNodes.size(), is(COUNT - 2));
    }
    
    @Test
    public void testGroupJoinWithGroupFailure()
    {
        Set<String> wellKnownAddresses = new ConcurrentHashMap<String, String>().keySet("");
        FactoryParameters factoryParameters = new FactoryParameters();
        factoryParameters.nodeCleanupPeriod = 1000;
        TestChannelFactory channelFactory = new TestChannelFactory(factoryParameters, new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses));
        for (int i = 0; i < COUNT; i++)
        {
            Parameters parameters = new Parameters();
            parameters.channelName = "test" + i;
            parameters.clientPart = true;
            parameters.serverPart = true;
            parameters.receiver = new ReceiverMock();
            IChannel channel = channelFactory.createChannel(parameters);
            if (i < 2)
            {
                channel.start();
                wellKnownAddresses.add(channel.getLiveNodeProvider().getLocalNode().getConnection());
            }
            channels[i] = channel;
        }
        
        IMembership membership = new Membership(1, new Group(UUID.randomUUID(), "test", true, Arrays.asList(
            channelFactory.membershipServices.get(0).getLocalNode(), channelFactory.membershipServices.get(1).getLocalNode())));
        channelFactory.failureDetectors.get(0).currentCoordinator = membership.getGroup().getCoordinator();
        channelFactory.failureDetectors.get(0).healthyMembers = membership.getGroup().getMembers();
        channelFactory.failureDetectors.get(1).currentCoordinator = membership.getGroup().getCoordinator();
        channelFactory.failureDetectors.get(1).healthyMembers = membership.getGroup().getMembers();

        wellKnownAddresses.add(membership.getGroup().getCoordinator().getAddress().getConnection());
        
        channelFactory.protocols.get(0).onPreparedMembershipChanged(null, membership, null);
        channelFactory.protocols.get(1).onPreparedMembershipChanged(null, membership, null);
        
        for (int i = 2; i < COUNT; i++)
        {
            channels[i].start();
            wellKnownAddresses.add(channels[i].getLiveNodeProvider().getLocalNode().getConnection());
        }

        for (DiscoveryProtocol protocol : channelFactory.protocols)
            protocol.startDiscovery();
        
        Threads.sleep(channelFactory.groupFormationPeriod + 5000);
        
        IOs.close(channels[0]);
        IOs.close(channels[1]);
        
        Threads.sleep(channelFactory.groupFormationPeriod + 5000);
        
        Set<INode> discoveredNodes = null;
        for (int i = 2; i < COUNT; i++)
        {
            INode local = channelFactory.membershipServices.get(i).getLocalNode();
            
            DiscoveryProtocol protocol = channelFactory.protocols.get(i);
            Set<INode> nodes = new TreeSet(protocol.getDiscoveredNodes());
            assertTrue(!nodes.contains(local));
            nodes.add(local);
            if (discoveredNodes == null)
                discoveredNodes = nodes;
            else
                assertThat(discoveredNodes, is(nodes));
            
            INode first = discoveredNodes.iterator().next();
            
            assertThat(protocol.canFormGroup(), is(first.equals(local)));
            assertTrue(channelFactory.joinStrategies.get(i).groupFailed);
        }
        
        assertThat(discoveredNodes.size(), is(COUNT - 2));
    }
    
    public static class GroupJoinStrategyMock implements IGroupJoinStrategy
    {
        public boolean groupFailed;
        public IMembershipService membershipService;
        public DiscoveryProtocol protocol;
        public IMessageFactory messageFactory;

        @Override
        public void onGroupDiscovered(List<IAddress> healthyMembers)
        {
            protocol.send(messageFactory.create(healthyMembers.get(0), new GroupJoinMessagePart(membershipService.getLocalNode())));
        }

        @Override
        public void onGroupFailed()
        {
            groupFailed = true;
        }
    }
    
    private static class MembershipServiceMock implements IMembershipService
    {
        private Node localNode;
        private Membership membership;
        private ILiveNodeProvider provider;
        private String name;
        
        public MembershipServiceMock(String name, ILiveNodeProvider provider)
        {
            this.name = name;
            this.provider = provider;
        }
        
        @Override
        public synchronized INode getLocalNode()
        {
            if (localNode == null)
                localNode = new Node(name, provider.getLocalNode(), Collections.<String, Object>emptyMap());
            
            return localNode;
        }

        @Override
        public synchronized IMembership getMembership()
        {
            return membership;
        }

        @Override
        public void addMembershipListener(IMembershipListener listener)
        {
        }

        @Override
        public void removeMembershipListener(IMembershipListener listener)
        {
        }

        @Override
        public void removeAllMembershipListeners()
        {
        }
    }
    
    private static class FailureDetectorMock implements IFailureDetector
    {
        private INode currentCoordinator;
        private List<INode> healthyMembers = new ArrayList<INode>();
        
        @Override
        public synchronized INode getCurrentCoordinator()
        {
            return currentCoordinator;
        }

        @Override
        public synchronized List<INode> getHealthyMembers()
        {
            return healthyMembers;
        }

        @Override
        public Set<INode> getFailedMembers()
        {
            return Collections.emptySet();
        }

        @Override
        public Set<INode> getLeftMembers()
        {
            return Collections.emptySet();
        }

        @Override
        public void addFailedMembers(Set<UUID> memberIds)
        {
        }

        @Override
        public void addLeftMembers(Set<UUID> memberIds)
        {
        }
    }
    
    private class TestChannelFactory extends ChannelFactory
    {
        private final IDiscoveryStrategy discoveryStrategy;
        private final long discoveryPeriod = 200;
        private final long groupFormationPeriod = 2000;
        private List<DiscoveryProtocol> protocols = new ArrayList<DiscoveryProtocol>();
        private List<GroupJoinStrategyMock> joinStrategies = new ArrayList<GroupJoinStrategyMock>();
        private List<MembershipServiceMock> membershipServices = new ArrayList<MembershipServiceMock>();
        private List<FailureDetectorMock> failureDetectors = new ArrayList<FailureDetectorMock>();
        
        public TestChannelFactory(FactoryParameters factoryParameters, IDiscoveryStrategy discoveryStrategy)
        {
            super(factoryParameters);
            this.discoveryStrategy = discoveryStrategy;
        }
        
        public TestChannelFactory(IDiscoveryStrategy discoveryStrategy)
        {
            this(new FactoryParameters(Debug.isDebug()), discoveryStrategy);
        }
        
        @Override
        protected void createProtocols(Parameters parameters, String channelName, IMessageFactory messageFactory, 
            ISerializationRegistry serializationRegistry, ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers,
            List<AbstractProtocol> protocols)
        {
            GroupJoinStrategyMock joinStrategy = new GroupJoinStrategyMock();
            joinStrategies.add(joinStrategy);
            MembershipServiceMock membershipService = new MembershipServiceMock(channelName, liveNodeProvider);
            membershipServices.add(membershipService);
            FailureDetectorMock failureDetector = new FailureDetectorMock();
            failureDetectors.add(failureDetector);
            
            DiscoveryProtocol discoveryProtocol = new DiscoveryProtocol(channelName, messageFactory, membershipService, 
                failureDetector, discoveryStrategy, liveNodeProvider, joinStrategy, discoveryPeriod, groupFormationPeriod);
            protocols.add(discoveryProtocol);
            
            this.protocols.add(discoveryProtocol);
            
            joinStrategy.protocol = discoveryProtocol;
            joinStrategy.membershipService = membershipService;
            joinStrategy.messageFactory = messageFactory;
            
            serializationRegistry.register(new MembershipSerializationRegistrar());
        }
    }
}
