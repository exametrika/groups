/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.ArrayList;
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
    private List<IShellCommand> commands = new ArrayList<IShellCommand>();
    private IShellCommand defaultCommand;
    private boolean loadFromServices = true;
    private String historyFilePath;
    private IShellPromptProvider promptProvider = new DefaultShellPromptProvider();
    private char nameSeparator = ':';
    private char namedParameterPrefix = '-';
    private boolean noColors;
    
    public ShellBuilder title(String title)
    {
        this.title = title;
        return this;
    }

    public ShellBuilder commands(List<IShellCommand> commands)
    {
        this.commands = commands;
        return this;
    }

    public ShellBuilder defaultCommand(IShellCommand defaultCommand)
    {
        this.defaultCommand = defaultCommand;
        return this;
    }

    public ShellBuilder loadFromServices(boolean loadFromServices)
    {
        this.loadFromServices = loadFromServices;
        return this;
    }

    public ShellBuilder historyFilePath(String historyFilePath)
    {
        this.historyFilePath = historyFilePath;
        return this;
    }

    public ShellBuilder promptProvider(IShellPromptProvider promptProvider)
    {
        this.promptProvider = promptProvider;
        return this;
    }

    public ShellBuilder nameSeparator(char nameSeparator)
    {
        this.nameSeparator = nameSeparator;
        return this;
    }
    
    public ShellBuilder namedParameterPrefix(char namedParameterPrefix)
    {
        this.namedParameterPrefix = namedParameterPrefix;
        return this;
    }
    
    public ShellBuilder noColors(boolean noColors)
    {
        this.noColors = noColors;
        return this;
    }
    
    public IShell build()
    {
        return new Shell(title, commands, defaultCommand, loadFromServices, historyFilePath, promptProvider, nameSeparator,
            namedParameterPrefix, noColors);
    }
}
