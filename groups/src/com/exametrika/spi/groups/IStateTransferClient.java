/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups;

import java.io.File;



/**
 * The {@link IStateTransferClient} is a client used in state transfer process.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IStateTransferClient
{
    /**
     * Loads snapshot of state from specified file.
     * 
     * @param full if true full (persistent and transient) state is loaded, else only persistent state is loaded
     * @param file file containing state snapshot
     */
    void loadSnapshot(boolean full, File file);
}
