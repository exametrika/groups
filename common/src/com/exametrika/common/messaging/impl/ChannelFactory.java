/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentFactory;
import com.exametrika.common.compartment.impl.CompartmentFactory;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.net.nio.TcpNioDispatcher;
import com.exametrika.common.tasks.impl.NoFlowController;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ChannelFactory} represents a factory of message-oriented channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class ChannelFactory extends AbstractChannelFactory
{
    public ChannelFactory()
    {
        this(new ChannelFactoryParameters());
    }
    
    public ChannelFactory(ChannelFactoryParameters factoryParameters)
    {
        super(factoryParameters);
    }
    
    public IChannel createChannel(ChannelParameters parameters)
    {
        Assert.notNull(parameters);
        
        String channelName;
        if (parameters.channelName != null)
            channelName = parameters.channelName;
        else
            channelName = ManagementFactory.getRuntimeMXBean().getName();
        
        ChannelObserver channelObserver = new ChannelObserver(channelName);
        List<IFailureObserver> failureObservers = new ArrayList<IFailureObserver>();
        failureObservers.add(channelObserver);
        LiveNodeManager liveNodeManager = new LiveNodeManager(channelName, failureObservers, channelObserver);
       
        TcpNioDispatcher dispatcher = new TcpNioDispatcher(factoryParameters.transportChannelTimeout, 
            factoryParameters.transportMaxChannelIdlePeriod, channelName);
        
        ICompartmentFactory.Parameters compartmentParameters = new ICompartmentFactory.Parameters();
        compartmentParameters.name = channelName;
        compartmentParameters.dispatchPeriod = factoryParameters.selectionPeriod;
        compartmentParameters.dispatcher = dispatcher;
        compartmentParameters.flowController = new NoFlowController();
        compartmentParameters.minLockQueueCapacity = factoryParameters.compartmentMinLockQueueCapacity;
        compartmentParameters.minLockQueueCapacity = factoryParameters.compartmentMinLockQueueCapacity;
        
        ICompartment compartment = new CompartmentFactory().createCompartment(compartmentParameters);
        
        return createChannel(channelName, channelObserver, liveNodeManager, dispatcher, compartment, parameters);
    }
}
