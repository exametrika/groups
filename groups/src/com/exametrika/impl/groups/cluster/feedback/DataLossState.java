/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.feedback;

import java.util.UUID;

import com.exametrika.common.utils.Assert;

/**
 * The {@link DataLossState} is a data loss state.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DataLossState implements IDataLossState
{
    private final String domain;
    private final UUID id;

    public DataLossState(String domain, UUID id)
    {
        Assert.notNull(domain);
        Assert.notNull(id);
        
        this.domain = domain;
        this.id = id;
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
}
