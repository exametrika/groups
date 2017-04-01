/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.shell.IShellCommand;



/**
 * The {@link ShellCommandsBuilder} defines a shell commands builder.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ShellCommandsBuilder
{
    private List<IShellCommand> commands = new ArrayList<IShellCommand>();

    public ShellCommandBuilder command()
    {
        return new ShellCommandBuilder(this);
    }
    
    public ShellNamespaceBuilder namespace()
    {
        return new ShellNamespaceBuilder(this);
    }
    
    public List<IShellCommand> build()
    {
        return commands;
    }
    
    void addCommand(IShellCommand command)
    {
        commands.add(command);
    }
}
