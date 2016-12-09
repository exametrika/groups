/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.exchange;

import java.util.Map;
import java.util.UUID;

import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link DataExchangeMessagePart} is data exchange message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class DataExchangeMessagePart implements IMessagePart
{
    private final Map<UUID, ProviderExchangeData> providerExchanges;
    private final int size;

    public DataExchangeMessagePart(Map<UUID, ProviderExchangeData> providerExchanges)
    {
        Assert.notNull(providerExchanges);
        
        this.providerExchanges = Immutables.wrap(providerExchanges);
        
        int size = 0;
        for (ProviderExchangeData exchange : providerExchanges.values())
            size += exchange.getSize();
        
        this.size = size;
    }

    public Map<UUID, ProviderExchangeData> getProviderExchanges()
    {
        return providerExchanges;
    }
    
    @Override
    public int getSize()
    {
        return size;
    }
    
    @Override
    public String toString()
    {
        return providerExchanges.toString();
    }
}
