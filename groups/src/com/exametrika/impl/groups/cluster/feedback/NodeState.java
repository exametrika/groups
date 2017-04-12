/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.UUID;

import com.exametrika.common.utils.Assert;

/**
 * The {@link NodeState} is a node state.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class NodeState implements INodeState
{
    private final String domain;
    private final UUID id;
    private final State state;

    public NodeState(String domain, UUID id, State state)
    {
        Assert.notNull(domain);
        Assert.notNull(id);
        Assert.notNull(state);
        
        this.domain = domain;
        this.id = id;
        this.state = state;
    }
    
    @Override
    public String getDomain()
    {
        return domain;
    }
    
    @Override
    public UUID getId()
    {
        return id;
    }

    @Override
    public State getState()
    {
        return state;
    }
}
