/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;

/**
 * The {@link IMessagePart} is a part of message.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IMessagePart
{
    /**
     * Returns estimated size of message part.
     *
     * @return estimated size of message part
     */
    int getSize();
}
