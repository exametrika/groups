/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import com.exametrika.common.messaging.IAddress;



/**
 * The {@link ILocalNodeAware} is a local node initializing interface.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ILocalNodeAware
{
    /**
     * Sets address of local node.
     *
     * @param node address of local node
     */
    void setLocalNode(IAddress node);
}
