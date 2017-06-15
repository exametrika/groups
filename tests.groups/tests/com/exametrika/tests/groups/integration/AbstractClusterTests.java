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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.CoreNodeParameters;
import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.api.groups.cluster.IClusterMembershipService;
import com.exametrika.api.groups.cluster.IDomainMembership;
import com.exametrika.api.groups.cluster.IGroupMembership;
import com.exametrika.api.groups.cluster.INode;
import com.exametrika.api.groups.cluster.INodesMembership;
import com.exametrika.api.groups.cluster.IWorkerNodeChannel;
import com.exametrika.api.groups.cluster.NodeParameters;
import com.exametrika.api.groups.cluster.WorkerNodeParameters;
import com.exametrika.common.l10n.SystemException;
import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.channel.CoreGroupSubChannel;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannel;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannelFactory;
import com.exametrika.impl.groups.cluster.channel.WorkerNodeChannel;
import com.exametrika.impl.groups.cluster.channel.WorkerNodeChannelFactory;
import com.exametrika.impl.groups.cluster.discovery.WellKnownAddressesDiscoveryStrategy;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
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
    
    protected void checkWorkerNodesMembership(Set<Integer> ignoredNodes)
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
        CoreNodeParameters parameters = new CoreNodeParameters();
        setNodeParameters(parameters, portRangeStart, index, count);
        parameters.stateStore = new EmptySimpleStateStore();
        return parameters;
    }
    
    protected WorkerNodeParameters createWorkerNodeParameters(int index, int count)
    {
        TestLoadSpec loadSpec = new TestLoadSpec(SizeType.SMALL, 0, SizeType.SMALL, 0, SendFrequencyType.MAXIMUM, 0d, 
            SendType.DIRECT, SendSourceType.SINGLE_NODE);
        
        TestLoadMessageSender sender = new TestLoadMessageSender(index, loadSpec, GroupMemberships.CORE_GROUP_ADDRESS);
        
        TestLoadStateStore stateStore = new TestLoadStateStore(TestLoadMessageSender.createBuffer(index, getStateLength(loadSpec)));
        TestLoadStateTransferFactory stateTransferFactory = new TestLoadStateTransferFactory(stateStore);
        sender.setStateTransferFactory(stateTransferFactory);
        
        int portRangeStart = 17000;
        WorkerNodeParameters parameters = new WorkerNodeParameters();
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
    
    private CoreNodeChannel createCoreChannel(CoreNodeParameters parameters)
    {
        CoreNodeChannelFactory factory = new CoreNodeChannelFactory();
        ICompositeChannel channel = factory.createChannel(parameters);
        disableCoreNodeProtocols(channel);
        return (CoreNodeChannel)channel;
    }

    private WorkerNodeChannel createWorkerChannel(WorkerNodeParameters parameters)
    {
        WorkerNodeChannelFactory factory = new WorkerNodeChannelFactory();
        ICompositeChannel channel = factory.createChannel(parameters);
        disableWorkerNodeProtocols(channel);
        return (WorkerNodeChannel)channel;
    }
    
    private int getStateLength(TestLoadSpec loadSpec)
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
    
    private class TestWorkerReconnector implements IChannelReconnector
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
