/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.optimize;

import java.util.ArrayList;
import java.util.List;

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
    private List<IMessage> messages = new ArrayList<IMessage>();

    public LocalSendOptimizationProtocol(String channelName, String loggerName, IMessageFactory messageFactory, ILiveNodeProvider liveNodeProvider)
    {
        super(channelName, null, messageFactory);
        
        Assert.notNull(liveNodeProvider);
        
        this.liveNodeProvider = liveNodeProvider;
    }
    
    @Override
    public void onTimer(long currentTime)
    {
        if (!messages.isEmpty())
        {
            List<IMessage> messages = this.messages;
            this.messages = new ArrayList<IMessage>();
            
            for (IMessage message : messages)
                getReceiver().receive(message);
        }
    }
    
    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        if (message.getDestination().equals(liveNodeProvider.getLocalNode()))
            messages.add(message);
        else
            super.doSend(sender, message);
    }
    
    @Override
    protected boolean doSend(IFeed feed, ISink sink, IMessage message)
    {
        if (message.getDestination().equals(liveNodeProvider.getLocalNode()))
        {
            messages.add(message);
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
