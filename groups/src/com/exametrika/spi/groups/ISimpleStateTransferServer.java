/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.ByteArray;



/**
 * The {@link ISimpleStateTransferServer} is a server used in simple state transfer process.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ISimpleStateTransferServer
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
     * @param message message to classify
     * @return message type
     */
    MessageType classifyMessage(IMessage message);
    
    /**
     * Returns snapshot of group state.
     *
     * @return snapshot of group state
     */
    ByteArray saveSnapshot();
}
