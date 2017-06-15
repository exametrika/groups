/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.groups.cluster.management.ICommand;

/**
 * The {@link RemoveGroupsCommand} is a remove groups command.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RemoveGroupsCommand implements ICommand, Serializable
{
    private final List<Pair<String, UUID>> groups;
    
    public RemoveGroupsCommand(List<Pair<String, UUID>> groups)
    {
        Assert.notNull(groups);
        
        this.groups = Immutables.wrap(groups);
    }

    public List<Pair<String, UUID>> getGroups()
    {
        return groups;
    }
    
    @Override
    public String toString()
    {
        return groups.toString();
    }
}
