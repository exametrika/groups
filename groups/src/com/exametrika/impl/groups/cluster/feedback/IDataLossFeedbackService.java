/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

/**
 * The {@link IDataLossFeedbackService} represents a data loss feedback service.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IDataLossFeedbackService
{
    /**
     * Updates data loss state.
     *
     * @param state node state
     */
    void updateDataLossState(IDataLossState state);
}