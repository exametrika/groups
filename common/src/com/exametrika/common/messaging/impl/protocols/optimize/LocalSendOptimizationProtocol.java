/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.optimize;

import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.utils.Assert;

/**
 * The {@link LocalSendOptimizationProtocol} represents a protocol that redirects local sends directly to local receiver.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class LocalSendOptimizationProtocol extends AbstractProtocol
{
    private final ILiveNodeProvider liveNodeProvider;

    public LocalSendOptimizationProtocol(String channelName, String loggerName, IMessageFactory messageFactory, ILiveNodeProvider liveNodeProvider)
    {
        super(channelName, null, messageFactory);
        
        Assert.notNull(liveNodeProvider);
        
        this.liveNodeProvider = liveNodeProvider;
    }
    
    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        if (message.getDestination().equals(liveNodeProvider.getLocalNode()))
            getReceiver().receive(message);
        else
            super.doSend(sender, message);
    }
    
    @Override
    protected boolean doSend(IFeed feed, ISink sink, IMessage message)
    {
        if (message.getDestination().equals(liveNodeProvider.getLocalNode()))
        {
            getReceiver().receive(message);
            return true;
        }
        else
            return super.doSend(feed, sink, message);
    }
    
    @Override
    protected boolean supportsPullSendModel()
    {
        return true;
    }
}
