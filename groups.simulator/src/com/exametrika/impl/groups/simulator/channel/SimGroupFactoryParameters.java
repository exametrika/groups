/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.channel;

import com.exametrika.api.groups.cluster.NodeFactoryParameters;

/**
 * The {@link SimGroupFactoryParameters} is a test group factory parameters.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author medvedev
 */
public class SimGroupFactoryParameters extends NodeFactoryParameters
{
    public SimGroupFactoryParameters()
    {
        super(false);
    }
    
    public SimGroupFactoryParameters(boolean debug)
    {
        super(debug);
    }
}