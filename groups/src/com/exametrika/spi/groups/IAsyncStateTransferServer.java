/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups;

import java.io.File;



/**
 * The {@link IAsyncStateTransferServer} is a server used in state transfer process.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IAsyncStateTransferServer extends IStateTransferServer
{
    /**
     * Saves snapshot of group state into the specified file.
     *
     * @param full if true full (persistent and transient) state is saved, else only persistent state is saved
     * @param file file for storing snapshot
     */
    void saveSnapshot(boolean full, File file);
}
