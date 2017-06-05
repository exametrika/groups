/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.channel;

import com.exametrika.api.groups.cluster.NodeFactoryParameters;

/**
 * The {@link TestGroupFactoryParameters} is a test group factory parameters.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author medvedev
 */
public class TestGroupFactoryParameters extends NodeFactoryParameters
{
    public long failureGenerationProcessPeriod = 1000;
    
    public TestGroupFactoryParameters()
    {
        super(false);
    }
    
    public TestGroupFactoryParameters(boolean debug)
    {
        super(debug);
    }
}