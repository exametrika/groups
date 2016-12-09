/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;


/**
 * The {@link IReceiver} receives messages from communication channel.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IReceiver
{
    /**
     * Called when message delivered from underlying protocol stack.
     * 
     * @param message received message
     */
    void receive(IMessage message);
}