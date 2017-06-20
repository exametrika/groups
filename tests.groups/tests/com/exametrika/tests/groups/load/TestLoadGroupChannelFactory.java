package com.exametrika.tests.groups.load;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.SystemException;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.composite.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.discovery.WellKnownAddressesDiscoveryStrategy;
import com.exametrika.impl.groups.cluster.failuredetection.CoreGroupFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.flush.FlushParticipantProtocol;
import com.exametrika.impl.groups.cluster.membership.GroupMemberships;
import com.exametrika.tests.groups.channel.TestGroupChannel;
import com.exametrika.tests.groups.channel.TestGroupChannelFactory;
import com.exametrika.tests.groups.channel.TestGroupFactoryParameters;
import com.exametrika.tests.groups.channel.TestGroupParameters;

public class TestLoadGroupChannelFactory extends TestGroupChannelFactory
{
    private List<TestFailureSpec> failureSpecs;

    public TestLoadGroupChannelFactory(TestGroupFactoryParameters factoryParameters)
    {
        super(factoryParameters);
    }
    
    public IChannel create(int index, int count, TestLoadSpec loadSpec, List<TestFailureSpec> failureSpecs)
    {
        Assert.notNull(loadSpec);
        Assert.notNull(failureSpecs);
        
        this.failureSpecs = failureSpecs;
        
        TestLoadMessageSender sender = new TestLoadMessageSender(index, loadSpec, GroupMemberships.CORE_GROUP_ADDRESS);
        
        TestLoadStateStore stateStore = new TestLoadStateStore(TestLoadMessageSender.createBuffer(index, getStateLength(loadSpec)));
        TestLoadStateTransferFactory stateTransferFactory = new TestLoadStateTransferFactory(stateStore);
        sender.setStateTransferFactory(stateTransferFactory);
        
        int portRangeStart = 17000;
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
        
        TestGroupParameters parameters = new TestGroupParameters();
        parameters.channelName = "test" + index;
        parameters.clientPart = true;
        parameters.serverPart = true;
        parameters.portRangeStart = portRangeStart + index;
        parameters.portRangeStart = parameters.portRangeEnd;
        parameters.receiver = sender;
        parameters.discoveryStrategy = new WellKnownAddressesDiscoveryStrategy(wellKnownAddresses);
        parameters.stateTransferFactory = stateTransferFactory;
        parameters.deliveryHandler = sender;
        parameters.localFlowController = sender;
        parameters.serializationRegistrars.add(new TestLoadMessagePartSerializer());
        parameters.channelReconnector = new TestGroupChannelReconnector(((TestGroupFactoryParameters)factoryParameters).reconnectPeriod, this, parameters);
        
        TestGroupChannel channel = createChannel(parameters);
        sender.setChannel(channel);
        channel.start();
        sender.start();
        
        channel.getCompartment().addTimerProcessor(sender);
        return channel;
    }
    
    @Override
    protected void createProtocols(ChannelParameters parameters, String channelName, IMessageFactory messageFactory, 
        ISerializationRegistry serializationRegistry, ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers, 
        List<AbstractProtocol> protocols)
    {
        super.createProtocols(parameters, channelName, messageFactory, serializationRegistry, liveNodeProvider, 
            failureObservers, protocols);
        
        TestGroupFailureGenerationProtocol failureGenerationProtocol = new TestGroupFailureGenerationProtocol(channelName, messageFactory,
            failureSpecs, ((TestGroupFactoryParameters)factoryParameters).failureGenerationProcessPeriod, true);
        protocols.add(failureGenerationProtocol);
    }
    
    @Override
    protected void wireProtocols(IChannel channel, TcpTransport transport, ProtocolStack protocolStack)
    {
        super.wireProtocols(channel, transport, protocolStack);
        
        FlushParticipantProtocol flushParticipantProtocol = protocolStack.find(FlushParticipantProtocol.class);
        TestGroupFailureGenerationProtocol failureGenerationProtocol = protocolStack.find(TestGroupFailureGenerationProtocol.class);
        flushParticipantProtocol.getParticipants().add(failureGenerationProtocol);
        
        CoreGroupFailureDetectionProtocol failureDetectionProtocol = protocolStack.find(CoreGroupFailureDetectionProtocol.class);
        failureGenerationProtocol.setFailureDetector(failureDetectionProtocol);
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
