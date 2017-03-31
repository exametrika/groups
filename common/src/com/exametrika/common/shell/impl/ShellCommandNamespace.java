/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jline.utils.AttributedStringBuilder;

import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellCommandExecutor;
import com.exametrika.common.shell.IShellContext;
import com.exametrika.common.shell.IShellParameter;
import com.exametrika.common.shell.IShellParameterValidator;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;



/**
 * The {@link ShellCommandNamespace} defines a shell command namespace.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ShellCommandNamespace implements IShellCommand
{
    private final List<String> names;
    private final String description;
    private final String shortDescription;
    private final IShellCommandExecutor executor;

    public ShellCommandNamespace(List<String> names, String description, String shortDescription)
    {
        Assert.notNull(names);
        Assert.notNull(description);
        
        this.names = Immutables.wrap(names);
        this.description = description;
        this.shortDescription = shortDescription;
        this.executor = new Executor();
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
        return null;
    }
    
    @Override
    public List<IShellParameter> getNamedParameters()
    {
        return Collections.emptyList();
    }
    
    @Override
    public List<IShellParameter> getPositionalParameters()
    {
        return Collections.emptyList();
    }
    
    @Override
    public IShellParameter getDefaultParameter()
    {
        return null;
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
        
        builder.append(" - ");
        builder.append(description);
        return builder.toAnsi();
    }

    @Override
    public String toString()
    {
        return getUsage(false);
    }
    
    private class Executor implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellContext context, Map<String, Object> parameters)
        {
            ((Shell)context.getShell()).changeLevel(names.get(0));
            return null;
        }
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
