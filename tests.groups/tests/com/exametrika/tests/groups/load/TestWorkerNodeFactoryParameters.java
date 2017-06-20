/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import com.exametrika.api.groups.cluster.WorkerNodeFactoryParameters;

public class TestWorkerNodeFactoryParameters extends WorkerNodeFactoryParameters
{
    public long failureGenerationProcessPeriod = 1000;
    public long reconnectPeriod = 60000;
    
    public TestWorkerNodeFactoryParameters()
    {
        super(false);
    }
    
    public TestWorkerNodeFactoryParameters(boolean debug)
    {
        super(debug);
    }
}
