/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import com.exametrika.common.messaging.IAddress;



/**
 * The {@link ICleanupManager} is a cleanup manager.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author Medvedev-A
 */
public interface ICleanupManager
{
    /**
     * Can cleanup be performed for specified node?
     *
     * @param node node
     * @return true if cleanup can be performed for specified node
     */
    boolean canCleanup(IAddress node);
}
