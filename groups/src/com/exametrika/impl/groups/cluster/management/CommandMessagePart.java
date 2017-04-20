/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.management;

import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;

/**
 * The {@link CommandMessagePart} is a command message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CommandMessagePart implements IMessagePart
{
    private final ICommand command;

    public CommandMessagePart(ICommand command)
    {
        Assert.notNull(command);
        
        this.command = command;
    }
    
    public ICommand getCommand()
    {
        return command;
    }
    
    @Override
    public int getSize()
    {
        return 1000;
    }
    
    @Override 
    public String toString()
    {
        return command.toString();
    }
}

