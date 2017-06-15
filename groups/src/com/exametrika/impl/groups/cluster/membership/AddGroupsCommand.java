/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.io.Serializable;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.groups.cluster.management.ICommand;

/**
 * The {@link AddGroupsCommand} is an add groups command.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class AddGroupsCommand implements ICommand, Serializable
{
    private final List<GroupDefinition> groupDefinitions;
    
    public AddGroupsCommand(List<GroupDefinition> groupDefinitions)
    {
        Assert.notNull(groupDefinitions);
        
        this.groupDefinitions = Immutables.wrap(groupDefinitions);
    }

    public List<GroupDefinition> getGroupDefinitions()
    {
        return groupDefinitions;
    }
    
    @Override
    public String toString()
    {
        return groupDefinitions.toString();
    }
}
