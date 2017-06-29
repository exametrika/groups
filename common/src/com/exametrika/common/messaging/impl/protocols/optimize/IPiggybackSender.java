/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.optimize;

import com.exametrika.common.messaging.IMessagePart;

/**
 * The {@link IPiggybackSender} represents a piggyback sender.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IPiggybackSender
{
    /**
     * Returns piggyback message part for sending.
     *
     * @return piggyback message part or null if message for sending is not available
     */
   IMessagePart send();
}
