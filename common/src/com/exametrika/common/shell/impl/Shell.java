/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.io.PrintWriter;
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
import org.jline.terminal.impl.DumbTerminal;
import org.jline.terminal.impl.ExternalTerminal;
import org.jline.utils.AttributedStringBuilder;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.l10n.SystemException;
import com.exametrika.common.services.Services;
import com.exametrika.common.shell.IShell;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellCommandParser;
import com.exametrika.common.shell.IShellCommandProvider;
import com.exametrika.common.shell.IShellContext;
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
public final class Shell implements IShell
{
    private static final IMessages messages = Messages.get(IMessages.class);
    public static final int INDENT = 4;
    public static final String PREVIOUS_LEVEL_COMMAND = "..";
    public static final String PREVIOUS_LEVEL_COMMAND_DESCRIPTION = messages.previousLevelCommandDescription().toString();
    public static final String PREVIOUS_LEVEL_COMMAND_SHORT_DESCRIPTION = messages.previousLevelCommandShortDescription().toString();
    private final String title;
    private final List<IShellCommand> commands;
    private final Map<String, IShellCommand> commandsMap;
    private final IShellCommand defaultCommand;
    private final ShellCommandParser parser;
    private final String historyFilePath;
    private final IShellPromptProvider promptProvider;
    private final char nameSeparator;
    private final char namedParameterPrefix;
    private boolean noColors;
    private final ShellNode rootNode = new ShellNode(this, "", null, null);
    private final ShellContext context = new ShellContext(this);
    private ShellNode contextNode = rootNode;
    private LineReader lineReader;
    
