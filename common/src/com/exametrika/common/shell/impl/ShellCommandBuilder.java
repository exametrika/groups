/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellCommandExecutor;
import com.exametrika.common.shell.IShellParameter;
import com.exametrika.common.shell.IShellParameterCompleter;
import com.exametrika.common.shell.IShellParameterConverter;
import com.exametrika.common.shell.IShellParameterHighlighter;
import com.exametrika.common.shell.IShellParameterValidator;
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
    private List<String> names;
    private String description;
    private String shortDescription;
    private IShellParameterValidator validator;
    private List<IShellParameter> namedParameters = new ArrayList<IShellParameter>();
    private List<IShellParameter> positionalParameters = new ArrayList<IShellParameter>();
    private IShellParameter defaultParameter;
    private IShellCommandExecutor executor;
    private List<IShellCommand> commands = new ArrayList<IShellCommand>();
    private boolean namespace;

    public ShellCommandBuilder names(String... names)
    {
        this.names = Arrays.asList(names);
        return this;
    }
    
    public ShellCommandBuilder description(String description)
    {
        this.description = description;
        return this;
    }
    
    public ShellCommandBuilder shortDescription(String description)
    {
        this.shortDescription = description;
        return this;
    }
    
    public ShellCommandBuilder validator(IShellParameterValidator validator)
    {
        this.validator = validator;
        return this;
    }
    
    /**
     * Adds named parameter without argument.
     *
     * @param key parameter key
     * @param paramNames list of parameter names
     * @param format parameter format
     * @param description parameter description
     * @param shortDescription parameter short description or null
     * @param required is parameter required?
     * @return builder
     */
    public ShellCommandBuilder addNamedParameter(String key, List<String> paramNames, String format, String description,
        String shortDescription, boolean required)
    {
        return addNamedParameter(key, paramNames, format, description, shortDescription, required, false, true, null, null, null, null);
    }
    
    /**
     * Adds named parameter.
     *
     * @param key parameter key
     * @param paramNames list of parameter names
     * @param format parameter format
     * @param description parameter description
     * @param shortDescription parameter short description or null
     * @param required is parameter required
     * @param hasArgument does parameter have an argument?
     * @param unique is parameter unique? Parameter must be unique if it does not have an argument
     * @param converter parameter converter. Must not be specified if parameter does not have an argument. If converter
     * is not specified, {@link String} value type parameter is assumed. Parameters without arguments always have
     * null as parameter value
     * @param defaultValue parameter default value. Must not be specified if parameter does not have an argument or
     * parameter is required. If default value has type {@link String} and converter is specified, converter is used
     * to convert default value
     * @param completer completer
     * @param highlighter highlighter
     * @return builder
     */
    public ShellCommandBuilder addNamedParameter(String key, List<String> paramNames, String format, String description, 
        String shortDescription, boolean required, 
        boolean hasArgument, boolean unique, IShellParameterConverter converter, Object defaultValue,
        IShellParameterCompleter completer, IShellParameterHighlighter highlighter)
    {
        Assert.notNull(key);
        Assert.notNull(paramNames);
        Assert.notNull(format);
        Assert.notNull(description);
        
        if (!hasArgument && (converter != null || !unique || defaultValue != null))
            throw new InvalidArgumentException(messages.noArgumentConverterNonUniqueDefaultValueError(key));
        if (required && defaultValue != null)
            throw new InvalidArgumentException(messages.requiredDefaultValueError(key));
        
        ShellParameter parameter = new ShellParameter(key, paramNames, format, description, shortDescription, 
            hasArgument, converter, unique, required, defaultValue, completer, highlighter);
        namedParameters.add(parameter);
        return this;
    }
    
    /**
     * Sets positional unnamed parameter.
     *
     * @param key parameter key
     * @param format parameter format
     * @param description parameter description
     * @param shortDescription parameter short description or null
     * @param converter parameter converter. If converter is not specified, {@link String} value type parameter is assumed
     * @param completer completer
     * @param highlighter highlighter
     * @return builder
     */
    public ShellCommandBuilder addPositionalParameter(String key, String format, String description, String shortDescription,
        IShellParameterConverter converter, IShellParameterCompleter completer, IShellParameterHighlighter highlighter)
    {
        Assert.notNull(key);
        Assert.notNull(format);
        Assert.notNull(description);

        positionalParameters.add(new ShellParameter(key, null, format, description, shortDescription, true, converter, 
            true, true, null, completer, highlighter));
        return this;
    }
    
    /**
     * Sets default unnamed (last) parameter.
     *
     * @param key parameter key
     * @param format parameter format
     * @param description parameter description
     * @param shortDescription parameter short description or null
     * @param required is parameter required
     * @param unique is parameter unique
     * @param converter parameter converter. If converter is not specified, {@link String} value type parameter is assumed
     * @param defaultValue parameter default value
     * @param completer completer
     * @param highlighter highlighter
     * @return builder
     */
    public ShellCommandBuilder defaultParameter(String key, String format, String description, String shortDescription, boolean required, 
        boolean unique, IShellParameterConverter converter, Object defaultValue, IShellParameterCompleter completer,
        IShellParameterHighlighter highlighter)
    {
        Assert.notNull(key);
        Assert.notNull(format);
        Assert.notNull(description);

        if (required && defaultValue != null)
            throw new InvalidArgumentException(messages.requiredDefaultValueError(key));
        
        defaultParameter = new ShellParameter(key, null, format, description, shortDescription, true, converter, unique, required, 
            defaultValue, completer, highlighter);
        return this;
    }
    
    public ShellCommandBuilder executor(IShellCommandExecutor executor)
    {
        this.executor = executor;
        return this;
    }
    
    public ShellCommandBuilder namespace()
    {
        this.namespace = true;
        return this;
    }
    
    public ShellCommandBuilder addCommand()
    {
        commands.add(buildCommand());
        
        names = null;
        description = null;
        shortDescription = null;
        validator = null;
        executor = null;
        namedParameters = new ArrayList<IShellParameter>();
        positionalParameters = new ArrayList<IShellParameter>();
        defaultParameter = null;
        namespace = false;
       
        return this;
    }
    
    public ShellCommandBuilder addNamespace()
    {
        namespace = true;
        return addCommand();
    }
    
    public IShellCommand buildCommand()
    {
        if (!namespace)
            return new ShellCommand(names, description, shortDescription, validator, namedParameters, positionalParameters, defaultParameter, executor);
        else
            return new ShellCommandNamespace(names, description, shortDescription);
    }
    
    public List<IShellCommand> build()
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
