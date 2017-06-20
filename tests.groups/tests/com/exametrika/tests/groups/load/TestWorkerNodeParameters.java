/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.util.List;

import com.exametrika.api.groups.cluster.WorkerNodeParameters;

public class TestWorkerNodeParameters extends WorkerNodeParameters
{
    public List<TestFailureSpec> failureSpecs;
}
