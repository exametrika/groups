/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.shell.IShellCommandExecutor;
import com.exametrika.common.shell.IShellContext;
import com.exametrika.common.shell.IShellParameter;
import com.exametrika.common.shell.IShellParameterValidator;
import com.exametrika.common.utils.Assert;



/**
 * The {@link ShellCommandNamespace} defines a shell command namespace.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ShellCommandNamespace implements IShellCommand
{
    private final String name;
    private final String description;
    private final IShellCommandExecutor executor;

    public ShellCommandNamespace(String name, String description)
    {
        Assert.notNull(name);
        Assert.notNull(description);
        
        this.name = name;
        this.description = description;
        this.executor = new Executor();
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
    public String getUsage()
    {
        return name + " - " + description;
    }

    @Override
    public String toString()
    {
        return getUsage();
    }
    
    private class Executor implements IShellCommandExecutor
    {
        @Override
        public Object execute(IShellContext context, Map<String, Object> parameters)
        {
            ((Shell)context.getShell()).changeLevel(name);
            return null;
        }
    }
}
