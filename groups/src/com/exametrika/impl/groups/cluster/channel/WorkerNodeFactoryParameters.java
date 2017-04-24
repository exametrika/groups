/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

/**
 * The {@link WorkerNodeFactoryParameters} is a worker node factory parameters.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author medvedev
 */
public class WorkerNodeFactoryParameters extends NodeFactoryParameters
{
    public WorkerNodeFactoryParameters()
    {
        super(false);
    }
    
    public WorkerNodeFactoryParameters(boolean debug)
    {
        super(debug);
    }
}