/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;


/**
 * The {@link ISender} represents a sender part of communication protocol. Implements "push" model of
 * sender part of protocol stack.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ISender
{
    /**
     * Sends the message to underlying protocol stack.
     *
     * @param message message to send
     */
    void send(IMessage message);
}
