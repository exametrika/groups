/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.List;

import com.exametrika.common.shell.IShell;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellPromptProvider;



/**
 * The {@link ShellBuilder} defines a shell builder.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ShellBuilder
{
    private String title;
    private List<IShellCommand> commands;
    private IShellCommand defaultCommand;
    private boolean loadFromServices = true;
    private String historyFilePath;
    private IShellPromptProvider promptProvider = new DefaultShellPromptProvider();
    private char nameSeparator = ':';
    
    public ShellBuilder setTitle(String title)
    {
        this.title = title;
        return this;
    }

    public ShellBuilder setCommands(List<IShellCommand> commands)
    {
        this.commands = commands;
        return this;
    }

    public ShellBuilder setDefaultCommand(IShellCommand defaultCommand)
    {
        this.defaultCommand = defaultCommand;
        return this;
    }

    public ShellBuilder setLoadFromServices(boolean loadFromServices)
    {
        this.loadFromServices = loadFromServices;
        return this;
    }

    public ShellBuilder setHistoryFilePath(String historyFilePath)
    {
        this.historyFilePath = historyFilePath;
        return this;
    }

    public ShellBuilder setPromptProvider(IShellPromptProvider promptProvider)
    {
        this.promptProvider = promptProvider;
        return this;
    }

    public ShellBuilder setNameSeparator(char nameSeparator)
    {
        this.nameSeparator = nameSeparator;
        return this;
    }
    
    public IShell build()
    {
        return new Shell(title, commands, defaultCommand, loadFromServices, historyFilePath, promptProvider, nameSeparator);
    }
}
