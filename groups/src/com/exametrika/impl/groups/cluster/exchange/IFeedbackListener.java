/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.exchange;

/**
 * The {@link IFeedbackListener} represents a group feedback listener.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */

public interface IFeedbackListener
{
    /**
     * Called when provider data have been changed.
     *
     * @param provider provider
     * @param data data
     */
    void onDataChanged(IFeedbackProvider provider, IExchangeData data);
}