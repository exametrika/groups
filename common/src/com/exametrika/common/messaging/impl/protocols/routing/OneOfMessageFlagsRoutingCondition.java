/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.routing;

import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.ICondition;

/**
 * The {@link OneOfMessageFlagsRoutingCondition} represents a routing condition based on one of specified message flags.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class OneOfMessageFlagsRoutingCondition implements ICondition<IMessage>
{
    private final int messageFlags;
    
    public OneOfMessageFlagsRoutingCondition(int messageFlags)
    {
        this.messageFlags = messageFlags;
    }
    
    @Override
    public boolean evaluate(IMessage value)
    {
        if (value.hasOneOfFlags(messageFlags))
            return true;
        else
            return false;
    }
}
