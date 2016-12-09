/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.List;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.groups.core.exchange.IExchangeData;

/**
 * The {@link FailureAtomicExchangeData} is a exchange data for failure atomic protocol.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FailureAtomicExchangeData implements IExchangeData
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<MissingMessageInfo> missingMessageInfos;

    public FailureAtomicExchangeData(List<MissingMessageInfo> missingMessageInfos)
    {
        Assert.notNull(missingMessageInfos);
        
        this.missingMessageInfos = Immutables.wrap(missingMessageInfos);
    }
    
    public List<MissingMessageInfo> getMissingMessageInfos()
    {
        return missingMessageInfos;
    }
    
    @Override
    public int getSize()
    {
        return missingMessageInfos.size() * 24;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(missingMessageInfos).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("missing message infos: {0}")
        ILocalizedMessage toString(List<MissingMessageInfo> missingMessageInfos);
    }
}

