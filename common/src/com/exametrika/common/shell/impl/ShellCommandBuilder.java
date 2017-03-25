/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.shell.IParameterConverter;
import com.exametrika.common.shell.IParameterValidator;
import com.exametrika.common.shell.IShellCommandExecutor;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;



/**
 * The {@link ShellCommandBuilder} defines a shell command builder.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ShellCommandBuilder
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private String name;
    private String description;
    private IParameterValidator validator;
    private List<ShellCommandParameter> parameters = new ArrayList<ShellCommandParameter>();
    private ShellCommandParameter unnamedParameter;
    private IShellCommandExecutor executor;
    private List<ShellCommand> commands = new ArrayList<ShellCommand>();

    public ShellCommandBuilder setName(String name)
    {
        this.name = name;
        return this;
    }
    
    public ShellCommandBuilder setDescription(String description)
    {
        this.description = description;
        return this;
    }
    
    public ShellCommandBuilder setValidator(IParameterValidator validator)
    {
        this.validator = validator;
        return this;
    }
    
    /**
     * Adds command line without argument.
     *
     * @param key parameter key
     * @param paramNames list of parameter names
     * @param format parameter format
     * @param description parameter description
     * @param required is parameter required?
     * @return builder
     */
    public ShellCommandBuilder addParameter(String key, List<String> paramNames, String format, String description, boolean required)
    {
        return addParameter(key, paramNames, format, description, required, false, true, null, null);
    }
    
    /**
     * Adds command parameter.
     *
     * @param key parameter key
     * @param paramNames list of parameter names
     * @param format parameter format
     * @param description parameter description
     * @param required is parameter required
     * @param hasArgument does parameter have an argument?
     * @param unique is parameter unique? Parameter must be unique if it does not have an argument
     * @param converter parameter converter. Must not be specified if parameter does not have an argument. If converter
     * is not specified, {@link String} value type parameter is assumed. Parameters without arguments always have
     * null as parameter value
     * @param defaultValue parameter default value. Must not be specified if parameter does not have an argument or
     * parameter is required. If default value has type {@link String} and converter is specified, converter is used
     * to convert default value
     * @return builder
     */
    public ShellCommandBuilder addParameter(String key, List<String> paramNames, String format, String description, boolean required, 
        boolean hasArgument, boolean unique, IParameterConverter converter, Object defaultValue)
    {
        Assert.notNull(key);
        Assert.notNull(paramNames);
        Assert.notNull(format);
        Assert.notNull(description);
        
        if (!hasArgument && (converter != null || !unique || defaultValue != null))
            throw new InvalidArgumentException(messages.noArgumentConverterNonUniqueDefaultValueError(key));
        if (required && defaultValue != null)
            throw new InvalidArgumentException(messages.requiredDefaultValueError(key));
        
        ShellCommandParameter parameter = new ShellCommandParameter(key, paramNames, format, description, hasArgument, converter, unique, required, defaultValue);
        parameters.add(parameter);
        return this;
    }
    
    /**
     * Sets unnamed command parameter.
     *
     * @param key parameter key
     * @param format parameter format
     * @param description parameter description
     * @param required is parameter required
     * @param unique is parameter unique
     * @param converter parameter converter
     * @param defaultValue parameter default value
     * @return builder
     */
    public ShellCommandBuilder setUnnamedParameter(String key, String format, String description, boolean required, 
        boolean unique, IParameterConverter converter, Object defaultValue)
    {
        Assert.notNull(key);
        Assert.notNull(format);
        Assert.notNull(description);

        if (required && defaultValue != null)
            throw new InvalidArgumentException(messages.requiredDefaultValueError(key));
        
        unnamedParameter = new ShellCommandParameter(key, null, format, description, true, converter, unique, required, defaultValue);
        return this;
    }
    
    public ShellCommandBuilder setExecutor(IShellCommandExecutor executor)
    {
        this.executor = executor;
        return this;
    }
    
    public ShellCommandBuilder addCommand()
    {
        commands.add(buildCommand());
        
        name = null;
        description = null;
        validator = null;
        executor = null;
        parameters = new ArrayList<ShellCommandParameter>();
        unnamedParameter = null;
       
        return this;
    }
    
    public ShellCommand buildCommand()
    {
        return new ShellCommand(name, description, validator, parameters, unnamedParameter, executor);
    }
    
    public List<ShellCommand> build()
    {
        return commands;
    }
    
    interface IMessages
    {
        @DefaultMessage("Converter, non-uniqueness or default value must not be specified for parameter without argument ''{0}''.")
        ILocalizedMessage noArgumentConverterNonUniqueDefaultValueError(Object parameter);
        @DefaultMessage("Default value must not be specified for required parameter ''{0}''.")
        ILocalizedMessage requiredDefaultValueError(Object parameter);
    }
}
