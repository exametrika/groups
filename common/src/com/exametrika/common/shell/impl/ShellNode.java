/**
 * Copyright 2017 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.shell.impl;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
    private final String name;
    private final ShellNode parent;
    private final Map<String, ShellNode> children = new TreeMap<String, ShellNode>();
    private IShellCommand command;
    
    public ShellNode(String name, ShellNode parent, IShellCommand command)
    {
        Assert.notNull(name);
        
        this.name = name;
        this.parent = parent;
        this.command = command;
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
                child = new ShellNode(segment, node, !last ? new ShellCommandNamespace(segment, "") : null);
                node.children.put(segment, child);
                if (node.parent != null && !last)
                {
                    if (!node.children.containsKey(Shell.PREVIOUS_LEVEL_COMMAND))
                        node.children.put(Shell.PREVIOUS_LEVEL_COMMAND, new ShellNode(Shell.PREVIOUS_LEVEL_COMMAND, node, 
                            new ShellCommandNamespace(Shell.PREVIOUS_LEVEL_COMMAND, Shell.PREVIOUS_LEVEL_COMMAND_DESCRIPTION)));
                }
            }
            
            node = child;
        }
        
        node.command = command;
        
        return node;
    }
}