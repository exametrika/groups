/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;

import java.util.List;


/**
 * The {@link IBulkSender} represents a bulk sender part of communication protocol. Implements "push" model of
 * sender part of protocol stack.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IBulkSender
{
    /**
     * Sends the messages to underlying protocol stack.
     *
     * @param messages messages to send
     */
    void send(List<IMessage> messages);
}
