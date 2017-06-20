/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.groups.cluster.CoreNodeParameters;
import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IGroup;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.IGroupsMembership;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.api.groups.cluster.INodesMembership;
import com.exametrika.api.groups.cluster.IWorkerNodeChannel;
import com.exametrika.api.groups.cluster.NodeParameters;
import com.exametrika.api.groups.cluster.WorkerNodeParameters;
import com.exametrika.common.l10n.SystemException;
import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.messaging.impl.SubChannel;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.channel.CoreGroupSubChannel;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannel;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannelFactory;
import com.exametrika.impl.groups.cluster.channel.WorkerNodeChannel;
import com.exametrika.impl.groups.cluster.channel.WorkerNodeChannelFactory;
import com.exametrika.impl.groups.cluster.discovery.WellKnownAddressesDiscoveryStrategy;
import com.exametrika.impl.groups.cluster.management.CommandManager;
import com.exametrika.impl.groups.cluster.membership.GroupAddress;
import com.exametrika.impl.groups.cluster.membership.GroupDefinition;
import com.exametrika.impl.groups.cluster.membership.GroupMembershipManager;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.membership.GroupProtocolSubStack;
import com.exametrika.impl.groups.cluster.membership.WorkerGroupMembershipProtocol;
import com.exametrika.impl.groups.cluster.membership.WorkerToCoreMembership;
import com.exametrika.impl.groups.cluster.state.EmptySimpleStateStore;
import com.exametrika.spi.groups.cluster.channel.IChannelReconnector;
import com.exametrika.tests.common.messaging.ReceiverMock;
import com.exametrika.tests.groups.load.TestLoadMessagePartSerializer;
import com.exametrika.tests.groups.load.TestLoadMessageSender;
import com.exametrika.tests.groups.load.TestLoadSpec;
import com.exametrika.tests.groups.load.TestLoadSpec.SendFrequencyType;
import com.exametrika.tests.groups.load.TestLoadSpec.SendSourceType;
import com.exametrika.tests.groups.load.TestLoadSpec.SendType;
import com.exametrika.tests.groups.load.TestLoadSpec.SizeType;
import com.exametrika.tests.groups.load.TestLoadStateStore;
import com.exametrika.tests.groups.load.TestLoadStateTransferFactory;

