/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;


/**
 * The {@link IFeed} represents a message feed that initiates on-demand message sending 
 * when {@link IPullableSender} is ready to send a message.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IFeed
{
    /**
     * Called by sender when sender is ready to send a message.
     *
     * @param sink message sink used to send a message
     */
    void feed(ISink sink);
}
