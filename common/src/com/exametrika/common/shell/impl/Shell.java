/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.l10n.SystemException;
import com.exametrika.common.services.Services;
import com.exametrika.common.shell.IShell;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellCommandParser;
import com.exametrika.common.shell.IShellCommandProvider;
import com.exametrika.common.shell.IShellPromptProvider;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Pair;



/**
 * The {@link Shell} defines an interactive shell.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class Shell implements IShell, Runnable
{
    private static final IMessages messages = Messages.get(IMessages.class);
    public static final int INDENT = 4;
    public static final String PREVIOUS_LEVEL_COMMAND = "..";
    public static final String PREVIOUS_LEVEL_COMMAND_DESCRIPTION = messages.previousLevelCommandDescription().toString();
    private final String title;
    private final List<IShellCommand> commands;
    private final Map<String, IShellCommand> commandsMap;
    private final IShellCommand defaultCommand;
    private final ShellCommandParser parser;
    private final String historyFilePath;
    private final IShellPromptProvider promptProvider;
    private final char nameSeparator;
    private final ShellNode rootNode = new ShellNode("", null, null);
    private ShellNode contextNode = rootNode;
    private LineReader lineReader;
    
    public Shell(String title, List<IShellCommand> commands, IShellCommand defaultCommand, boolean loadFromServices,
        String historyFilePath, IShellPromptProvider promptProvider, char nameSeparator)
    {
        Assert.notNull(commands);
        
        commands = new ArrayList<IShellCommand>(commands);
        commands.addAll(new DefaultShellCommandProvider().getCommands());
        if (loadFromServices)
        {
            List<IShellCommandProvider> providers = Services.loadProviders(IShellCommandProvider.class);
            for (IShellCommandProvider provider : providers)
                commands.addAll(provider.getCommands());
        }
        
        this.title = title;
        Map<String, IShellCommand> commandsMap = new TreeMap<String, IShellCommand>();
        for (IShellCommand command : commands)
        {
            String name = command.getName();
            commandsMap.put(name, command);
            String[] path = name.split("[" + nameSeparator + "]");
            rootNode.ensure(Arrays.asList(path), command);
        }
        this.commands = Immutables.wrap(commands);
        this.commandsMap = commandsMap;
        this.defaultCommand = defaultCommand;
        this.parser = new ShellCommandParser(commands, defaultCommand, nameSeparator);
        this.historyFilePath = historyFilePath;
        this.promptProvider = promptProvider;
        this.nameSeparator = nameSeparator;
    }
    
    public LineReader getLineReader()
    {
        return lineReader;
    }
    
    public ShellNode getContextNode()
    {
        return contextNode;
    }
    
    @Override
    public String getTitle()
    {
        return title;
    }
    
    @Override
    public List<IShellCommand> getCommands()
    {
        return commands;
    }
    
    @Override
    public IShellCommand getDefaultCommand()
    {
        return defaultCommand;
    }
    
    @Override
    public IShellCommand findCommand(String name)
    {
        Assert.notNull(name);
        
        return commandsMap.get(name);
    }
    
    @Override
    public IShellCommandParser getParser()
    {
        return parser;
    }

    @Override
    public char getNameSeparator()
    {
        return nameSeparator;
    }
    
    @Override
    public String getUsage()
    {
        StringBuilder builder = new StringBuilder();
        if (title != null)
        {
            builder.append(title);
            builder.append("\n\n");
        }
        
        boolean first = true;
        for (IShellCommand command : commandsMap.values())
        {
            if (first)
                first = false;
            else
                builder.append("\n\n");
            
            builder.append(command.getUsage());
        }
        
        return builder.toString();
    }
    
    @Override
    public void run()
    {
        Terminal terminal = null;
        try
        {
            terminal = TerminalBuilder.builder().system(true).jna(true).build();
            
            if (title != null)
                terminal.writer().print(title + "\n\n");
            
            LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder().terminal(terminal).appName(title)
                .completer(createCompleter()).highlighter(createHighlighter());
            if (historyFilePath != null)
                lineReaderBuilder.variable(LineReader.HISTORY_FILE, historyFilePath);
            
            String[] defaultPrompt = new String[]{null, null};
            lineReader = lineReaderBuilder.build();
            ShellContext context = new ShellContext(lineReader, this);
            while (true)
            {
                String[] prompt;
                if (promptProvider != null)
                    prompt = promptProvider.getPrompt(context);
                else
                    prompt = defaultPrompt;
                
                try
                {
                    String line = lineReader.readLine(prompt[0], prompt[1], null, null);
                    
                    List<Pair<IShellCommand, Map<String, Object>>> parsedCommands = parser.parseCommands(context.getPath(), line);
                    execute(context, parsedCommands);
                }
                catch (UserInterruptException e)
                {
                    break;
                }
                catch (EndOfFileException e)
                {
                    break;
                }
                catch (InvalidArgumentException e)
                {
                    terminal.writer().write(new AttributedStringBuilder()
                        .style(AttributedStyle.DEFAULT.background(AttributedStyle.RED))
                        .append(e.getMessage()).toAnsi());
                }
            }
        }
        catch (Exception e)
        {
            throw new SystemException(e);
        }
        finally
        {
            lineReader = null;
            IOs.close(terminal);
        }
    }

    public void changeLevel(String name)
    {
        if (name.equals(PREVIOUS_LEVEL_COMMAND))
        {
            Assert.checkState(contextNode.getParent() != null);
            contextNode = contextNode.getParent();
        }
        else
        {
            ShellNode child = contextNode.getChildren().get(name);
            Assert.notNull(child);
            Assert.checkState(child.getCommand() instanceof ShellCommandNamespace);
            contextNode = child;
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

    private void execute(ShellContext context, List<Pair<IShellCommand, Map<String, Object>>> parsedCommands)
    {
        Object result = null;
        boolean first = true;
        for (Pair<IShellCommand, Map<String, Object>> parsedCommand : parsedCommands)
        {
            IShellCommand command = parsedCommand.getKey();
            Map<String, Object> parameters = parsedCommand.getValue();
            
            if (first)
                first = false;
            else if (command.getDefaultParameter() != null && result != null)
            {
                String key = command.getDefaultParameter().getKey();
                if (command.getDefaultParameter().isUnique())
                {
                    if (!parameters.containsKey(key))
                        parameters.put(key, result);
                }
                else
                {
                    List<Object> values = (List<Object>)parameters.get(key);
                    if (values == null)
                    {
                        values = new ArrayList<Object>();
                        parameters.put(key, values);
                    }
                    
                    values.add(result);
                }
            }
            
            result = command.getExecutor().execute(context, parameters);
        }
        
        if (result != null)
            lineReader.getTerminal().writer().print(result.toString());
    }
    
    interface IMessages
    {
        @DefaultMessage("go to previous level")
        ILocalizedMessage previousLevelCommandDescription();
    }
}
