/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.UUID;

import com.exametrika.common.utils.Assert;

/**
 * The {@link GroupDefinition} is a group definition.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class GroupDefinition
{
    private final String domain;
    private final UUID id;
    private final String name;
    private final String nodeFilterExpression;
    private final int nodeCount;
    private final String type;
    
    public GroupDefinition(String domain, UUID id, String name, String nodeFilterExpression, int nodeCount, String type)
    {
        Assert.notNull(domain);
        Assert.notNull(id);
        Assert.notNull(name);
        Assert.notNull(type);
        
        this.domain = domain;
        this.id = id;
        this.name = name;
        this.nodeFilterExpression = nodeFilterExpression;
        this.nodeCount = nodeCount;
        this.type = type;
    }

    public String getDomain()
    {
        return domain;
    }
    
    public UUID getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getNodeFilterExpression()
    {
        return nodeFilterExpression;
    }

    public int getNodeCount()
    {
        return nodeCount;
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
