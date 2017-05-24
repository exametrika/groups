/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.message;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;

/**
 * The {@link IWrapperMessagePart} is a wrapper message part.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IWrapperMessagePart extends IMessagePart
{
    /**
     * Returns original message.
     *
     * @return original message
     */
    IMessage getMessage();
}
