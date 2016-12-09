/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;



/**
 * The {@link ISink} is used by {@link IFeed} to send messages and control readiness of data to send.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ISink
{
    /**
     * Returns sink destination.
     *
     * @return sink destination
     */
    IAddress getDestination();
    
    /**
     * Returns sink message factory.
     *
     * @return sink message factory
     */
    IMessageFactory getMessageFactory();
    
    /**
     * Sets readiness of data to send.
     *
     * @param ready if true data are ready to send, if false data are not ready to send
     */
    void setReady(boolean ready);
    
    /**
     * Sends message to message sink. Must be called from {@link IFeed#feed(ISink)} only.
     * Message must have destination equals to sink destination. Sender can use sink for sending more than one message,
     * first message is always sent, other messages can be sent only if result of previous sending is positive, i.e. sink allows sending
     * subsequent messages
     *
     * @param message message to send
     * @return true if sink can send subsequent messages, false if sink can not send subsequent messages
     */
    boolean send(IMessage message);
}
