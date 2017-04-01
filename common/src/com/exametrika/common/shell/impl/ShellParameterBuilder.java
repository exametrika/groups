/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.Arrays;
import java.util.List;

import com.exametrika.common.shell.IShellParameterCompleter;
import com.exametrika.common.shell.IShellParameterConverter;
import com.exametrika.common.shell.IShellParameterHighlighter;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;



/**
 * The {@link ShellParameterBuilder} defines a shell parameter builder.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ShellParameterBuilder
{
    private final Type type;
    private final ShellCommandBuilder parent;
    private String key;
    private List<String> names;
    private String format;
    private String description;
    private String shortDescription;
    private boolean hasArgument;
    private IShellParameterConverter converter;
    private boolean unique;
    private boolean required;
    private Object defaultValue;
    private IShellParameterCompleter completer;
    private IShellParameterHighlighter highlighter;
    
    public enum Type
    {
        NAMED,
        POSITIONAL,
        DEFAULT
    }
    
    public ShellParameterBuilder(ShellCommandBuilder parent, Type type)
    {
        Assert.notNull(parent);
        Assert.notNull(type);
        
        this.parent = parent;
        this.type = type;
    }
    
    public ShellParameterBuilder key(String key)
    {
        this.key = key;
        return this;
    }
    
    public ShellParameterBuilder names(String... names)
    {
        this.names = Arrays.asList(names);
        return this;
    }
    
    public ShellParameterBuilder format(String format)
    {
        this.format = format;
        return this;
    }
    
    public ShellParameterBuilder description(String description)
    {
        this.description = description;
        return this;
    }
    
    public ShellParameterBuilder shortDescription(String description)
    {
        this.shortDescription = description;
        return this;
    }
    
    public ShellParameterBuilder hasArgument()
    {
        this.hasArgument = true;
        return this;
    }
    
    public ShellParameterBuilder converter(IShellParameterConverter converter)
    {
        this.converter = converter;
        return this;
    }
    
    public ShellParameterBuilder unique()
    {
        this.unique = true;
        return this;
    }
    
    
    public ShellParameterBuilder required()
    {
        this.required = true;
        return this;
    }
    
    public ShellParameterBuilder defaultValue(Object value)
    {
        this.defaultValue = value;
        return this;
    }
    
    public ShellParameterBuilder completer(IShellParameterCompleter completer)
    {
        this.completer = completer;
        return this;
    }
    
    public ShellParameterBuilder highlighter(IShellParameterHighlighter highlighter)
    {
        this.highlighter = highlighter;
        return this;
    }
    
    public ShellCommandBuilder end()
    {
        if (type == Type.NAMED)
        {
            Assert.isTrue(!Collections.isEmpty(names));
            
            ShellParameter parameter = new ShellParameter(key, names, format, description, shortDescription, 
                hasArgument, converter, unique, required, defaultValue, completer, highlighter);
            parent.addNamed(parameter);
        }
        else if (type == Type.POSITIONAL)
        {
            Assert.isNull(names);
            Assert.isNull(defaultValue);
            
            unique = true;
            required = true;
            hasArgument = true;
            
            ShellParameter parameter = new ShellParameter(key, names, format, description, shortDescription, 
                hasArgument, converter, unique, required, defaultValue, completer, highlighter);
            parent.addPositional(parameter);    
        }
        else if (type == Type.DEFAULT)
        {
            Assert.isNull(names);
            hasArgument = true;
            
            ShellParameter parameter = new ShellParameter(key, names, format, description, shortDescription, 
                hasArgument, converter, unique, required, defaultValue, completer, highlighter);
            parent.setDefault(parameter);
        }
        else
            Assert.error();
            
        return parent;
    }
}