    public Shell(String title, List<IShellCommand> commands, IShellCommand defaultCommand, boolean loadFromServices,
        String historyFilePath, IShellPromptProvider promptProvider, char nameSeparator, char namedParameterPrefix, boolean noColors)
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
            List<String> names = command.getNames();
            for (String name : names)
            {
                if (command instanceof ShellCommand)
                    commandsMap.put(name, command);
                String[] path = name.split("[" + nameSeparator + "]");
                rootNode.ensure(Arrays.asList(path), command);
            }
        }
        this.commands = Immutables.wrap(commands);
        this.commandsMap = commandsMap;
        this.defaultCommand = defaultCommand;
        this.parser = new ShellCommandParser(rootNode, defaultCommand, nameSeparator, namedParameterPrefix);
        this.historyFilePath = historyFilePath;
        this.promptProvider = promptProvider;
        this.nameSeparator = nameSeparator;
        this.namedParameterPrefix = namedParameterPrefix;
        this.noColors = noColors;
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
    public boolean isNoColors()
    {
        return noColors;
    }
    
    @Override
    public String getUsage(boolean colorized)
    {
        if (noColors)
            colorized = false;
        
        AttributedStringBuilder builder = new AttributedStringBuilder();
        if (title != null)
        {
            if (colorized)
                builder.style(ShellStyles.APPLICATION_STYLE);
            builder.append(title);
            if (colorized)
                builder.style(ShellStyles.DEFAULT_STYLE);
            builder.append("\n\n");
        }
        
        boolean first = true;
        for (IShellCommand command : commandsMap.values())
        {
            if (first)
                first = false;
            else
                builder.append("\n\n");
            
            builder.appendAnsi(command.getUsage(colorized, false));
        }
        
        return builder.toAnsi();
    }
    
    @Override
    public List<Pair<IShellCommand, Map<String, Object>>> parse(String[] args)
    {
        try
        {
            return parser.parseCommands("", Arrays.asList(args));
        }
        catch (InvalidArgumentException e)
        {
            Terminal terminal = null;
            try
            {
                terminal = TerminalBuilder.builder().system(true).jna(true).build();
                if (terminal instanceof DumbTerminal || terminal instanceof ExternalTerminal)
                    noColors = true;
                
                boolean colorized = true;
                if (noColors)
                    colorized = false;
                
                AttributedStringBuilder builder = new AttributedStringBuilder();
                if (title != null)
                {
                    if (colorized)
                        builder.style(ShellStyles.APPLICATION_STYLE);
                    builder.append(title);
                    if (colorized)
                        builder.style(ShellStyles.DEFAULT_STYLE);
                    builder.append("\n\n");
                }
                if (colorized)
                    builder.style(ShellStyles.ERROR_STYLE);
                builder.append(e.getMessage());
                builder.append("\n\n");
                if (colorized)
                    builder.style(ShellStyles.DEFAULT_STYLE);
                
                builder.append(defaultCommand.getUsage(colorized, true));
                
                terminal.writer().print(builder.toAnsi());
            }
            catch (Exception ee)
            {
                throw new SystemException(ee);
            }
            finally
            {
                IOs.close(terminal);
            }
            return null;
        }    
    }
    
    @Override
    public PrintWriter getWriter()
    {
        return lineReader.getTerminal().writer();
    }
    
    @Override
    public void flush()
    {
        lineReader.getTerminal().flush();
    }

    @Override
    public IShellContext getContext()
    {
        return context;
    }
    
    @Override
    public Object executeCommand(String commandName, Map<String, Object> parameters)
    {
        IShellCommand command = findCommand(commandName);
        if (command != null)
            return command.getExecutor().execute(command, context, parameters);
        else
            throw new InvalidArgumentException(messages.commandNotFound(commandName));
    }
    
    @Override
    public void executeScript(String scriptText)
    {
        Assert.notNull(scriptText);
        
        String[] parts = scriptText.split("[\r\n]");
        List<String> commands = new ArrayList<String>();
        String lastCommand = "";
        for (String part : parts)
        {
            if (part.startsWith("#"))
                continue;
            else if (part.endsWith("\\"))
                lastCommand += part.substring(0, part.length() - 1);
            else
            {
                lastCommand += part;
                commands.add(lastCommand);
                
                lastCommand = "";
            }
        }
        
        commands.add(lastCommand);
        
        for (String line : commands)
            executeLine(line);
    }
    
    @Override
    public void run()
    {
        Terminal terminal = null;
        try
        {
            terminal = TerminalBuilder.builder().system(true).jna(true).build();
            if (terminal instanceof DumbTerminal || terminal instanceof ExternalTerminal)
                noColors = true;
            
            if (title != null)
            {
                AttributedStringBuilder builder = new AttributedStringBuilder();
                if (!noColors)
                    builder.style(ShellStyles.APPLICATION_STYLE);
                builder.append("\n");
                builder.append(title);
                if (!noColors)
                    builder.style(ShellStyles.DEFAULT_STYLE);
                builder.append("\n\n");
               
                terminal.writer().print(builder.toAnsi());
            }
            
            LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder().terminal(terminal).appName(title)
                .completer(createCompleter()).highlighter(createHighlighter());
            if (historyFilePath != null)
                lineReaderBuilder.variable(LineReader.HISTORY_FILE, historyFilePath);
            
            String[] defaultPrompt = new String[]{null, null};
            lineReader = lineReaderBuilder.build();
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
                    executeLine(line);
                }
                catch (UserInterruptException e)
                {
                    break;
                }
                catch (EndOfFileException e)
                {
                    break;
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

    public void addCommand(IShellCommand command)
    {
        Assert.notNull(command);
        
        List<String> names = command.getNames();
        for (String name : names)
        {
            if (command instanceof ShellCommand)
            {
                if (commandsMap.containsKey(name))
                    throw new SystemException(messages.commandExists(name));
                
                commandsMap.put(name, command);
            }
            String[] path = name.split("[" + nameSeparator + "]");
            rootNode.ensure(Arrays.asList(path), command);
        }
        
        Immutables.unwrap(commands).add(command);
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
            ShellNode child = rootNode.findNode(name);
            Assert.notNull(child);
            Assert.checkState(child.getCommand() instanceof ShellCommandNamespace);
            contextNode = child;
        }
    }
    
    public Object execute(ShellContext context, List<Pair<IShellCommand, Map<String, Object>>> parsedCommands)
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
            
            result = command.getExecutor().execute(command, context, parameters);
        }
        
        return result;
    }
    
    private Highlighter createHighlighter()
    {
        return new ShellHighlighter(rootNode, defaultCommand, nameSeparator, context, namedParameterPrefix);
    }

    private Completer createCompleter()
    {
        return new ShellCompleter(rootNode, defaultCommand, nameSeparator, context, namedParameterPrefix);
    }

    private void executeLine(String line)
    {
        Assert.checkState(lineReader != null);
        
        line = line.trim();
        
        try
        {
            if (line.isEmpty())
                return;
            if (line.equals("/"))
            {
                contextNode = rootNode;
                return;
            }
            
            List<Pair<IShellCommand, Map<String, Object>>> parsedCommands = parser.parseCommands(context.getPath(), line);
            Object result = execute(context, parsedCommands);
            if (result != null)
                lineReader.getTerminal().writer().print(result.toString() + "\n\n");
        }
        catch (InvalidArgumentException e)
        {
            AttributedStringBuilder builder = new AttributedStringBuilder();
            if (!noColors)
                builder.style(ShellStyles.ERROR_STYLE);
            builder.append(e.getMessage());
            if (!noColors)
                builder.style(ShellStyles.DEFAULT_STYLE);
           
            builder.append("\n\n");
            
            lineReader.getTerminal().writer().print(builder.toAnsi());
        }
    }
    
    interface IMessages
    {
        @DefaultMessage("go to previous level")
        ILocalizedMessage previousLevelCommandDescription();
        @DefaultMessage("level up")
        ILocalizedMessage previousLevelCommandShortDescription();
        @DefaultMessage("Command ''{0}'' is not found.")
        ILocalizedMessage commandNotFound(String command);
        @DefaultMessage("Command ''{0}'' already exists.")
        ILocalizedMessage commandExists(String command);
    }
}
