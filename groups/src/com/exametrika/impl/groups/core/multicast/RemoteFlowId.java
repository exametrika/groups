/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import java.util.UUID;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.utils.Assert;

/**
 * The {@link RemoteFlowId} is a remote flow identifier.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class RemoteFlowId
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IAddress sender;
    private final IAddress receiver;
    private final UUID flowId;

    public RemoteFlowId(IAddress sender, IAddress receiver, UUID flowId)
    {
        Assert.notNull(sender);
        Assert.notNull(receiver);
        Assert.notNull(flowId);
        
        this.sender = sender;
        this.receiver = receiver;
        this.flowId = flowId;
    }
    
    public IAddress getSender()
    {
        return sender;
    }
    
    public IAddress getReceiver()
    {
        return receiver;
    }
    
    public UUID getFlowId()
    {
        return flowId;
    }
    
    @Override 
    public String toString()
    {
        return messages.toString(sender, receiver, flowId).toString();
    }
    
    private interface IMessages
    {
        @DefaultMessage("sender: {0}, receiver: {1}, flow-id: {2}")
        ILocalizedMessage toString(IAddress sender, IAddress receiver, UUID flowId);
    }
}

