/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.ArrayList;
import java.util.List;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellContext;
import com.exametrika.common.shell.IShellParameter;
import com.exametrika.common.shell.IShellParameterHighlighter;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ShellHighlighter} is a shell command highlighter.
 * 
 * @threadsafety This class and its methods are not thread safe.
 */
public class ShellHighlighter implements Highlighter
{
    private final ShellNode rootNode;
    private final IShellCommand defaultCommand;
    private final char nameSeparator;
    private final IShellContext context;

    public ShellHighlighter(ShellNode rootNode, IShellCommand defaultCommand, char nameSeparator,
        IShellContext context)
    {
        Assert.notNull(rootNode);
        Assert.notNull(context);
        
        this.rootNode = rootNode;
        this.defaultCommand = defaultCommand;
        this.nameSeparator = nameSeparator;
        this.context = context;
    }
    
    @Override
    public AttributedString highlight(LineReader reader, String buffer)
    {
        List<TokenInfo> args = parseArgs(buffer);
        parseCommands(context.getPath(), buffer, args);
        AttributedStringBuilder builder = new AttributedStringBuilder();
        int k = 0;
        for (int i = 0; i < buffer.length(); i++)
        {
            if (k == args.size())
                break;
            
            TokenInfo arg = args.get(k);
            if (arg.start == i)
            {
                if (arg.style == Style.COMMAND)
                    builder.style(ShellConstants.COMMAND_STYLE);
                else if (arg.style == Style.PARAMETER)
                    builder.style(ShellConstants.PARAMETER_STYLE);
                else if (arg.style == Style.PARAMETER_ARGUMENT)
                {
                    if (arg.highlighter == null)
                        builder.style(ShellConstants.PARAMETER_ARGUMENT_STYLE);
                    else
                    {
                        String value = buffer.substring(arg.start, arg.start + arg.length);
                        builder.appendAnsi(arg.highlighter.highlight(context, value));
                        k++;
                        i += value.length() - 1;
                        continue;
                    }
                }
                else if (arg.style == Style.WARNING)
                    builder.style(ShellConstants.WARNING_STYLE);
                else if (arg.style == Style.ERROR)
                    builder.style(ShellConstants.ERROR_STYLE);
                else if (arg.style == Style.PIPE)
                    builder.style(ShellConstants.DEFAULT_STYLE);
                
                k++;
            }
            builder.append(buffer.charAt(i));
        }

        return builder.toAttributedString();
    }
    
    private void parseCommands(String context, String line, List<TokenInfo> args)
    {
        boolean first = true;
        IShellCommand command = null;
        TokenInfo commandToken = null;
        List<TokenInfo> commandArgs = new ArrayList<TokenInfo>();
        for (TokenInfo arg : args)
        {
            if (first)
            {
                String prefix;
                if (!context.isEmpty())
                    prefix = context + nameSeparator;
                else
                    prefix = "";
                
                String commandName = prefix + line.substring(arg.start, arg.start + arg.length);
                command = rootNode.find(commandName, nameSeparator);
                if (command == null)
                    command = defaultCommand;
                
                if (command == null)
                {
                    arg.style = Style.ERROR;
                    break;
                }
                arg.style = Style.COMMAND;
                commandToken = arg;
                first = false;
            }
            else if (arg.style == Style.PIPE)
            {
                Assert.notNull(command);
                parseCommandParameters(command, line, commandToken, commandArgs);
                command = null;
                commandToken = null;
                commandArgs.clear();
                first = true;
            }
            else
                commandArgs.add(arg);
        }
        
        Assert.notNull(command);
        parseCommandParameters(command, line, commandToken, commandArgs);
    }
    
    private List<TokenInfo> parseArgs(String argsStr)
    {
        List<TokenInfo> args = new ArrayList<TokenInfo>();
        int start = 0;
        int length = 0;
        boolean quote = false;
        for (int i = 0; i < argsStr.length(); i++)
        {
            char ch = argsStr.charAt(i);
            if (ch == '\"')
            {
                quote = !quote;
                continue;
            }
            
            if (!quote && (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == ShellCommandParser.PIPE))
            {
                if (length > 0)
                {
                    TokenInfo token = new TokenInfo();
                    token.start = start;
                    token.length = length;
                    args.add(token);
                }
                
                if (ch == ShellCommandParser.PIPE)
                {
                    TokenInfo token = new TokenInfo();
                    token.start = i;
                    token.length = 1;
                    token.style = Style.PIPE;
                    args.add(token);
                }
                
                start = i + 1;
                length = 0;
                continue;
            }
            else
                length++;
        }
        
        if (length > 0)
        {
            TokenInfo token = new TokenInfo();
            token.start = start;
            token.length = length;
            args.add(token);
        }

        return args;
    }
    
    private void parseCommandParameters(IShellCommand command, String line, TokenInfo commandToken, List<TokenInfo> args)
    {
        for (IShellParameter parameter : command.getNamedParameters())
            parseParameter(parameter, line, commandToken, args);
        
        for (IShellParameter parameter : command.getPositionalParameters())
            parseParameter(parameter, line, commandToken, args);
        
        if (command.getDefaultParameter() != null)
            parseParameter(command.getDefaultParameter(), line, commandToken, args);
        
        for (TokenInfo arg : args)
            arg.style = Style.ERROR;
    }
    
    private void parseParameter(IShellParameter parameter, String line, TokenInfo commandToken, List<TokenInfo> args)
    {
        List<Object> values = null;
        if (!parameter.isUnique())
            values = new ArrayList<Object>();
        
        Object object = null;
        
        while (true)
        {
            int i = findParameter(parameter.getNames(), line, args);
            if (i != -1)
            {
                TokenInfo value = args.remove(i);
                if (parameter.isUnique() && object != null)
                    value.style = Style.WARNING;
                else
                    value.style = Style.PARAMETER;
               
                if (parameter.hasArgument() && parameter.getNames() != null)
                {
                    if (i >= args.size())
                        value.style = Style.WARNING;
                    else
                    {
                        value = args.remove(i);
                        value.style = Style.PARAMETER_ARGUMENT;
                        value.highlighter = parameter.getHighlighter();
                    }
                }
                
                object = value;
                
                if (!parameter.isUnique())
                    values.add(object);
            }
            else 
            {
                if (parameter.isUnique())
                {
                    if (object != null)
                        break;
                    if (!parameter.isRequired())
                        break;
                    
                    commandToken.style = Style.WARNING;
                }
                else
                {
                    if (values.isEmpty() && parameter.isRequired())
                        commandToken.style = Style.WARNING;
                    else if (values.isEmpty() && !parameter.isRequired() && parameter.getDefaultValue() != null)
                        values.add(parameter.getDefaultValue());
                    break;
                }
            }
        }
    }
    
    private int findParameter(List<String> names, String line, List<TokenInfo> args)
    {
        if (names != null)
        {
            for (int i = 0; i < args.size(); i++)
            {
                TokenInfo token = args.get(i);
                String arg = line.substring(token.start, token.start + token.length);
                for (String name : names)
                {
                    if (arg.equals(name))
                        return i;
                }
            }
        }
        else if (!args.isEmpty())
            return 0;
        
        return -1;
    }
    
    private enum Style
    {
        WARNING,
        ERROR,
        COMMAND,
        PARAMETER,
        PARAMETER_ARGUMENT,
        PIPE
    }
    
    private static class TokenInfo
    {
        int start;
        int length;
        Style style;
        IShellParameterHighlighter highlighter;
    }
}