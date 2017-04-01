/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.exametrika.common.shell.IShellCommandExecutor;
import com.exametrika.common.shell.IShellParameter;
import com.exametrika.common.shell.IShellParameterValidator;
import com.exametrika.common.shell.impl.ShellParameterBuilder.Type;
import com.exametrika.common.utils.Assert;



/**
 * The {@link ShellCommandBuilder} defines a shell command builder.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ShellCommandBuilder
{
    private final ShellCommandsBuilder parent;
    private String key;
    private List<String> names;
    private String description;
    private String shortDescription;
    private IShellParameterValidator validator;
    private List<IShellParameter> namedParameters = new ArrayList<IShellParameter>();
    private List<IShellParameter> positionalParameters = new ArrayList<IShellParameter>();
    private IShellParameter defaultParameter;
    private IShellCommandExecutor executor;

    public ShellCommandBuilder(ShellCommandsBuilder parent)
    {
        Assert.notNull(parent);
        
        this.parent = parent;
    }
    
    public ShellCommandBuilder key(String key)
    {
        this.key = key;
        return this;
    }
    
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
    
    public ShellParameterBuilder namedParameter()
    {
        return new ShellParameterBuilder(this, Type.NAMED);
    }
    
    public ShellParameterBuilder positionalParameter()
    {
        return new ShellParameterBuilder(this, Type.POSITIONAL);
    }
    
    public ShellParameterBuilder defaultParameter()
    {
        return new ShellParameterBuilder(this, Type.DEFAULT);
    }
    
    public ShellCommandBuilder validator(IShellParameterValidator validator)
    {
        this.validator = validator;
        return this;
    }
    
    public ShellCommandBuilder executor(IShellCommandExecutor executor)
    {
        this.executor = executor;
        return this;
    }
    
    public ShellCommandsBuilder end()
    {
        ShellCommand command = new ShellCommand(key, names, description, shortDescription, validator, namedParameters, 
            positionalParameters, defaultParameter, executor);
        
        parent.addCommand(command);
        return parent;
    }
    
    void addNamed(IShellParameter parameter)
    {
        namedParameters.add(parameter);
    }
    
    void addPositional(IShellParameter parameter)
    {
        positionalParameters.add(parameter);
    }
    
    void setDefault(IShellParameter parameter)
    {
        Assert.checkState(defaultParameter == null);
        defaultParameter = parameter;
    }
}
