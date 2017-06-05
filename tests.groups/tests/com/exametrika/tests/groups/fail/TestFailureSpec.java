/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.groups.fail;

import com.exametrika.common.utils.Assert;

public class TestFailureSpec
{
    private final FailureTarget failureTarget;
    private final FailureQuantityType failureQuantityType;
    private final double failureQuantity;
    private final FailureEventType failureEventType;
    private final FailurePeriodType failurePeriodType;
    private final long failurePeriod;
    
    public enum FailureTarget
    {
        RANDOM_NODE,
        COORDINATOR
    }
    
    public enum FailureQuantityType
    {
        SINGLE,
        SET,
        SET_PERCENTAGE
    }
    
    public enum FailureEventType
    {
        START_FLUSH,
        PROCESSING_FLUSH,
        RANDOM
    }
    
    public enum FailurePeriodType
    {
        SET,
        RANDOM
    }

    public TestFailureSpec(FailureTarget failureTarget, FailureQuantityType failureQuantityType, double failureQuantity,
        FailureEventType failureEventType, FailurePeriodType failurePeriodType, long failurePeriod)
    {
        Assert.notNull(failureTarget);
        Assert.notNull(failureQuantityType);
        Assert.notNull(failureEventType);
        Assert.notNull(failurePeriodType);
        
        this.failureTarget = failureTarget;
        this.failureQuantityType = failureQuantityType;
        this.failureQuantity = failureQuantity;
        this.failureEventType = failureEventType;
        this.failurePeriodType = failurePeriodType;
        this.failurePeriod = failurePeriod;
    }

    public FailureTarget getFailureTarget()
    {
        return failureTarget;
    }

    public FailureQuantityType getFailureQuantityType()
    {
        return failureQuantityType;
    }

    public double getFailureQuantity()
    {
        return failureQuantity;
    }

    public FailureEventType getFailureEventType()
    {
        return failureEventType;
    }

    public FailurePeriodType getFailurePeriodType()
    {
        return failurePeriodType;
    }

    public long getFailurePeriod()
    {
        return failurePeriod;
    }
}
