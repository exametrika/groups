/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.optimize;

import java.util.List;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageListPart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;

/**
 * The {@link MessageListPart} is a message list part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MessageListPart implements IMessageListPart
{
    private final List<IMessage> messages;
    private final int size;

    public MessageListPart(List<IMessage> messages)
    {
        Assert.notNull(messages);
        
        this.messages = Immutables.wrap(messages);
        int size = 0;
        for (IMessage message : messages)
            size += message.getSize();
        
        this.size = size;
    }
    
    @Override
    public List<IMessage> getMessages()
    {
        return messages;
    }
    
    @Override
    public int getSize()
    {
        return size;
    }
    
    @Override 
    public String toString()
    {
       return Strings.toString(messages,false);
    }
}