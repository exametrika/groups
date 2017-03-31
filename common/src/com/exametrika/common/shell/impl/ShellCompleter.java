/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellContext;
import com.exametrika.common.shell.IShellParameter;
import com.exametrika.common.shell.IShellParameterCompleter;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ShellCompleter} is a shell command completer.
 * 
 * @threadsafety This class and its methods are not thread safe.
 */
public class ShellCompleter implements Completer
{
    private final ShellNode rootNode;
    private final IShellCommand defaultCommand;
    private final char nameSeparator;
    private final IShellContext context;
    private final char namedParameterPrefix;

    public ShellCompleter(ShellNode rootNode, IShellCommand defaultCommand, char nameSeparator,
        IShellContext context, char namedParameterPrefix)
    {
        Assert.notNull(rootNode);
        Assert.notNull(context);
        
        this.rootNode = rootNode;
        this.defaultCommand = defaultCommand;
        this.nameSeparator = nameSeparator;
        this.context = context;
        this.namedParameterPrefix = namedParameterPrefix;
    }
    
    @Override
    public void complete(LineReader reader, ParsedLine parsedLine, List<Candidate> candidates)
    {
        String line = parsedLine.line().substring(0, parsedLine.cursor());
        List<TokenInfo> args = parseArgs(line);
        Set<IShellParameter> parameters = new HashSet<IShellParameter>();
        CompletionInfo completionInfo = parseCommands(context.getPath(), line, args, parameters);
        if (completionInfo == null)
            return;
        
        if (completionInfo.type == CompletionType.COMMAND)
        {
            ShellNode contextNode = ((Shell)context.getShell()).getContextNode();
            for (ShellNode child : contextNode.getChildren().values())
            {
                if (child.getName().startsWith(completionInfo.prefix))
                {
                    Candidate candidate = new Candidate(child.getName(), child.getName(), null, 
                        child.getCommand().getShortDescription(), null, null, true);
                    candidates.add(candidate);
                }
            }
        }
        else if (completionInfo.type == CompletionType.PARAMETER)
        {
            IShellCommand command = completionInfo.command;
            for (IShellParameter parameter : command.getNamedParameters())
            {
                for (String name : parameter.getNames())
                {
                    if (name.startsWith(completionInfo.prefix) && (!parameter.isUnique() || !parameters.contains(parameter)))
                        candidates.add(new Candidate(name, name, null, parameter.getShortDescription(), null, null, true));
                }
            }
             
            boolean positionalsNotSet = false;
            for (IShellParameter parameter : command.getPositionalParameters())
            {
                if (!parameters.contains(parameter))
                {
                    if (parameter.getCompleter() != null)
                    {
                        List<IShellParameterCompleter.Candidate> list = parameter.getCompleter().complete(context, completionInfo.prefix);
                        for (IShellParameterCompleter.Candidate value : list)
                            candidates.add(new Candidate(value.value, value.displayName, null, value.description, null, null, true));
                    }
                    else
                        candidates.add(new Candidate("", parameter.getFormat(), null, parameter.getShortDescription(), null, null, true));
                    
                    positionalsNotSet = true;
                    break;
                }
            }
            
            IShellParameter defaultParameter = command.getDefaultParameter();
            if (!positionalsNotSet && defaultParameter != null && (!defaultParameter.isUnique() || !parameters.contains(defaultParameter)))
            {
                if (defaultParameter.getCompleter() != null)
                {
                    List<IShellParameterCompleter.Candidate> list = defaultParameter.getCompleter().complete(context, completionInfo.prefix);
                    for (IShellParameterCompleter.Candidate value : list)
                        candidates.add(new Candidate(value.value, value.displayName, null, value.description, null, null, true));
                }
                else
                    candidates.add(new Candidate("", defaultParameter.getFormat(), null, defaultParameter.getShortDescription(), null, null, true));
            }
        }
        else if (completionInfo.type == CompletionType.PARAMETER_ARGUMENT)
        {
            IShellParameter parameter = completionInfo.parameter;
            if (parameter.getCompleter() != null)
            {
                List<IShellParameterCompleter.Candidate> list = parameter.getCompleter().complete(context, completionInfo.prefix);
                for (IShellParameterCompleter.Candidate value : list)
                    candidates.add(new Candidate(value.value, value.displayName, null, value.description, null, null, true));
            }
            else
                candidates.add(new Candidate("", parameter.getFormat(), null, parameter.getShortDescription(), null, null, true));
        }
        else
            Assert.error();
    }
    
