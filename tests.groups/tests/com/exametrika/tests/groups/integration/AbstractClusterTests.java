/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.integration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.groups.cluster.CoreNodeParameters;
import com.exametrika.api.groups.cluster.NodeParameters;
import com.exametrika.api.groups.cluster.WorkerNodeParameters;
import com.exametrika.common.l10n.SystemException;
import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannelFactory;
import com.exametrika.impl.groups.cluster.channel.WorkerNodeChannelFactory;
import com.exametrika.impl.groups.cluster.discovery.WellKnownAddressesDiscoveryStrategy;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.impl.groups.cluster.state.EmptySimpleStateStore;
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
    protected List<ICompositeChannel> coreChannels;
    protected List<ICompositeChannel> workerChannels;

    protected void createCluster(int coreNodeCount, int workerNodeCount)
    {
        coreChannels = new ArrayList<ICompositeChannel>();
        for (int i = 0; i < coreNodeCount; i++)
        {
            CoreNodeParameters parameters = createCoreNodeParameters(i, coreNodeCount);
            coreChannels.add(createCoreChannel(parameters));
        }
        
        workerChannels = new ArrayList<ICompositeChannel>();
        for (int i = 0; i < workerNodeCount; i++)
        {
            WorkerNodeParameters parameters = createWorkerNodeParameters(i, workerNodeCount);
            workerChannels.add(createWorkerChannel(parameters));
        }
    }
    
    protected void startCluster()
    {
        for (ICompositeChannel channel : coreChannels)
            channel.start();
        for (ICompositeChannel channel : workerChannels)
            channel.start();
    }
    
    protected void stopCluster()
    {
        for (ICompositeChannel channel : coreChannels)
            channel.stop();
        for (ICompositeChannel channel : workerChannels)
            channel.stop();
    }
    
    private ICompositeChannel createCoreChannel(CoreNodeParameters parameters)
    {
        CoreNodeChannelFactory factory = new CoreNodeChannelFactory();
        ICompositeChannel channel = factory.createChannel(parameters);
        disableCoreNodeProtocols(channel);
        return channel;
    }

    private ICompositeChannel createWorkerChannel(WorkerNodeParameters parameters)
    {
        WorkerNodeChannelFactory factory = new WorkerNodeChannelFactory();
        ICompositeChannel channel = factory.createChannel(parameters);
        disableWorkerNodeProtocols(channel);
        return channel;
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
        parameters.channelReconnector = null;
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
        for (int i = 0; i < count; i++)
            wellKnownAddresses.add("tcp://" + hostName + ":" + (portRangeStart + i));
        
       
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
}
