/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.util.Arrays;
import java.util.List;

import com.exametrika.api.groups.cluster.CoreNodeFactoryParameters;
import com.exametrika.api.groups.cluster.CoreNodeParameters;
import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.messaging.impl.AbstractChannelFactory;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannelFactory;
import com.exametrika.impl.groups.cluster.channel.CoreToWorkerSubChannelFactory;

public class TestCoreNodeChannelFactory extends CoreNodeChannelFactory
{
    public TestCoreNodeChannelFactory()
    {
        this(new TestCoreNodeFactoryParameters());
    }
    
    public TestCoreNodeChannelFactory(TestCoreNodeFactoryParameters factoryParameters)
    {
        super(createSubChannelFactories(factoryParameters), 0, factoryParameters);
    }
    
    @Override
    public ICompositeChannel createChannel(CoreNodeParameters parameters)
    {
        Assert.isInstanceOf(TestCoreNodeParameters.class, parameters);
        
        return super.createChannel(parameters);
    }
    
    private static List<AbstractChannelFactory> createSubChannelFactories(CoreNodeFactoryParameters factoryParameters)
    {
        Assert.isInstanceOf(TestCoreNodeFactoryParameters.class, factoryParameters);
        return Arrays.asList(new TestCoreGroupSubChannelFactory((TestCoreNodeFactoryParameters)factoryParameters),
            new CoreToWorkerSubChannelFactory(factoryParameters));
    }
}
