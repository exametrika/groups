package com.exametrika.tests.groups.load;

import java.util.HashSet;
import java.util.Set;

import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.discovery.WellKnownAddressesDiscoveryStrategy;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.tests.groups.channel.TestGroupChannel;
import com.exametrika.tests.groups.channel.TestGroupChannelFactory;
import com.exametrika.tests.groups.channel.TestGroupFactoryParameters;
import com.exametrika.tests.groups.channel.TestGroupParameters;

public class TestLoadGroupChannelFactory extends TestGroupChannelFactory
{
    public TestLoadGroupChannelFactory(TestGroupFactoryParameters factoryParameters)
    {
        super(factoryParameters);
    }
    
    public IChannel create(int index, TestLoadSpec loadSpec)
    {
        TestLoadMessageSender sender = new TestLoadMessageSender(index, loadSpec, GroupMemberships.CORE_GROUP_ADDRESS);
        
        TestLoadStateStore stateStore = new TestLoadStateStore(TestLoadMessageSender.createBuffer(index, getStateLength(loadSpec)));
        TestLoadStateTransferFactory stateTransferFactory = new TestLoadStateTransferFactory(stateStore);
        sender.setStateTransferFactory(stateTransferFactory);
        
        Set<String> wellKnownAddresses = new HashSet<String>();
        TestGroupParameters parameters = new TestGroupParameters();
        parameters.channelName = "test" + index;
        parameters.clientPart = true;
        parameters.serverPart = true;
        parameters.receiver = sender;
        parameters.discoveryStrategy = new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses);
        parameters.stateTransferFactory = stateTransferFactory;
        parameters.deliveryHandler = sender;
        parameters.localFlowController = sender;
        parameters.serializationRegistrars.add(new TestLoadMessagePartSerializer());
        
        TestGroupChannel channel = createChannel(parameters);
        sender.setChannel(channel);
        channel.start();
        sender.start();
        
        channel.getCompartment().addTimerProcessor(sender);
        return channel;
    }
    
    @Override
    protected void wireProtocols(IChannel channel, TcpTransport transport, ProtocolStack protocolStack)
    {
        super.wireProtocols(channel, transport, protocolStack);
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
