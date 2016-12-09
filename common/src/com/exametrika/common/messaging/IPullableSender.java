/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;



/**
 * The {@link IPullableSender} represents a pull-oriented message sender where sending part can register its intent to send a message. 
 * Sender notifies sending part using {@link IFeed} when it is actually ready to send a message.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IPullableSender
{
    /**
     * Registers intent to send messages.
     *
     * @param destination message destination
     * @param feed feed to notify when transport is actually ready to send messages to specified destination
     * @return message sink used to send messages and set readiness of data by sender part or null if sink could not be established
     */
    ISink register(IAddress destination, IFeed feed);
    
    /**
     * Unregisters intent to send messages.
     *
     * @param sink message sink registered by {@link IPullableSender#register}
     */
    void unregister(ISink sink);
}
