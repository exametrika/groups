/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.channel;

import java.util.List;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.impl.Channel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.message.MessageFactory;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ConnectionManager;
import com.exametrika.common.messaging.impl.transports.tcp.TcpTransport;

/**
 * The {@link GroupChannelFactory} is a group channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public class GroupChannelFactory extends ChannelFactory
{
    // TODO: реализовать фабрику по аналогии с тестовой в FlushProtocolTests
    public static class GroupFactoryParameters extends FactoryParameters
    {
        public GroupFactoryParameters()
        {
            super(false);
        }
        
        public GroupFactoryParameters(boolean debug)
        {
            super(debug);
        }
    }
    
    public static class GroupParameters extends Parameters
    {
    }

    public GroupChannelFactory()
    {
        this(new GroupFactoryParameters());
    }
    
    public GroupChannelFactory(GroupFactoryParameters factoryParameters)
    {
        super(factoryParameters);
    }
    
    public IChannel createChannel(GroupParameters parameters)
    {
        return super.createChannel(parameters);
    }
    
    @Override
    protected void createProtocols(Parameters parameters, String channelName, IMessageFactory messageFactory, 
        ISerializationRegistry serializationRegistry, ILiveNodeProvider liveNodeProvider, List<IFailureObserver> failureObservers,
        List<AbstractProtocol> protocols)
    {
    }
    
    @Override
    protected Channel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
        MessageFactory messageFactory, ProtocolStack protocolStack, TcpTransport transport,
        ConnectionManager connectionManager, ICompartment compartment)
    {
        return null;
    }
}
