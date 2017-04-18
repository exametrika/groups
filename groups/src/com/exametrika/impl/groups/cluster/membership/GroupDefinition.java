/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICondition;

/**
 * The {@link GroupDefinition} is a group definition.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupDefinition
{
    private final UUID id;
    private final String name;
    private final ICondition<INode> nodeFilter;
    private final int minNodeCount;
    private final int maxNodeCount;
    private final String type;
    
    public GroupDefinition(UUID id, String name, ICondition<INode> nodeFilter, int minNodeCount, int maxNodeCount,
        String type)
    {
        Assert.notNull(id);
        Assert.notNull(name);
        Assert.notNull(type);
        
        this.id = id;
        this.name = name;
        this.nodeFilter = nodeFilter;
        this.minNodeCount = minNodeCount;
        this.maxNodeCount = maxNodeCount;
        this.type = type;
    }

    public UUID getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public ICondition<INode> getNodeFilter()
    {
        return nodeFilter;
    }

    public int getMinNodeCount()
    {
        return minNodeCount;
    }

    public int getMaxNodeCount()
    {
        return maxNodeCount;
    }

    public String getType()
    {
        return type;
    }
    
    @Override
    public String toString()
    {
        return name;
    }
}
