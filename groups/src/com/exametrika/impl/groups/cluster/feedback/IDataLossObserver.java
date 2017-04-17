/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import com.exametrika.api.groups.cluster.IGroup;


/**
 * The {@link IDataLossObserver} represents a data loss observer.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IDataLossObserver
{
    /**
     * Called when data loss for given group has been detected.
     *
     * @param group group with data loss
     */
    void onDataLoss(IGroup group);
}