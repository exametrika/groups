/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.modelling;

import com.exametrika.common.messaging.IMessage;


/**
 * The {@link MessageSizeLossModel} is a message loss model that drops large messages.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MessageSizeLossModel implements ILossModel
{
    private final int maxMessageSize;
    
    public MessageSizeLossModel(int maxMessageSize)
    {
        this.maxMessageSize = maxMessageSize;
    }
    
    @Override
    public boolean canDropMessage(IMessage message)
    {
        return message.getSize() > maxMessageSize;
    }
}