    private CompletionInfo parseCommands(String context, String line, List<TokenInfo> args,
        Set<IShellParameter> parameters)
    {
        CompletionInfo completionInfo = new CompletionInfo();
        if (args.isEmpty())
        {
            completionInfo.type = CompletionType.COMMAND;
            completionInfo.prefix = "";
            return completionInfo;
        }
        
        boolean first = true;
        IShellCommand command = null;
        List<TokenInfo> commandArgs = new ArrayList<TokenInfo>();
        for (int i = 0; i < args.size(); i++)
        {
            TokenInfo arg = args.get(i);
            boolean last = i == args.size() - 1;
            if (first)
            {
                String argStr = line.substring(arg.start, arg.start + arg.length);
                String qualifiedCommandName;
                String commandName;
                if (argStr.startsWith("/"))
                {
                    qualifiedCommandName = argStr.substring(1);
                    commandName = qualifiedCommandName;
                }
                else
                {
                    commandName = argStr;
                    if (!context.isEmpty())
                        qualifiedCommandName = context + nameSeparator + argStr;
                    else
                        qualifiedCommandName = argStr;
                }
                
                command = rootNode.find(qualifiedCommandName);
                if (command == null)
                {
                    command = rootNode.getShell().findCommand(commandName);
                    if (command == null)
                        command = defaultCommand;
                }
                
                if (last)
                {
                    if (arg.start + arg.length == line.length())
                    {
                        completionInfo.type = CompletionType.COMMAND;
                        completionInfo.prefix = line.substring(arg.start, arg.start + arg.length);
                    }
                    else if (command != null)
                    {
                        completionInfo.type = CompletionType.PARAMETER;
                        completionInfo.prefix = "";
                        completionInfo.command = command;
                    }
                    else
                        completionInfo = null;
                    
                    return completionInfo;
                }
                
                if (command == null)
                    return null;
               
                first = false;
            }
            else
                commandArgs.add(arg);
        }
        
        Assert.notNull(command);
        Assert.checkState(args.size() > 1);
        parseCommandParameters(command, line, commandArgs, parameters);
        
        completionInfo.command = command;
        TokenInfo lastToken = args.get(args.size() - 1);
        if (lastToken.start + lastToken.length == line.length())
        {
            if (lastToken.argument)
            {
                completionInfo.type = CompletionType.PARAMETER_ARGUMENT;
                completionInfo.prefix = line.substring(lastToken.start, lastToken.start + lastToken.length);
                completionInfo.parameter = lastToken.parameter;
            }
            else
            {
                completionInfo.type = CompletionType.PARAMETER;
                completionInfo.prefix = line.substring(lastToken.start, lastToken.start + lastToken.length);
            }
        }
        else
        {
            if (lastToken.parameter != null && lastToken.parameter.hasArgument() && !lastToken.argument)
            {
                completionInfo.type = CompletionType.PARAMETER_ARGUMENT;
                completionInfo.prefix = "";
                completionInfo.parameter = lastToken.parameter;
            }
            else
            {
                completionInfo.type = CompletionType.PARAMETER;
                completionInfo.prefix = "";
            }
        }
        return completionInfo;
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
                    args.clear();
                
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
    
    private void parseCommandParameters(IShellCommand command, String line, List<TokenInfo> args,
        Set<IShellParameter> parameters)
    {
        for (IShellParameter parameter : command.getNamedParameters())
            parseParameter(parameter, line, args, parameters, false);
        
        for (IShellParameter parameter : command.getPositionalParameters())
            parseParameter(parameter, line, args, parameters, true);
        
        if (command.getDefaultParameter() != null)
            parseParameter(command.getDefaultParameter(), line, args, parameters, false);
    }
    
    private void parseParameter(IShellParameter parameter, String line, List<TokenInfo> args,
        Set<IShellParameter> parameters, boolean positional)
    {
        while (true)
        {
            int i = findParameter(parameter.getNames(), line, args);
            if (i != -1)
            {
                TokenInfo value = args.remove(i);
                value.parameter = parameter;
                parameters.add(parameter);
                
                if (parameter.hasArgument())
                {
                    if (parameter.getNames() != null)
                    {
                        if (i < args.size() && line.charAt(args.get(i).start) != namedParameterPrefix)
                        {
                            value = args.remove(i);
                            value.parameter = parameter;
                            value.argument = true;
                        }
                    }
                    else
                        value.argument = true;
                }
            }
            else
                break;
            
            if (positional)
                break;
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
        else
        {
            for (int i = 0; i < args.size(); i++)
            {
                TokenInfo token = args.get(i);
                String arg = line.substring(token.start, token.start + token.length);
                if (arg.charAt(0) != namedParameterPrefix)
                    return i;
            }
        }
        
        return -1;
    }
    
    private static class TokenInfo
    {
        int start;
        int length;
        IShellParameter parameter;
        boolean argument;
    }
    
    private enum CompletionType
    {
        COMMAND,
        PARAMETER,
        PARAMETER_ARGUMENT
    }
    
    private static class CompletionInfo
    {
        CompletionType type;
        String prefix;
        IShellCommand command;
        IShellParameter parameter;
    }
}