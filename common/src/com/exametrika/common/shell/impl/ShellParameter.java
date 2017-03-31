/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.List;

import org.jline.utils.AttributedStringBuilder;

import com.exametrika.common.shell.IShellParameter;
import com.exametrika.common.shell.IShellParameterCompleter;
import com.exametrika.common.shell.IShellParameterConverter;
import com.exametrika.common.shell.IShellParameterHighlighter;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;

/**
 * The {@link ShellParameter} defines a shell command parameter.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class ShellParameter implements IShellParameter
{
    private final String key;
    private final List<String> names;
    private final String format;
    private final String description;
    private final String shortDescription;
    private final boolean hasArgument;
    private final IShellParameterConverter converter;
    private final boolean unique;
    private final boolean required;
    private final Object defaultValue;
    private final IShellParameterCompleter completer;
    private final IShellParameterHighlighter highlighter;
    
    public ShellParameter(String key, List<String> names, String format, String description, String shortDescription,
        boolean hasArgument, IShellParameterConverter converter, boolean unique, boolean required, Object defaultValue,
        IShellParameterCompleter completer, IShellParameterHighlighter highlighter)
    {
        Assert.isTrue(!Strings.isEmpty(key));
        Assert.notNull(format);
        Assert.notNull(description);
        
        this.key = key;
        this.names = Immutables.wrap(names);
        this.format = format;
        this.description = description;
        this.shortDescription = shortDescription;
        this.hasArgument = hasArgument;
        this.converter = converter;
        this.unique = unique;
        this.required = required;
        this.defaultValue = defaultValue;
        this.completer = completer;
        this.highlighter = highlighter;
    }
    
    @Override
    public Object getDefaultValue()
    {
        if (converter != null && defaultValue instanceof String)
            return converter.convert((String)defaultValue);

        return defaultValue;
    }

    @Override
    public String getKey()
    {
        return key;
    }

    @Override
    public List<String> getNames()
    {
        return names;
    }

    @Override
    public String getFormat()
    {
        return format;
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
    public boolean hasArgument()
    {
        return hasArgument;
    }

    @Override
    public IShellParameterConverter getConverter()
    {
        return converter;
    }

    @Override
    public boolean isUnique()
    {
        return unique;
    }

    @Override
    public boolean isRequired()
    {
        return required;
    }
    
    @Override
    public IShellParameterCompleter getCompleter()
    {
        return completer;
    }

    @Override
    public IShellParameterHighlighter getHighlighter()
    {
        return highlighter;
    }
    
    @Override
    public String getUsage(boolean colorized)
    {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        if (colorized)
            builder.style(ShellStyles.PARAMETER_STYLE);
        builder.append(format);
        if (colorized)
            builder.style(ShellStyles.DEFAULT_STYLE);
        
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
        
        return builder.toAnsi();
    }
    
    @Override
    public String toString()
    {
        return getUsage(false);
    }
}