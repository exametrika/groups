/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import com.exametrika.api.groups.cluster.CoreNodeFactoryParameters;

public class TestCoreNodeFactoryParameters extends CoreNodeFactoryParameters
{
    public long failureGenerationProcessPeriod = 1000;
    public long reconnectPeriod = 60000;
    
    public TestCoreNodeFactoryParameters()
    {
        super(false);
    }
    
    public TestCoreNodeFactoryParameters(boolean debug)
    {
        super(debug);
    }
}
