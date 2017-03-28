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
    private final String name;
    private final String description;
    private final IShellParameterValidator validator;
    private final List<IShellParameter> namedParameters;
    private final List<IShellParameter> positionalParameters;
    private final IShellParameter defaultParameter;
    private final IShellCommandExecutor executor;

    public ShellCommand(String name, String description, IShellParameterValidator validator,
        List<? extends IShellParameter> namedParameters, List<? extends IShellParameter> positionalParameters, 
        IShellParameter defaultParameter, IShellCommandExecutor executor)
    {
        Assert.isTrue(!Strings.isEmpty(name));
        Assert.checkState(!Shell.PREVIOUS_LEVEL_COMMAND.equals(name));
        Assert.notNull(description);
        Assert.notNull(namedParameters);
        Assert.notNull(positionalParameters);
        Assert.notNull(executor);
        
        this.name = name;
        this.description = description;
        this.validator = validator;
        this.namedParameters = Immutables.wrap(namedParameters);
        this.positionalParameters = Immutables.wrap(positionalParameters);
        this.defaultParameter = defaultParameter;
        this.executor = executor;
    }
    
    @Override
    public String getName()
    {
        return name;
    }
    
    @Override
    public String getDescription()
    {
        return description;
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
            builder.style(ShellConstants.COMMAND_STYLE);
        builder.append(name);
        if (colorized)
            builder.style(ShellConstants.DEFAULT_STYLE);
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
}
