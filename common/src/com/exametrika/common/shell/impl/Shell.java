/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.ArrayList;
import java.util.List;

import org.jline.reader.Completer;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.exametrika.common.l10n.SystemException;
import com.exametrika.common.services.Services;
import com.exametrika.common.shell.IShell;
import com.exametrika.common.shell.IShellCommandProvider;
import com.exametrika.common.utils.Assert;



/**
 * The {@link Shell} defines an interactive shell.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class Shell implements IShell, Runnable
{
    private final String title;
    private final List<ShellCommand> commands;
    private final ShellCommand defaultCommand;
    private final ShellCommandParser parser;
    private final String historyFilePath;
    
    public Shell(String title, List<ShellCommand> commands, ShellCommand defaultCommand, boolean loadFromServices,
        String historyFilePath)
    {
        Assert.notNull(title);
        Assert.notNull(commands);
        
        commands = new ArrayList<ShellCommand>(commands);
        if (loadFromServices)
        {
            List<IShellCommandProvider> providers = Services.loadProviders(IShellCommandProvider.class);
            for (IShellCommandProvider provider : providers)
                commands.addAll(provider.getCommands());
        }
        
        this.title = title;
        this.commands = commands;
        this.defaultCommand = defaultCommand;
        this.parser = new ShellCommandParser(commands, defaultCommand);
        this.historyFilePath = historyFilePath;
    }
    
    public ShellCommandParser getParser()
    {
        return parser;
    }

    @Override
    public void run()
    {
        try
        {
            Terminal terminal = TerminalBuilder.builder().system(true).jna(true).build();
            terminal.writer().print(title + "\n\n");
            
            LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder().terminal(terminal).appName(title)
                .completer(createCompleter()).highlighter(createHighlighter());
            if (historyFilePath != null)
                lineReaderBuilder.variable(LineReader.HISTORY_FILE, historyFilePath);
            
            LineReader lineReader = lineReaderBuilder.build();
        }
        catch (Exception e)
        {
            throw new SystemException(e);
        }
    }

    private Highlighter createHighlighter()
    {
        // TODO Auto-generated method stub
        return null;
    }

    private Completer createCompleter()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
