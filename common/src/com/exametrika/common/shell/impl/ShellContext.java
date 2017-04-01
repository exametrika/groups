/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.io.PrintWriter;
import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.shell.IShell;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellContext;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;

/**
 * The {@link ShellContext} is a shell context.
 * 
 * @threadsafety This class and its methods are not thread safe.
 */
public class ShellContext implements IShellContext
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Shell shell;

    public ShellContext(Shell shell)
    {
        Assert.notNull(shell);
        
        this.shell = shell;
    }
    
    @Override
    public IShell getShell()
    {
        return shell;
    }
    
    @Override
    public PrintWriter getWriter()
    {
        return shell.getLineReader().getTerminal().writer();
    }
    
    @Override
    public void flush()
    {
        shell.getLineReader().getTerminal().flush();
    }

    @Override
    public String getPath()
    {
        ShellNode node = shell.getContextNode();
        char nameSeparator = shell.getNameSeparator();
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        while (node.getParent() != null)
        {
            if (first)
                first = false;
            else
                builder.insert(0, nameSeparator);
            
            builder.insert(0, node.getName());
            
            node = node.getParent();
        }
        
        return builder.toString();
    }
    
    @Override
    public Object execute(String commandName, Map<String, Object> parameters)
    {
        IShellCommand command = shell.findCommand(commandName);
        if (command != null)
            return command.getExecutor().execute(command, this, parameters);
        else
            throw new InvalidArgumentException(messages.commandNotFound(commandName));
    }
    
    interface IMessages
    {
        @DefaultMessage("Command ''{0}'' is not found.")
        ILocalizedMessage commandNotFound(String command);
    }
}