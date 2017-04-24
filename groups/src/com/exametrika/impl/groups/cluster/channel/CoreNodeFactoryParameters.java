/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.channel;

/**
 * The {@link CoreNodeFactoryParameters} is a core node factory parameters.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author medvedev
 */
public class CoreNodeFactoryParameters extends NodeFactoryParameters
{
    public CoreNodeFactoryParameters()
    {
        super(false);
    }
    
    public CoreNodeFactoryParameters(boolean debug)
    {
        super(debug);
    }
}