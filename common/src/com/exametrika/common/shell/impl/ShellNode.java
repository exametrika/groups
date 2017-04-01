/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.exametrika.common.shell.IShell;
import com.exametrika.common.shell.IShellCommand;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;

/**
 * The {@link ShellNode} is a shell command hierarchy node.
 * 
 * @threadsafety This class and its methods are not thread safe.
 */
public class ShellNode
{
    private final IShell shell;
    private final String name;
    private final ShellNode parent;
    private final Map<String, ShellNode> children = new TreeMap<String, ShellNode>();
    private IShellCommand command;
    
    public ShellNode(IShell shell, String name, ShellNode parent, IShellCommand command)
    {
        Assert.notNull(shell);
        Assert.notNull(name);
        
        this.shell = shell;
        this.name = name;
        this.parent = parent;
        this.command = command;
    }
    
    public IShell getShell()
    {
        return shell;
    }
    
    public String getName()
    {
        return name;
    }
    
    public ShellNode getParent()
    {
        return parent;
    }
    
    public Map<String, ShellNode> getChildren()
    {
        return children;
    }
    
    public IShellCommand getCommand()
    {
        return command;
    }
    
    public ShellNode ensure(List<String> path, IShellCommand command)
    {
        Assert.isTrue(!Collections.isEmpty(path));
        
        ShellNode node = this;
        for (int i = 0; i < path.size(); i++)
        {
            String segment = path.get(i);
            boolean last = i == path.size() - 1;
            ShellNode child = node.children.get(segment);
            if (child == null)
            {
                String name = buildName(path, i);
                child = new ShellNode(shell, segment, node, !last ? new ShellCommandNamespace(
                    name, java.util.Collections.singletonList(name), "", "") : null);
                node.children.put(segment, child);
                if (node.parent != null)
                {
                    if (!node.children.containsKey(Shell.PREVIOUS_LEVEL_COMMAND))
                        node.children.put(Shell.PREVIOUS_LEVEL_COMMAND, new ShellNode(shell, Shell.PREVIOUS_LEVEL_COMMAND, node, 
                            new ShellCommandNamespace(Shell.PREVIOUS_LEVEL_COMMAND, 
                                java.util.Collections.singletonList(Shell.PREVIOUS_LEVEL_COMMAND), 
                                Shell.PREVIOUS_LEVEL_COMMAND_DESCRIPTION, Shell.PREVIOUS_LEVEL_COMMAND_SHORT_DESCRIPTION)));
                }
            }
            
            node = child;
        }
        
        node.command = command;
        
        return node;
    }
    
    public ShellNode findNode(String commandName)
    {
        String[] path = commandName.split("[" + shell.getNameSeparator() + "]");
        ShellNode node = this;
        for (int i = 0; i < path.length; i++)
        {
            ShellNode child = node.children.get(path[i]);
            if (child == null)
                return null;
            
            node = child;
        }
        
        return node;
    }
    
    public IShellCommand find(String commandName)
    {
        String[] path = commandName.split("[" + shell.getNameSeparator() + "]");
        ShellNode node = this;
        for (int i = 0; i < path.length; i++)
        {
            ShellNode child = node.children.get(path[i]);
            if (child == null)
                return shell.findCommand(commandName);
            
            node = child;
        }
        
        return node.command;
    }
    
    private String buildName(List<String> path, int index)
    {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (int i= 0; i <= index; i++)
        {
            if (first)
                first = false;
            else
                builder.append(shell.getNameSeparator());
            
            builder.append(path.get(i));
        }
        
        return builder.toString();
    }
}