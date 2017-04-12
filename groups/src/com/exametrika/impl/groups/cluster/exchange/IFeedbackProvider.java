/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.exchange;

import java.util.UUID;

import com.exametrika.api.groups.cluster.IClusterMembership;
import com.exametrika.common.io.ISerializationRegistrar;



/**
 * The {@link IFeedbackProvider} is a provider of feedback data sent from worker nodes to cluster coordinator.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface IFeedbackProvider extends ISerializationRegistrar
{
    /**
     * Returns unique identifier of provider.
     *
     * @return unique identifier of provider
     */
    UUID getId();
    
    /**
     * Returns local data for exchange.
     *
     * @param force if true current data returned (if any) if false data returned only if changed since last call
     * @return local data for exchange or null if there is no data for exchange
     */
    IExchangeData getData(boolean force);
    
    /**
     * Sets remote data.
     *
     * @param data data for exchange
     */
    void setData(IExchangeData data);
    
    /**
     * Called when cluster membership has been changed. Can be used for cleanup.
     *
     * @param membership new cluster membership
     */
    void onClusterMembershipChanged(IClusterMembership membership);
}
