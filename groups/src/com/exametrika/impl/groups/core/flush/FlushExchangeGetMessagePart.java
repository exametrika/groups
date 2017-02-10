/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.flush;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.groups.core.exchange.IExchangeData;

/**
 * The {@link FlushExchangeGetMessagePart} is a flush data exchange get message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-Anse 
 */

public final class FlushExchangeGetMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Set<UUID> failedMembers;
    private final Set<UUID> leftMembers;
    private final List<IExchangeData> dataExchanges;
    private final int size;

    public FlushExchangeGetMessagePart(Set<UUID> failedMembers, Set<UUID> leftMembers,
        List<IExchangeData> dataExchanges)
    {
        Assert.notNull(failedMembers);
        Assert.notNull(leftMembers);
        Assert.notNull(dataExchanges);
        
        this.failedMembers = Immutables.wrap(failedMembers);
        this.leftMembers = Immutables.wrap(leftMembers);
        this.dataExchanges = Immutables.wrap(dataExchanges);
        
        int size = (failedMembers.size() + leftMembers.size()) * 16;
        for (IExchangeData exchange : dataExchanges)
        {
            if (exchange != null)
                size += exchange.getSize();
        }
        
        this.size = size;
    }
    
    public Set<UUID> getFailedMembers()
    {
        return failedMembers;
    }
    
    public Set<UUID> getLeftMembers()
    {
        return leftMembers;
    }
    
    public List<IExchangeData> getDataExchanges()
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
        return messages.toString(failedMembers, leftMembers, dataExchanges).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("failed members: {0}, failed members: {1}, data exchanges: {2}")
        ILocalizedMessage toString(Set<UUID> failedMembers, Set<UUID> leftMembers, List<IExchangeData> exchanges);
    }
}

