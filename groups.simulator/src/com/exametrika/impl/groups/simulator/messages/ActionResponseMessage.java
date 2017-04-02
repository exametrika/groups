/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.messages;

import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ActionResponseMessage} is an action response message.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ActionResponseMessage implements IMessagePart
{
    private final String actionName;
    private final Object result;

    public ActionResponseMessage(String actionName, Object result)
    {
        Assert.notNull(actionName);
        
        this.actionName = actionName;
        this.result = result;
    }
    
    public String getActionName()
    {
        return actionName;
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
        return actionName + "=" + result != null ? result.toString() : "";
    }
}

