/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellCommandParser;
import com.exametrika.common.shell.IShellParameter;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Pair;



/**
 * The {@link ShellCommandParser} defines a shell command parser.
 * 
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ShellCommandParser implements IShellCommandParser
{
    private static final IMessages messages = Messages.get(IMessages.class);
    public static final char PIPE = '|';
    private final ShellNode rootNode;
    private final IShellCommand defaultCommand;
    private final char nameSeparator;
    private final char namedParameterPrefix;
    
    public ShellCommandParser(ShellNode rootNode, IShellCommand defaultCommand, char nameSeparator,
        char namedParameterPrefix)
    {
        Assert.notNull(rootNode);
        
        this.rootNode = rootNode;
        this.defaultCommand = defaultCommand;
        this.nameSeparator = nameSeparator;
        this.namedParameterPrefix = namedParameterPrefix;
    }
    
    @Override
    public List<Pair<IShellCommand, Map<String, Object>>> parseCommands(String context, String argsStr)
    {
        List<String> args = parseArgs(argsStr);
        return parseCommands(context, args);
    }
    
    @Override
    public List<Pair<IShellCommand, Map<String, Object>>> parseCommands(String context, List<String> args)
    {
        if (args.isEmpty() && defaultCommand == null)
            return Collections.emptyList();
        
        List<Pair<IShellCommand, Map<String, Object>>> commands = new ArrayList<Pair<IShellCommand, Map<String, Object>>>();
        boolean first = true;
        IShellCommand command = null;
        Map<String, Object> parameters = null;
        List<String> commandArgs = new ArrayList<String>();
        for (String arg : args)
        {
            if (first)
            {
                String qualifiedCommandName;
                String commandName;
                if (arg.startsWith("/"))
                {
                    qualifiedCommandName = arg.substring(1);
                    commandName = qualifiedCommandName;
                }
                else
                {
                    commandName = arg;
                    if (!context.isEmpty())
                        qualifiedCommandName = context + nameSeparator + arg;
                    else
                        qualifiedCommandName = arg;
                }
                
                command = rootNode.find(qualifiedCommandName);
                if (command == null)
                {
                    command = rootNode.getShell().findCommand(commandName);
                    if (command == null)
                        command = defaultCommand;
                }
                
                if (command == null)
                    throw new InvalidArgumentException(messages.unknownCommand(arg));
                first = false;
            }
            else if (arg.length() == 1 && arg.charAt(0) == PIPE)
            {
                Assert.notNull(command);
                parameters = parseCommandParameters(command, commandArgs);
                commands.add(new Pair<IShellCommand, Map<String,Object>>(command, parameters));
                command = null;
                parameters = null;
                commandArgs.clear();
                first = true;
            }
            else
                commandArgs.add(arg);
        }
        
        if (command == null)
            command = defaultCommand;
        
        if (command != null)
        {
            parameters = parseCommandParameters(command, commandArgs);
            commands.add(new Pair<IShellCommand, Map<String,Object>>(command, parameters));
        }
        
        return commands;
    }
    
    public static List<String> parseArgs(String argsStr)
    {
        List<String> args = new ArrayList<String>();
        StringBuilder builder = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < argsStr.length(); i++)
        {
            char ch = argsStr.charAt(i);
            if (ch == '\"')
            {
                quote = !quote;
                continue;
            }
            
            if (!quote && (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == PIPE))
            {
                if (builder.length() > 0)
                {
                    args.add(builder.toString());
                    builder = new StringBuilder();
                }
                
                if (ch == PIPE)
                    args.add(Character.toString(ch));
                
                continue;
            }
            else
                builder.append(ch);
        }
        
        if (builder.length() > 0)
            args.add(builder.toString());

        return args;
    }

    private Map<String, Object> parseCommandParameters(IShellCommand command, List<String> args)
    {
        Map<String, Object> parsedParameters = new LinkedHashMap<String, Object>();
        
        for (IShellParameter parameter : command.getNamedParameters())
            parseParameter(parameter, args, parsedParameters, false);
        
        for (IShellParameter parameter : command.getPositionalParameters())
            parseParameter(parameter, args, parsedParameters, true);
        
        if (command.getDefaultParameter() != null)
            parseParameter(command.getDefaultParameter(), args, parsedParameters, false);
        
        if (args.size() == 1)
            throw new InvalidArgumentException(messages.unrecognizedParameter(args.get(0)));
        else if (args.size() > 1)
        {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (String name : args)
            {
                if (first)
                    first = false;
                else
                    builder.append(", ");
                
                builder.append(name);
            }
            
            throw new InvalidArgumentException(messages.unrecognizedParameters(builder));
        }
        
        if (command.getValidator() != null)
            command.getValidator().validate(parsedParameters);
        
        return parsedParameters;
    }
    
    private void parseParameter(IShellParameter parameter, List<String> args, Map<String, Object> parameterMap,
        boolean positional)
    {
        List<Object> values = null;
        if (!parameter.isUnique())
            values = new ArrayList<Object>();
        
        Object object = null;
        
        while (true)
        {
            int i = findParameter(parameter.getNames(), args);
            if (i != -1)
            {
                if (parameter.isUnique() && object != null)
                    throw new InvalidArgumentException(messages.duplicateParameterFound(parameter.getFormat()));
                
                String value = args.remove(i);
                if (parameter.hasArgument() && parameter.getNames() != null)
                {
                    if (i >= args.size() || args.get(i).charAt(0) == namedParameterPrefix)
                        throw new InvalidArgumentException(messages.parameterArgumentNotFound(parameter.getFormat()));
                    value = args.remove(i);
                }
                
                if (parameter.getConverter() != null)
                    object = parameter.getConverter().convert(value);
                else
                    object = value;
                
                if (parameter.isUnique())
                    parameterMap.put(parameter.getKey(), parameter.hasArgument() ? object : null);
                else
                {
                    if (!parameterMap.containsKey(parameter.getKey()))
                        parameterMap.put(parameter.getKey(), values);
                    
                    values.add(object);
                }
            }
            else 
            {
                if (parameter.isUnique())
                {
                    if (object != null)
                        break;
                    if (!parameter.isRequired())
                    {
                        if (parameter.hasArgument() && parameter.getDefaultValue() != null)
                            parameterMap.put(parameter.getKey(), parameter.getDefaultValue());
                        break;
                    }
                    
                    throw new InvalidArgumentException(messages.requiredParameterNotFound(parameter.getFormat()));
                }
                else
                {
                    if (values.isEmpty() && parameter.isRequired())
                        throw new InvalidArgumentException(messages.requiredParameterNotFound(parameter.getFormat()));
                    else if (values.isEmpty() && !parameter.isRequired() && parameter.getDefaultValue() != null)
                        values.add(parameter.getDefaultValue());
                    
                    parameterMap.put(parameter.getKey(), values);
                    break;
                }
            }
            
            if (positional)
                break;
        }
    }
    
    private int findParameter(List<String> names, List<String> args)
    {
        if (names != null)
        {
            for (int i = 0; i < args.size(); i++)
            {
                String arg = args.get(i);
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
                String arg = args.get(i);
                if (arg.charAt(0) != namedParameterPrefix)
                    return i;
            }
        }
        
        return -1;
    }
    
    interface IMessages
    {
        @DefaultMessage("Unknown command ''{0}'' is specified.")
        ILocalizedMessage unknownCommand(String command);
        @DefaultMessage("Unrecognized parameter ''{0}'' is specified.")
        ILocalizedMessage unrecognizedParameter(Object parameter);
        @DefaultMessage("Unrecognized parameters ''{0}'' is specified.")
        ILocalizedMessage unrecognizedParameters(Object parameters);
        @DefaultMessage("Required parameter ''{0}'' is not found.")
        ILocalizedMessage requiredParameterNotFound(Object format);
        @DefaultMessage("Argument of parameter ''{0}'' is not found.")
        ILocalizedMessage parameterArgumentNotFound(Object option);
        @DefaultMessage("Duplicate parameter ''{0}'' is found.")
        ILocalizedMessage duplicateParameterFound(Object option);
    }
}
