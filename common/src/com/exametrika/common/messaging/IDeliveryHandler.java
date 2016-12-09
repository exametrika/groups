/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;


/**
 * The {@link IDeliveryHandler} is used for confirmation of actual message delivery by application.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IDeliveryHandler
{
    /**
     * Called by application when specified message has been delivered.
     * 
     * @param message delivered message
     */
    void onDelivered(IMessage message);
}