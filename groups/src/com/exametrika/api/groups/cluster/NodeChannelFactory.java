/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.cluster.channel.CoreNodeChannelFactory;
import com.exametrika.impl.groups.cluster.channel.WorkerNodeChannelFactory;

/**
 * The {@link NodeChannelFactory} is a node channel factory.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class NodeChannelFactory
{
    private final CoreNodeFactoryParameters coreNodeFactoryParameters;
    private final WorkerNodeFactoryParameters workerNodeFactoryParameters;

    public NodeChannelFactory()
    {
        this(new CoreNodeFactoryParameters(), new WorkerNodeFactoryParameters());
    }
    
    public NodeChannelFactory(CoreNodeFactoryParameters coreNodeFactoryParameters, WorkerNodeFactoryParameters workerNodeFactoryParameters)
    {
        Assert.notNull(coreNodeFactoryParameters);
        Assert.notNull(workerNodeFactoryParameters);
        
        this.coreNodeFactoryParameters = coreNodeFactoryParameters;
        this.workerNodeFactoryParameters = workerNodeFactoryParameters;
    }
    
    public ICoreNodeChannel createCoreNodeChannel(CoreNodeParameters parameters)
    {
        CoreNodeChannelFactory factory = new CoreNodeChannelFactory(coreNodeFactoryParameters);
        return (ICoreNodeChannel)factory.createChannel(parameters);
    }
    
    public IWorkerNodeChannel createWorkerNodeChannel(WorkerNodeParameters parameters)
    {
        WorkerNodeChannelFactory factory = new WorkerNodeChannelFactory(workerNodeFactoryParameters);
        return (IWorkerNodeChannel)factory.createChannel(parameters);
    }
}
