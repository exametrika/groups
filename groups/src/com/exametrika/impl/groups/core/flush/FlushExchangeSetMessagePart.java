/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.groups.core.exchange.IExchangeData;

/**
 * The {@link FlushExchangeSetMessagePart} is flush data exchange set message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FlushExchangeSetMessagePart implements IMessagePart
{
    private final List<Map<UUID, IExchangeData>> dataExchanges;
    private final int size;

    public FlushExchangeSetMessagePart(List<Map<UUID, IExchangeData>> dataExchanges)
    {
        Assert.notNull(dataExchanges);
        
        this.dataExchanges = Immutables.wrap(dataExchanges);
        
        int size = 0;
        for (Map<UUID, IExchangeData> map : dataExchanges)
        {
            for (IExchangeData exchange : map.values())
            {
                if (exchange != null)
                    size += exchange.getSize();
            }
        }
            
        this.size = size;
    }

    public List<Map<UUID, IExchangeData>> getDataExchanges()
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
