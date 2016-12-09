/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups;

import java.io.File;

import com.exametrika.common.messaging.IMessagePart;



/**
 * The {@link IStateTransferServer} is a server used in state transfer process.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IStateTransferServer
{
    /**
     * Does specified message part modify replicated group state.
     *
     * @param part message part
     * @return true if specified message part modifies replicated group state
     */
    boolean isModifyingMessage(IMessagePart part);
    
    /**
     * Saves snapshot of group state into the specified file.
     *
     * @param file file for storing snapshot
     */
    void saveSnapshot(File file);
}
