/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.Arrays;
import java.util.List;

import com.exametrika.common.utils.Assert;



/**
 * The {@link ShellNamespaceBuilder} defines a shell command namespace builder.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class ShellNamespaceBuilder
{
    private final ShellCommandsBuilder parent;
    private String key;
    private List<String> names;
    private String description;
    private String shortDescription;

    public ShellNamespaceBuilder(ShellCommandsBuilder parent)
    {
        Assert.notNull(parent);
        
        this.parent = parent;
    }
    
    public ShellNamespaceBuilder key(String key)
    {
        this.key = key;
        return this;
    }
    
    public ShellNamespaceBuilder names(String... names)
    {
        this.names = Arrays.asList(names);
        return this;
    }
    
    public ShellNamespaceBuilder description(String description)
    {
        this.description = description;
        return this;
    }
    
    public ShellNamespaceBuilder shortDescription(String description)
    {
        this.shortDescription = description;
        return this;
    }
    
    public ShellCommandsBuilder end()
    {
        ShellCommandNamespace namespace = new ShellCommandNamespace(key, names, description, shortDescription);
        parent.addCommand(namespace);
        return parent;
    }
}
