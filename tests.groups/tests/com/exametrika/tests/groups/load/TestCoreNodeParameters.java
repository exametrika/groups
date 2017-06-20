/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.load;

import java.util.List;

import com.exametrika.api.groups.cluster.CoreNodeParameters;

public class TestCoreNodeParameters extends CoreNodeParameters
{
    public List<TestFailureSpec> failureSpecs;
}
