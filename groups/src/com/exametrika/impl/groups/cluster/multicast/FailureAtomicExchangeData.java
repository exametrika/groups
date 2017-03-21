/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.multicast;

import java.util.List;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.groups.cluster.exchange.IExchangeData;

/**
 * The {@link FailureAtomicExchangeData} is a exchange data for failure atomic protocol.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FailureAtomicExchangeData implements IExchangeData
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final long id;
    private final List<MissingMessageInfo> missingMessageInfos;

    public FailureAtomicExchangeData(long id, List<MissingMessageInfo> missingMessageInfos)
    {
        Assert.notNull(missingMessageInfos);
        
        this.id = id;
        this.missingMessageInfos = Immutables.wrap(missingMessageInfos);
    }
    
    @Override
    public long getId()
    {
        return id;
    }
    
    public List<MissingMessageInfo> getMissingMessageInfos()
    {
        return missingMessageInfos;
    }
    
    @Override
    public int getSize()
    {
        return 8 + missingMessageInfos.size() * 24;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(id, missingMessageInfos).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("id: {0}, missing message infos: {1}")
        ILocalizedMessage toString(long id, List<MissingMessageInfo> missingMessageInfos);
    }
}

