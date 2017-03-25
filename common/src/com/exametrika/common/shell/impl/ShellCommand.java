/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.List;

import com.exametrika.common.shell.IParameterValidator;
import com.exametrika.common.shell.IShellCommandExecutor;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;



/**
 * The {@link ShellCommand} defines a shell command.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ShellCommand
{
    private final String name;
    private final String description;
    private final IParameterValidator validator;
    private final List<ShellCommandParameter> parameters;
    private final ShellCommandParameter unnamedParameter;
    private final IShellCommandExecutor executor;

    public ShellCommand(String name, String description, IParameterValidator validator,
        List<ShellCommandParameter> parameters, ShellCommandParameter unnamedParameter, IShellCommandExecutor executor)
    {
        Assert.notNull(name);
        Assert.notNull(description);
        Assert.notNull(parameters);
        Assert.notNull(executor);
        
        this.name = name;
        this.description = description;
        this.validator = validator;
        this.parameters = Immutables.wrap(parameters);
        this.unnamedParameter = unnamedParameter;
        this.executor = executor;
    }
    
    public String getName()
    {
        return name;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public IParameterValidator getValidator()
    {
        return validator;
    }
    
    public List<ShellCommandParameter> getParameters()
    {
        return parameters;
    }
    
    public ShellCommandParameter getUnnamedParameter()
    {
        return unnamedParameter;
    }
    
    public IShellCommandExecutor getExecutor()
    {
        return executor;
    }
    
    public String getUsage()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(name);
        builder.append("\n\n");
        builder.append(description);
        
        if (unnamedParameter != null)
        {
            builder.append("\n\n");
            builder.append(Strings.indent(unnamedParameter.getUsage(), Shell.INDENT));
        }
        
        if (!parameters.isEmpty())
        {
            builder.append("\n\n");
            
            boolean first = true;
            for (ShellCommandParameter parameter : parameters)
            {
                if (first)
                    first = false;
                else
                    builder.append("\n");
                
                builder.append(Strings.indent(parameter.getUsage(), Shell.INDENT));
            }
        }
        
        return builder.toString();
    }

    @Override
    public String toString()
    {
        return getUsage();
    }
}
