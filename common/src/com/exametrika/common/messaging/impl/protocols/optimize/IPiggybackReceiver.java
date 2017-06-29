/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.optimize;

import com.exametrika.common.messaging.IMessagePart;

/**
 * The {@link IPiggybackReceiver} represents a piggyback receiver.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IPiggybackReceiver
{
   /**
    * Receives piggyback message part.
    *
    * @param part message part
    * @return true if given message part is handled by receiver
    */
   boolean receive(IMessagePart part);
}
