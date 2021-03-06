/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.membership;

import java.util.Map;
import java.util.UUID;

import com.exametrika.api.groups.cluster.INode;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link Node} is implementation of {@link INode}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Node implements INode
{
    private final UUID id;
    private final String name;
    private final IAddress address;
    private final Map<String, Object> properties;
    private final String domain;

    public Node(IAddress address, Map<String, Object> properties, String domain)
    {
        Assert.notNull(address);
        Assert.notNull(properties);
        Assert.notNull(domain);

        this.id = address.getId();
        this.name = address.getName();
        this.address = address;
        this.properties = Immutables.wrap(properties);
        this.domain = domain;
    }

    @Override
    public UUID getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public IAddress getAddress()
    {
        return address;
    }
    
    @Override
    public <T> T findProperty(String name)
    {
        Assert.notNull(name);
        
        return (T)properties.get(name);
    }
    
    @Override
    public Map<String, Object> getProperties()
    {
        return properties;
    }
    
    @Override
    public String getDomain()
    {
        return domain;
    }
    
    @Override
    public int compareTo(INode o)
    {
        return id.compareTo(o.getId());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof Node))
            return false;

        Node node = (Node)o;
        return id.equals(node.id);
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    @Override
    public String toString()
    {
        return name + "@" + domain;
    }
}
