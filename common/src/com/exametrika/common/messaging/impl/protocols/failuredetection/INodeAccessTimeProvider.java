/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import com.exametrika.common.messaging.IAddress;



/**
 * The {@link INodeAccessTimeProvider} is a provider of last access time for specified channel.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface INodeAccessTimeProvider
{
    /**
     * Returns last read time for specified node.
     *
     * @param node node
     * @return last read time
     */
    long getLastReadTime(IAddress node);
    
    /**
     * Returns last write time for specified node.
     *
     * @param node node
     * @return last write time
     */
    long getLastWriteTime(IAddress node);
}
