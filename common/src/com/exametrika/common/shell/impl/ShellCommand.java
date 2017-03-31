/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.List;

import org.jline.utils.AttributedStringBuilder;

import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellCommandExecutor;
import com.exametrika.common.shell.IShellParameter;
import com.exametrika.common.shell.IShellParameterValidator;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;



/**
 * The {@link ShellCommand} defines a shell command.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ShellCommand implements IShellCommand
{
    private final List<String> names;
    private final String description;
    private final String shortDescription;
    private final IShellParameterValidator validator;
    private final List<IShellParameter> namedParameters;
    private final List<IShellParameter> positionalParameters;
    private final IShellParameter defaultParameter;
    private final IShellCommandExecutor executor;

    public ShellCommand(List<String> names, String description, String shortDescription, IShellParameterValidator validator,
        List<? extends IShellParameter> namedParameters, List<? extends IShellParameter> positionalParameters, 
        IShellParameter defaultParameter, IShellCommandExecutor executor)
    {
        Assert.isTrue(!Collections.isEmpty(names));
        Assert.checkState(names.indexOf(Shell.PREVIOUS_LEVEL_COMMAND) == -1);
        Assert.notNull(description);
        Assert.notNull(namedParameters);
        Assert.notNull(positionalParameters);
        Assert.notNull(executor);
        
        this.names = Immutables.wrap(names);
        this.description = description;
        this.shortDescription = shortDescription;
        this.validator = validator;
        this.namedParameters = Immutables.wrap(namedParameters);
        this.positionalParameters = Immutables.wrap(positionalParameters);
        this.defaultParameter = defaultParameter;
        this.executor = executor;
    }
    
    @Override
    public List<String> getNames()
    {
        return names;
    }
    
    @Override
    public String getDescription()
    {
        return description;
    }
    
    @Override
    public String getShortDescription()
    {
        return shortDescription;
    }
    
    @Override
    public IShellParameterValidator getValidator()
    {
        return validator;
    }
    
    @Override
    public List<IShellParameter> getNamedParameters()
    {
        return namedParameters;
    }
    
    @Override
    public List<IShellParameter> getPositionalParameters()
    {
        return positionalParameters;
    }
    
    @Override
    public IShellParameter getDefaultParameter()
    {
        return defaultParameter;
    }
    
    @Override
    public IShellCommandExecutor getExecutor()
    {
        return executor;
    }
    
    @Override
    public String getUsage(boolean colorized)
    {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        if (colorized)
            builder.style(ShellStyles.COMMAND_STYLE);
        builder.append(buildName(names));
        if (colorized)
            builder.style(ShellStyles.DEFAULT_STYLE);
        builder.append("\n\n");
        builder.append(Strings.indent(description, Shell.INDENT));
        
        if (!namedParameters.isEmpty())
        {
            builder.append("\n\n");
            
            boolean first = true;
            for (IShellParameter parameter : namedParameters)
            {
                if (first)
                    first = false;
                else
                    builder.append("\n");
                
                builder.appendAnsi(Strings.indent(parameter.getUsage(colorized), Shell.INDENT));
            }
        }
        
        if (!positionalParameters.isEmpty())
        {
            builder.append("\n\n");
            
            boolean first = true;
            for (IShellParameter parameter : positionalParameters)
            {
                if (first)
                    first = false;
                else
                    builder.append("\n");
                
                builder.appendAnsi(Strings.indent(parameter.getUsage(colorized), Shell.INDENT));
            }
        }

        if (defaultParameter != null)
        {
            builder.append("\n\n");
            builder.append(Strings.indent(defaultParameter.getUsage(colorized), Shell.INDENT));
        }
        
        return builder.toAnsi();
    }

    @Override
    public String toString()
    {
        return getUsage(false);
    }
    
    private String buildName(List<String> names)
    {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String name : names)
        {
            if (first)
                first = false;
            else
                builder.append(", ");
            
            builder.append(name);
        }
            
        return builder.toString();
    }
}
