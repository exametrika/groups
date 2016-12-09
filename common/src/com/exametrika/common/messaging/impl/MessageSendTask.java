/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl;

import java.util.List;

import com.exametrika.common.compartment.ICompartmentTaskSize;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;

/**
 * The {@link MessageSendTask} represents a message send task.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MessageSendTask implements Runnable, ICompartmentTaskSize
{
    private final AbstractProtocol entry;
    private final List<IMessage> messages;
    private final int size;
    
    public MessageSendTask(AbstractProtocol entry, List<IMessage> messages)
    {
        Assert.notNull(messages);

        this.entry = entry;
        this.messages = messages;
        
        int size = 0;
        for (IMessage message : messages)
            size += message.getSize();
        
        this.size = size;
    }
    
    @Override
    public int getSize()
    {
        return size;
    }

    @Override
    public void run()
    {
        for (IMessage message : messages)
            entry.send(message);
    }
    
    @Override
    public String toString()
    {
        return messages.toString();
    }
}
