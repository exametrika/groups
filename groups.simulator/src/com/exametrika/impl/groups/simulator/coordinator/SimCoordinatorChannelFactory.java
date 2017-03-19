/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.coordinator;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelFactory.Parameters;



/**
 * The {@link SimCoordinatorChannelFactory} represents a factory of simulator agent channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SimCoordinatorChannelFactory
{
    public SimCoordinatorChannel createChannel(String channelName, int port, String bindAddress)
    {
        ChannelFactory factory = new ChannelFactory();
        SimCoordinatorChannel coordinatorChannel = new SimCoordinatorChannel(channelName);
        
        Parameters parameters = new Parameters();
        parameters.channelName = channelName;
        parameters.serverPart = true;
        parameters.portRangeStart = port;
        parameters.portRangeEnd = port;
        parameters.receiver = coordinatorChannel;
        
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
        
        parameters.serializationRegistrars.add(coordinatorChannel);
        
        IChannel channel = factory.createChannel(parameters);
        coordinatorChannel.setChannel(channel);
        channel.getChannelObserver().addChannelListener(coordinatorChannel);
        channel.getCompartment().addTimerProcessor(coordinatorChannel);
        
        return coordinatorChannel;
    }
}
