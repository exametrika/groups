/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import com.exametrika.common.messaging.impl.transports.UnicastAddress;

/**
 * The {@link ILocalNodeAware} is a local node initializing interface.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ILocalNodeAware
{
    /**
     * Sets address of local node specific to given transport.
     *
     * @param transportId identifier of transport
     * @param address address of local node
     * @param connection connection string
     * @return local address
     */
    UnicastAddress setLocalNode(int transportId, Object address, String connection);
}
