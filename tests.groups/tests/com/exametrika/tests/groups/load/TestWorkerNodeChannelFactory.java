/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.util.Arrays;
import java.util.List;

import com.exametrika.api.groups.cluster.WorkerNodeFactoryParameters;
import com.exametrika.api.groups.cluster.WorkerNodeParameters;
import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.messaging.impl.AbstractChannelFactory;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.channel.WorkerNodeChannelFactory;
import com.exametrika.impl.groups.cluster.channel.WorkerToCoreSubChannelFactory;

public class TestWorkerNodeChannelFactory extends WorkerNodeChannelFactory
{
    public TestWorkerNodeChannelFactory()
    {
        this(new TestWorkerNodeFactoryParameters());
    }
    
    public TestWorkerNodeChannelFactory(TestWorkerNodeFactoryParameters factoryParameters)
    {
        super(createSubChannelFactories(factoryParameters), 0, factoryParameters);
    }
    
    @Override
    public ICompositeChannel createChannel(WorkerNodeParameters parameters)
    {
        Assert.isInstanceOf(TestWorkerNodeParameters.class, parameters);
        
        return super.createChannel(parameters);
    }
    
    private static List<AbstractChannelFactory> createSubChannelFactories(WorkerNodeFactoryParameters factoryParameters)
    {
        Assert.isInstanceOf(TestWorkerNodeFactoryParameters.class, factoryParameters);
        return Arrays.asList(new TestWorkerGroupSubChannelFactory((TestWorkerNodeFactoryParameters)factoryParameters),
            new WorkerToCoreSubChannelFactory(factoryParameters));
    }
}
