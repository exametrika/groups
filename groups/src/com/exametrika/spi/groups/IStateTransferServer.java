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
     * Type of message received by server.
     */
    enum MessageType
    {
        /** Message which is not involved in replicated read/write operations.*/
        NON_STATE,
        /** Message which is used to read replicated state.*/
        STATE_READ,
        /** Message which is used to write replicated state.*/
        STATE_WRITE
    }
    /**
     * Classifies incoming message to segregate state read/write messages.
     *
     * @param part message part to classify
     * @return message type
     */
    MessageType classifyMessage(IMessagePart part);
    
    /**
     * Saves snapshot of group state into the specified file.
     *
     * @param file file for storing snapshot
     */
    void saveSnapshot(File file);
}
