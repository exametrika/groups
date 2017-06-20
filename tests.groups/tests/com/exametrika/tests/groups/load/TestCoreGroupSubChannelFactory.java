/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.util.List;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.composite.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;
import com.exametrika.impl.groups.cluster.channel.CoreGroupSubChannelFactory;
import com.exametrika.impl.groups.cluster.failuredetection.CoreGroupFailureDetectionProtocol;
import com.exametrika.impl.groups.cluster.flush.FlushParticipantProtocol;
import com.exametrika.tests.groups.channel.TestGroupFactoryParameters;

public class TestCoreGroupSubChannelFactory extends CoreGroupSubChannelFactory
{
    public TestCoreGroupSubChannelFactory(TestCoreNodeFactoryParameters factoryParameters)
    {
        super(factoryParameters);
    }
    
    @Override
    protected void createProtocols(ChannelParameters parameters, String channelName, IMessageFactory messageFactory, 
        ISerializationRegistry serializationRegistry, ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers, 
        List<AbstractProtocol> protocols)
    {
        super.createProtocols(parameters, channelName, messageFactory, serializationRegistry, liveNodeProvider, 
            failureObservers, protocols);
        
        TestGroupFailureGenerationProtocol failureGenerationProtocol = new TestGroupFailureGenerationProtocol(channelName, messageFactory,
            ((TestCoreNodeParameters)parameters).failureSpecs, ((TestGroupFactoryParameters)factoryParameters).failureGenerationProcessPeriod, true);
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
}
