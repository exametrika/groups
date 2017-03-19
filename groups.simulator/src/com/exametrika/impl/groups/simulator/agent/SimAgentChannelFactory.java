/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.agent;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelFactory.Parameters;



/**
 * The {@link SimAgentChannelFactory} represents a simulator agent channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimAgentChannelFactory
{
    public SimAgentChannel createChannel(String channelName, String bindAddress, String host, int port)
    {
        ChannelFactory factory = new ChannelFactory();
        SimAgentChannel agentChannel = new SimAgentChannel(channelName, host, port);
        
        Parameters parameters = new Parameters();
        parameters.channelName = channelName;
        parameters.clientPart = true;
        parameters.receiver = agentChannel;
        
        if (bindAddress != null)
        {
            try
            {
                parameters.bindAddress = InetAddress.getByName(bindAddress);
            }
            catch (UnknownHostException e)
            {
                throw new ChannelException(e);
            }
        }
        
        parameters.secured = false;
        
        parameters.serializationRegistrars.add(agentChannel);
        
        IChannel channel = factory.createChannel(parameters);
        channel.getChannelObserver().addChannelListener(agentChannel);
        channel.getCompartment().addTimerProcessor(agentChannel);
        agentChannel.setChannel(channel);
        
        return agentChannel;
    }
}
