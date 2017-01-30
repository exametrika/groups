/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import java.util.Map;
import java.util.UUID;

import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.groups.core.exchange.IExchangeData;

/**
 * The {@link FlushDataExchangeMessagePart} is flush data exchange message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FlushDataExchangeMessagePart implements IMessagePart
{
    private final Map<UUID, IExchangeData> dataExchanges;
    private final int size;

    public FlushDataExchangeMessagePart(Map<UUID, IExchangeData> dataExchanges)
    {
        Assert.notNull(dataExchanges);
        
        this.dataExchanges = Immutables.wrap(dataExchanges);
        
        int size = 0;
        for (IExchangeData exchange : dataExchanges.values())
            size += exchange.getSize();
        
        this.size = size;
    }

    public Map<UUID, IExchangeData> getDataExchanges()
    {
        return dataExchanges;
    }
    
    @Override
    public int getSize()
    {
        return size;
    }
    
    @Override
    public String toString()
    {
        return dataExchanges.toString();
    }
}
