/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.routing;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;

/**
 * The {@link MessagePartTypeRoutingCondition} represents a routing condition based on type of message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MessagePartTypeRoutingCondition implements ICondition<IMessage>
{
    private final Class messagePartType;
    
    public MessagePartTypeRoutingCondition(Class messagePartType)
    {
        Assert.notNull(messagePartType);
        
        this.messagePartType = messagePartType;
    }
    
    @Override
    public boolean evaluate(IMessage value)
    {
        if (value.getPart().getClass() == messagePartType)
            return true;
        else
            return false;
    }
}
