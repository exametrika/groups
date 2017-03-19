/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.simulator.messages;

import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link ActionMessage} is an action message.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ActionMessage implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String actionName;
    private final Map<String, Object> parameters;

    public ActionMessage(String actionName, Map<String, Object> parameters)
    {
        Assert.notNull(actionName);
        Assert.notNull(parameters);
        
        this.actionName = actionName;
        this.parameters = Immutables.wrap(parameters);
    }
    
    public String getActionName()
    {
        return actionName;
    }
    
    public Map<String, Object> getParameters()
    {
        return parameters;
    }
    
    @Override
    public int getSize()
    {
        return 0;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(actionName, parameters).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("action: {0}, parameters: {1}")
        ILocalizedMessage toString(String actionName, Map<String, Object> parameters);
    }
}