public abstract class AbstractClusterTests
{
    protected List<CoreNodeChannel> coreChannels;
    protected List<WorkerNodeChannel> workerChannels;
    protected Set<Integer> reconnections = new HashSet<Integer>();
    protected Set<Integer> wellKnownAddressesIndexes;
    protected TestLoadSpec loadSpec = new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
        SendType.DIRECT, SendSourceType.SINGLE_NODE);;
    protected GroupAddress groupAddress = GroupMemberships.CORE_GROUP_ADDRESS;

    protected void createCluster(int coreNodeCount, int workerNodeCount)
    {
        coreChannels = new ArrayList<CoreNodeChannel>();
        for (int i = 0; i < coreNodeCount; i++)
        {
            CoreNodeParameters parameters = createCoreNodeParameters(i, coreNodeCount);
            coreChannels.add(createCoreChannel(parameters));
        }
        
        workerChannels = new ArrayList<WorkerNodeChannel>();
        for (int i = 0; i < workerNodeCount; i++)
        {
            WorkerNodeParameters parameters = createWorkerNodeParameters(i, workerNodeCount);
            workerChannels.add(createWorkerChannel(parameters));
        }
    }
    
    protected void startCoreNodes(Set<Integer> ignoredNodes)
    {
        for (int i = 0; i < coreChannels.size(); i++)
        {
            if (ignoredNodes != null && ignoredNodes.contains(i))
                continue;
            
            ICompositeChannel channel = coreChannels.get(i);
            channel.start();
        }
    }
    
    protected void startWorkerNodes(Set<Integer> ignoredNodes)
    {
        for (int i = 0; i < workerChannels.size(); i++)
        {
            if (ignoredNodes != null && ignoredNodes.contains(i))
                continue;
            
            ICompositeChannel channel = workerChannels.get(i);
            channel.start();
        }
    }
    
    protected void stopCluster()
    {
        for (ICompositeChannel channel : coreChannels)
            channel.stop();
        for (ICompositeChannel channel : workerChannels)
            channel.stop();
    }

    protected CommandManager findCommandManager(CoreNodeChannel coreNode)
    {
        return ((SubChannel)coreNode.getMainSubChannel()).getProtocolStack().find(CommandManager.class);
    }
    
    protected void checkClusterWorkerNodesMembership(Set<Integer> ignoredNodes)
    {
        List<INode> nodes = null;
        synchronized (workerChannels)
        {
            for (int i = 0; i < workerChannels.size(); i++)
            {
                if (ignoredNodes != null && ignoredNodes.contains(i))
                    continue;
                
                IWorkerNodeChannel channel = workerChannels.get(i);
                IClusterMembershipService membershipService = channel.getMembershipService();
                IClusterMembership membership = membershipService.getMembership();
                IDomainMembership domainMembership = membership.findDomain("test");
                INodesMembership nodesMembership = domainMembership.findElement(INodesMembership.class);
                if (nodes == null)
                    nodes = nodesMembership.getNodes();
                else
                    assertThat(nodesMembership.getNodes(), is(nodes));
                
                assertTrue(nodes.contains(membershipService.getLocalNode()));
            }
            
            for (int i = 0; i < workerChannels.size(); i++)
            {
                if (ignoredNodes == null || !ignoredNodes.contains(i))
                    continue;
                
                IWorkerNodeChannel channel = workerChannels.get(i);
                IClusterMembershipService membershipService = channel.getMembershipService();
                assertTrue(!nodes.contains(membershipService.getLocalNode()));
            }
        }
    }
    
    protected Map<UUID, GroupDefinition> buildGroupDefinitionsMap(List<GroupDefinition> groupDefinitions)
    {
        Map<UUID, GroupDefinition> map = new HashMap<UUID, GroupDefinition>();
        for (GroupDefinition groupDefinition : groupDefinitions)
            map.put(groupDefinition.getId(), groupDefinition);
        
        return map;
    }
    
    protected void checkClusterGroupsMembership(Map<UUID, GroupDefinition> groupDefinitions,
        Set<Integer> ignoredNodes)
    {
        Set<INode> nodes = new HashSet<INode>();
        List<IGroup> groups = null;
        synchronized (workerChannels)
        {
            for (int i = 0; i < workerChannels.size(); i++)
            {
                IWorkerNodeChannel channel = workerChannels.get(i);
                IClusterMembershipService membershipService = channel.getMembershipService();
                IClusterMembership membership = membershipService.getMembership();
                IDomainMembership domainMembership = membership.findDomain("test");
                IGroupsMembership groupsMembership = domainMembership.findElement(IGroupsMembership.class);
                if (groups == null)
                    groups = groupsMembership.getGroups();
                else
                    assertThat(groupsMembership.getGroups(), is(groups));
            }
            
            assertThat(groups.size(), is(groupDefinitions.size()));
            for (IGroup group : groups)
            {
                nodes.addAll(group.getMembers());
                GroupDefinition groupDefinition = groupDefinitions.get(group.getId());
                assertThat(group.getName(), is(groupDefinition.getName()));
                assertThat(group.getOptions(), is(groupDefinition.getOptions()));
                assertThat(group.getMembers().size(), is(groupDefinition.getNodeCount()));
            }
        }
        
        for (int i = 0; i < workerChannels.size(); i++)
        {
            IWorkerNodeChannel channel = workerChannels.get(i);
            if (ignoredNodes != null && ignoredNodes.contains(i))
                assertTrue(!nodes.contains(channel.getMembershipService().getLocalNode()));
            else
                assertTrue(nodes.contains(channel.getMembershipService().getLocalNode()));
        }
    }
    
    protected void checkWorkerGroupsMembership(Map<UUID, GroupDefinition> groupDefinitions, Set<Integer> ignoredNodes) throws Throwable
    {
        Set<INode> nodes = new HashSet<INode>();
        synchronized (workerChannels)
        {
            IWorkerNodeChannel channel = workerChannels.get(0);
            IClusterMembershipService membershipService = channel.getMembershipService();
            IClusterMembership membership = membershipService.getMembership();
            IDomainMembership domainMembership = membership.findDomain("test");
            IGroupsMembership groupsMembership = domainMembership.findElement(IGroupsMembership.class);
            List<IGroup> groups = groupsMembership.getGroups();
            
            for (IGroup group : groups)
            {
                nodes.addAll(group.getMembers());
                IGroupMembership groupMembership = null;
                for (INode node : group.getMembers())
                {
                    WorkerNodeChannel workerChannel = findWorker(node);
                    IGroupMembership workerGroupMembership = findGroupMembership(workerChannel, group.getId());
                    if (groupMembership == null)
                        groupMembership = workerGroupMembership;
                    else
                        checkGroupMembership(groupMembership, workerGroupMembership);
                        
                    assertTrue(groupMembership.getGroup().findMember(workerChannel.getMembershipService().getLocalNode().getId()) != null);
                }
                
                checkGroup(groupMembership.getGroup(), group);
            }
        }
        
        for (int i = 0; i < workerChannels.size(); i++)
        {
            IWorkerNodeChannel channel = workerChannels.get(i);
            if (ignoredNodes != null && ignoredNodes.contains(i))
                assertTrue(!nodes.contains(channel.getMembershipService().getLocalNode()));
            else
                assertTrue(nodes.contains(channel.getMembershipService().getLocalNode()));
        }
        
        checkClusterWorkerNodesMembership(ignoredNodes);
        checkClusterGroupsMembership(groupDefinitions, ignoredNodes);
    }
    
    private void checkGroup(IGroup first, IGroup second)
    {
        assertThat(first.getId(), is(second.getId()));
        assertThat(first.getName(), is(second.getName()));
        assertThat(first.getAddress(), is(second.getAddress()));
        assertThat(first.getOptions(), is(second.getOptions()));
        assertThat(first.isPrimary(), is(second.isPrimary()));
        assertThat(first.getMembers(), is(second.getMembers()));
    }

    private void checkGroupMembership(IGroupMembership first, IGroupMembership second)
    {
        assertThat(first.getId(), is(second.getId()));
        checkGroup(first.getGroup(), second.getGroup());
    }

    protected WorkerNodeChannel findWorker(INode node)
    {
        for (WorkerNodeChannel worker : workerChannels)
        {
            if (worker.getMembershipService().getLocalNode().equals(node))
                return worker;
        }
        
        return null;
    }
    
    protected IGroupMembership findGroupMembership(WorkerNodeChannel workerNode, UUID groupId) throws Throwable
    {
        WorkerGroupMembershipProtocol groupMembershipProtocol = ((SubChannel)workerNode.getMainSubChannel()
            ).getProtocolStack().find(WorkerGroupMembershipProtocol.class);
        Map<UUID, GroupProtocolSubStack> groupsStacks = Tests.get(groupMembershipProtocol, "groupStacks");
        GroupProtocolSubStack stack = groupsStacks.get(groupId);
        if (stack == null)
            return null;
        GroupMembershipManager groupMembershipManager = Tests.get(stack, "membershipManager");
        return groupMembershipManager.getMembership();
    }
    
    protected void checkWorkerReconnections(Set<Integer> ignoredNodes)
    {
        synchronized (workerChannels)
        {
            for (int i = 0; i < workerChannels.size(); i++)
            {
                if (ignoredNodes != null && ignoredNodes.contains(i))
                    continue;
                
                reconnections.contains(i);
            }
        }
    }
    
    protected int findCoreChannel(INode coreNode)
    {
        for (int i = 0; i < coreChannels.size(); i++)
        {
            if (coreChannels.get(i).getMembershipService().getLocalNode().equals(coreNode))
                return i;
        }
        
        return Assert.error();
    }
    
    protected int findCoordinator()
    {
        CoreGroupSubChannel subChannel = (CoreGroupSubChannel)coreChannels.get(0).getSubChannels().get(0);
        IGroupMembership membership = subChannel.getMembershipService().getMembership();
        return findCoreChannel(membership.getGroup().getCoordinator());
    }
    
    protected int findController(INode workerNode)
    {
        IClusterMembership membership = coreChannels.get(0).getMembershipService().getMembership();
        WorkerToCoreMembership mapping = membership.findDomain("test").findElement(WorkerToCoreMembership.class);
        INode controller = mapping.findCoreNode(workerNode);
        return findCoreChannel(controller);
    }
    
    protected Set<Integer> findNonCoordinators(int count)
    {
        Set<Integer> set = new HashSet<Integer>();
        CoreGroupSubChannel subChannel = (CoreGroupSubChannel)coreChannels.get(0).getSubChannels().get(0);
        IGroupMembership membership = subChannel.getMembershipService().getMembership();
        List<INode> nodes = membership.getGroup().getMembers();
        for (int i = 0; i < nodes.size(); i++)
        {
            INode node = nodes.get(i);
            if (node.equals(membership.getGroup().getCoordinator()))
                continue;
           
            set.add(findCoreChannel(node));
            if (set.size() >= count)
                break;
        }
        
        return set;
    }
    
    protected CoreNodeParameters createCoreNodeParameters(int index, int count)
    {
        int portRangeStart = 17000;
        CoreNodeParameters parameters = createCoreNodeParameters();
        setNodeParameters(parameters, portRangeStart, index, count);
        parameters.stateStore = new EmptySimpleStateStore();
        return parameters;
    }

    protected CoreNodeParameters createCoreNodeParameters()
    {
        CoreNodeParameters parameters = new CoreNodeParameters();
        return parameters;
    }
    
    protected WorkerNodeParameters createWorkerNodeParameters(int index, int count)
    {
        TestLoadMessageSender sender = new TestLoadMessageSender(index, loadSpec, groupAddress);
        
        TestLoadStateStore stateStore = new TestLoadStateStore(TestLoadMessageSender.createBuffer(index, getStateLength(loadSpec)));
        TestLoadStateTransferFactory stateTransferFactory = new TestLoadStateTransferFactory(stateStore);
        sender.setStateTransferFactory(stateTransferFactory);
        
        int portRangeStart = 17000;
        WorkerNodeParameters parameters = createWorkerNodeParameters();
        setNodeParameters(parameters, portRangeStart, index, count);
        parameters.stateTransferFactory = stateTransferFactory;
        parameters.receiver = sender;
        parameters.stateTransferFactory = stateTransferFactory;
        parameters.deliveryHandler = sender;
        parameters.localFlowController = sender;
        parameters.serializationRegistrars.add(new TestLoadMessagePartSerializer());
        parameters.channelReconnector = new TestWorkerReconnector(index, count);
        return parameters;
    }

    protected WorkerNodeParameters createWorkerNodeParameters()
    {
        WorkerNodeParameters parameters = new WorkerNodeParameters();
        return parameters;
    }
    
    protected void setNodeParameters(NodeParameters parameters, int portRangeStart, int index, int count)
    {
        String hostName;
        try
        {
            hostName = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            throw new SystemException(e);
        }
        
        Set<String> wellKnownAddresses = new HashSet<String>();
        if (wellKnownAddressesIndexes == null || parameters instanceof CoreNodeParameters)
        {
            for (int i = 0; i < count; i++)
                wellKnownAddresses.add("tcp://" + hostName + ":" + (portRangeStart + i));
        }
        else
        {
            for (int i : wellKnownAddressesIndexes)
                wellKnownAddresses.add("tcp://" + hostName + ":" + (portRangeStart + i));
        }
       
        parameters.channelName = "test" + index;
        parameters.clientPart = true;
        parameters.serverPart = true;
        parameters.portRangeStart = portRangeStart + index;
        parameters.portRangeStart = parameters.portRangeEnd;
        parameters.receiver = new ReceiverMock();
        parameters.discoveryStrategy = new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses);
    }
    
    protected void disableCoreNodeProtocols(ICompositeChannel coreChannel)
    {
    }
    
    protected void disableWorkerNodeProtocols(ICompositeChannel workerChannel)
    {
    }
    
    protected CoreNodeChannel createCoreChannel(CoreNodeParameters parameters)
    {
        CoreNodeChannelFactory factory = createCoreNodeFactory();
        ICompositeChannel channel = factory.createChannel(parameters);
        disableCoreNodeProtocols(channel);
        return (CoreNodeChannel)channel;
    }

    protected CoreNodeChannelFactory createCoreNodeFactory()
    {
        CoreNodeChannelFactory factory = new CoreNodeChannelFactory();
        return factory;
    }

    protected WorkerNodeChannel createWorkerChannel(WorkerNodeParameters parameters)
    {
        WorkerNodeChannelFactory factory = createWorkerNodeFactory();
        ICompositeChannel channel = factory.createChannel(parameters);
        TestLoadMessageSender sender = (TestLoadMessageSender)parameters.receiver;
        sender.setChannel(channel.getMainSubChannel());
        channel.getCompartment().addTimerProcessor(sender);
        disableWorkerNodeProtocols(channel);
        return (WorkerNodeChannel)channel;
    }

    protected WorkerNodeChannelFactory createWorkerNodeFactory()
    {
        WorkerNodeChannelFactory factory = new WorkerNodeChannelFactory();
        return factory;
    }
    
    protected int getStateLength(TestLoadSpec loadSpec)
    {
        switch (loadSpec.getStateSizeType())
        {
        case SMALL:
            return 1000;
        case MEDIUM:
            return 100000;
        case LARGE:
            return 10000000;
        case SET:
            return loadSpec.getStateSize();
        default:
            return Assert.error();
        }
    }
    
    protected class TestWorkerReconnector implements IChannelReconnector
    {
        private int index;
        private int count;
        
        public TestWorkerReconnector(int index, int count)
        {
            this.index = index;
            this.count = count;
        }
        
        @Override
        public void reconnect()
        {
            WorkerNodeParameters parameters = createWorkerNodeParameters(index, count);
            WorkerNodeChannel channel = createWorkerChannel(parameters);
            synchronized (workerChannels)
            {
                workerChannels.set(index, channel);
                reconnections.add(index);
            }
        }
    }
}
