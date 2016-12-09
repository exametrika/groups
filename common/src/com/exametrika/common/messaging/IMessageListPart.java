/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;

import java.util.List;

/**
 * The {@link IMessageListPart} is a part of message containing list of messages.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IMessageListPart extends IMessagePart
{
    /**
     * Returns list of messages.
     *
     * @return list of messages
     */
    List<IMessage> getMessages();
}
