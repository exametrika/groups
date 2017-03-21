/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.cluster.state;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;

/**
 * The {@link StateTransferResponseMessagePart} is a state transfer message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StateTransferResponseMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final boolean first;
    private final boolean last;
    private final boolean rejected;

    public StateTransferResponseMessagePart(boolean first, boolean last, boolean rejected)
    {
        this.first = first;
        this.last = last;
        this.rejected = rejected;
    }
    
    public boolean isFirst()
    {
        return first;
    }
    
    public boolean isLast()
    {
        return last;
    }
    
    public boolean isRejected()
    {
        return rejected;
    }
    
    @Override
    public int getSize()
    {
        return 3;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(first, last, rejected).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("first: {0}, last: {1}, rejected: {2}")
        ILocalizedMessage toString(boolean first, boolean last, boolean rejected);
    }
}

