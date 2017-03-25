/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Pair;



/**
 * The {@link ShellCommandParser} defines a shell command parser.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ShellCommandParser
{
    static final IMessages messages = Messages.get(IMessages.class);
    private final Map<String, ShellCommand> commandsMap;
    private final ShellCommand defaultCommand;
    
    public ShellCommandParser(List<ShellCommand> commands, ShellCommand defaultCommand)
    {
        Assert.notNull(commands);
        
        Map<String, ShellCommand> commandsMap = new HashMap<String, ShellCommand>();
        for (ShellCommand command : commands)
            commandsMap.put(command.getName(), command);
        
        this.commandsMap = commandsMap;
        this.defaultCommand = defaultCommand;
    }
    
    public List<Pair<ShellCommand, Map<String, Object>>> parseCommands(String context, String argsStr)
    {
        List<String> args = parseArgs(argsStr);
        return parseCommands(context, args);
    }
    
    public List<Pair<ShellCommand, Map<String, Object>>> parseCommands(String context, List<String> args)
    {
        List<Pair<ShellCommand, Map<String, Object>>> commands = new ArrayList<Pair<ShellCommand, Map<String, Object>>>();
        boolean first = true;
        ShellCommand command = null;
        Map<String, Object> parameters = null;
        List<String> commandArgs = new ArrayList<String>();
        for (String arg : args)
        {
            if (first)
            {
                String commandName = context + ":" + arg;
                command = commandsMap.get(commandName);
                if (command == null)
                    command = defaultCommand;
                
                if (command == null)
                    throw new InvalidArgumentException(messages.unknownCommand(arg));
                first = false;
            }
            else if (arg.equals("|"))
            {
                Assert.notNull(command);
                parameters = parseCommandParameters(command, commandArgs);
                commands.add(new Pair<ShellCommand, Map<String,Object>>(command, parameters));
                command = null;
                parameters = null;
                commandArgs.clear();
                first = true;
            }
        }
        
        Assert.notNull(command);
        parameters = parseCommandParameters(command, commandArgs);
        commands.add(new Pair<ShellCommand, Map<String,Object>>(command, parameters));
        
        return commands;
    }
    
    private List<String> parseArgs(String argsStr)
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
            
            if (!quote && (ch == ' ' || ch == '\t') || ch == '|')
            {
                if (builder.length() > 0)
                {
                    args.add(builder.toString());
                    builder = new StringBuilder();
                }
                
                if (ch == '|')
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

    private  Map<String, Object> parseCommandParameters(ShellCommand command, List<String> args)
    {
        Map<String, Object> parsedParameters = new LinkedHashMap<String, Object>();
        
        for (ShellCommandParameter parameter : command.getParameters())
            parseParameter(parameter, args, parsedParameters);
        
        if (command.getUnnamedParameter() != null)
            parseParameter(command.getUnnamedParameter(), args, parsedParameters);
        
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
    
    private void parseParameter(ShellCommandParameter parameter, List<String> args, Map<String, Object> parameterMap)
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
                    if (i >= args.size() || args.get(i).charAt(0) == '-')
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
                    values.add(object);
            }
            else 
            {
                if (parameter.isUnique())
                {
                    if (object != null)
                        break;
                    if (!parameter.isRequired())
                    {
                        if (parameter.hasArgument())
                            parameterMap.put(parameter.getKey(), parameter.getDefaultValue());
                        break;
                    }
                    
                    throw new InvalidArgumentException(messages.requiredParameterNotFound(parameter.getFormat()));
                }

                if (values.isEmpty() && !parameter.isRequired())
                    values.add(parameter.getDefaultValue());
                if (values.isEmpty() && parameter.isRequired())
                    throw new InvalidArgumentException(messages.requiredParameterNotFound(parameter.getFormat()));
                
                parameterMap.put(parameter.getKey(), values);
                break;
            }
        }
    }
    
    private int findParameter(List<String> names, List<String> args)
    {
        for (int i = 0; i < args.size(); i++)
        {
            String arg = args.get(i);
            if (names != null)
            {
                for (String name : names)
                {
                    if (arg.equals(name))
                        return i;
                }
            }
            else
            {
                if (arg.charAt(0) != '-')
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
