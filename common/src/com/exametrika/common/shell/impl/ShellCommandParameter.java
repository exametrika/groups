/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.List;

import com.exametrika.common.shell.IParameterConverter;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;

/**
 * The {@link ShellCommandParameter} defines a shell command parameter.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class ShellCommandParameter
{
    private final String key;
    private final List<String> names;
    private final String format;
    private final String description;
    private final boolean hasArgument;
    private final IParameterConverter converter;
    private final boolean unique;
    private final boolean required;
    private final Object defaultValue;
    
    public ShellCommandParameter(String key, List<String> names, String format, String description, boolean hasArgument, 
        IParameterConverter converter, boolean unique, boolean required, Object defaultValue)
    {
        Assert.notNull(key);
        Assert.notNull(format);
        Assert.notNull(description);
        
        this.key = key;
        this.names = Immutables.wrap(names);
        this.format = format;
        this.description = description;
        this.hasArgument = hasArgument;
        this.converter = converter;
        this.unique = unique;
        this.required = required;
        this.defaultValue = defaultValue;
    }
    
    public Object getDefaultValue()
    {
        if (converter != null && defaultValue instanceof String)
            return converter.convert((String)defaultValue);

        return defaultValue;
    }

    public String getKey()
    {
        return key;
    }

    public List<String> getNames()
    {
        return names;
    }

    public String getFormat()
    {
        return format;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean hasArgument()
    {
        return hasArgument;
    }

    public IParameterConverter getConverter()
    {
        return converter;
    }

    public boolean isUnique()
    {
        return unique;
    }

    public boolean isRequired()
    {
        return required;
    }
    
    public String getUsage()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(format);
        
        if (format.length() < Shell.INDENT)
        {
            builder.append(Strings.duplicate(' ', Shell.INDENT - format.length()));
            builder.append(Strings.indent(description, Strings.duplicate(' ', Shell.INDENT * 2), false));
        }
        else
        {
            builder.append("\n");
            builder.append(Strings.indent(description, Shell.INDENT * 2));
        }
        
        return builder.toString();
    }
    
    @Override
    public String toString()
    {
        return getUsage();
    }
}