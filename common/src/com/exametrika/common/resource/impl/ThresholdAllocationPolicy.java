/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.util.List;
import java.util.Map;

import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;

/**
 * The {@link ThresholdAllocationPolicy} is an allocation policy which dynamically switches to some specified allocation policy
 * if given threshold of available resource has been reached.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ThresholdAllocationPolicy implements IAllocationPolicy
{
    private final List<Pair<Long, IAllocationPolicy>> thresholds;

    /**
     * Creates an object.
     *
     * @param thresholds list of pairs threshold:allocation policy defining thresholds of available resource and allocation policy
     * applied when amount of available resource exceeded threshold (i.e. allocation policy with greatest threshold less then
     * specified amount of available resource). One of thresholds must be 0, i.e. defines allocation policy for start of sclale.
     */
    public ThresholdAllocationPolicy(List<Pair<Long, IAllocationPolicy>> thresholds)
    {
        Assert.notNull(thresholds);
        Assert.isTrue(!thresholds.isEmpty());
        
        boolean startFound = false;
        for (Pair<Long, IAllocationPolicy> threshold : thresholds)
        {
            if (threshold.getKey() == 0)
            {
                startFound = true;
                break;
            }
        }
        Assert.isTrue(startFound);
        
        this.thresholds = thresholds;
    }

    @Override
    public void allocate(long amount, Map<String, IResourceConsumer> consumers)
    {
        long maxThreshold = Long.MIN_VALUE;
        IAllocationPolicy policy = null;
        for (Pair<Long, IAllocationPolicy> pair : thresholds)
        {
            long threshold = pair.getKey();
            if (threshold > amount)
                continue;
            
            if (threshold > maxThreshold)
            {
                maxThreshold = threshold;
                policy = pair.getValue();
            }
        }
        
        Assert.notNull(policy);
        policy.allocate(amount, consumers);
    }
}
