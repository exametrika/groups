/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.routing;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.ICondition;

/**
 * The {@link AllMessageFlagsRoutingCondition} represents a routing condition based on all specified message flags.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class AllMessageFlagsRoutingCondition implements ICondition<IMessage>
{
    private final int messageFlags;
    
    public AllMessageFlagsRoutingCondition(int messageFlags)
    {
        this.messageFlags = messageFlags;
    }
    
    @Override
    public boolean evaluate(IMessage value)
    {
        if (value.hasFlags(messageFlags))
            return true;
        else
            return false;
    }
}
