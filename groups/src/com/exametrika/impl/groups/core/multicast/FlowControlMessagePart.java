/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.UUID;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;

/**
 * The {@link FlowControlMessagePart} is a flow control message part.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FlowControlMessagePart implements IMessagePart
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final UUID flowId;
    private final boolean blocked;

    public FlowControlMessagePart(UUID flowId, boolean blocked)
    {
        Assert.notNull(flowId);
        
        this.flowId = flowId;
        this.blocked = blocked;
    }
    
    public UUID getFlowId()
    {
        return flowId;
    }
    
    public boolean isBlocked()
    {
        return blocked;
    }
    
    @Override
    public int getSize()
    {
        return 17;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(flowId, blocked).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("flow-id: {0}, blocked: {1}")
        ILocalizedMessage toString(UUID id, boolean blocked);
    }
}

