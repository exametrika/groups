/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups.cluster.state;

import com.exametrika.common.utils.ByteArray;



/**
 * The {@link ISimpleStateTransferServer} is a server used in simple state transfer process.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ISimpleStateTransferServer extends IStateTransferServer
{
    /**
     * Returns snapshot of group state.
     *
     * @param full if true full (persistent and transient) state is saved, else only persistent state is saved
     * @return snapshot of group state
     */
    ByteArray saveSnapshot(boolean full);
}
