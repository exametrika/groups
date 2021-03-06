/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.exchange;

import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;



/**
 * The {@link IDataExchangeProvider} is a provider of data for exchanging in cluster.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface IDataExchangeProvider
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
     * @return local data for exchange or null if there is no data for exchange
     */
    IExchangeData getData();
    
    /**
     * Sets remote data received from specified node.
     *
     * @param source source of data
     * @param data data for exchange
     */
    void setData(INode source, IExchangeData data);
}
