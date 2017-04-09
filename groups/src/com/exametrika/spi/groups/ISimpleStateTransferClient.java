/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups;

import com.exametrika.common.utils.ByteArray;



/**
 * The {@link ISimpleStateTransferClient} is a client used in simple state transfer process.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ISimpleStateTransferClient
{
    /**
     * Loads snapshot of state from specified data.
     *
     * @param data data containing state snapshot
     */
    void loadSnapshot(ByteArray data);
}
