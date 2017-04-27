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
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.IFailureObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.net.nio.TcpNioDispatcher;
import com.exametrika.common.tasks.impl.NoFlowController;
import com.exametrika.common.utils.Assert;

/**
 * The {@link CompositeChannelFactory} represents a composite factory of message-oriented channels.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class CompositeChannelFactory
{
    protected final ILogger logger = Loggers.get(getClass());
    protected final List<AbstractChannelFactory> subChannelFactories;
    protected final int mainSubChannelIndex;
    protected final ChannelFactoryParameters factoryParameters;
    
    public CompositeChannelFactory(List<AbstractChannelFactory> subChannelFactories, int mainSubChannelIndex)
    {
        this(subChannelFactories, mainSubChannelIndex, new ChannelFactoryParameters());
    }
    
    public CompositeChannelFactory(List<AbstractChannelFactory> subChannelFactories, int mainSubChannelIndex, 
        ChannelFactoryParameters factoryParameters)
    {
        Assert.notNull(subChannelFactories);
        Assert.notNull(factoryParameters);
        
        this.subChannelFactories = subChannelFactories;
        this.mainSubChannelIndex = mainSubChannelIndex;
        this.factoryParameters = factoryParameters;
    }
    
    protected ICompositeChannel createChannel(String channelName, List<? extends ChannelParameters> parameters)
    {
        Assert.notNull(parameters);
        Assert.isTrue(parameters.size() == subChannelFactories.size());
        
        if (channelName == null)
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
        
        List<IChannel> subChannels = createSubChannels(channelName, parameters, channelObserver, liveNodeManager,
            dispatcher, compartment);
        
        ICompositeChannel channel = createChannel(channelName, channelObserver, liveNodeManager, compartment, subChannels);
        wireSubChannels(channel, subChannels);
        return channel;
    }

    protected List<IChannel> createSubChannels(String channelName, List<? extends ChannelParameters> parameters,
        ChannelObserver channelObserver, LiveNodeManager liveNodeManager, TcpNioDispatcher dispatcher,
        ICompartment compartment)
    {
        List<IChannel> subChannels = new ArrayList<IChannel>();
        for (int i = 0; i < parameters.size(); i++)
        {
            subChannels.add(subChannelFactories.get(i).createChannel(channelName, channelObserver, liveNodeManager, dispatcher, 
                compartment, parameters.get(i)));
            wireSubChannel(i, subChannels);
        }
        return subChannels;
    }

    protected void wireSubChannel(int index, List<IChannel> subChannels)
    {
    }
    
    protected void wireSubChannels(ICompositeChannel channel, List<IChannel> subChannels)
    {
    }

    protected ICompositeChannel createChannel(String channelName, ChannelObserver channelObserver, LiveNodeManager liveNodeManager,
        ICompartment compartment, List<IChannel> subChannels)
    {
        return new CompositeChannel(channelName, liveNodeManager, channelObserver, subChannels, 
            subChannels.get(mainSubChannelIndex), compartment);
    }
}
