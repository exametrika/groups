/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.messages;

import com.exametrika.common.messaging.IMessagePart;

/**
 * The {@link ActionResponseMessage} is an action response message.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ActionResponseMessage implements IMessagePart
{
    private final Object result;

    public ActionResponseMessage(Object result)
    {
        this.result = result;
    }
    
    public Object getResult()
    {
        return result;
    }
    
    @Override
    public int getSize()
    {
        return 0;
    }
    
    @Override 
    public String toString()
    {
        return result != null ? result.toString() : "";
    }
}

