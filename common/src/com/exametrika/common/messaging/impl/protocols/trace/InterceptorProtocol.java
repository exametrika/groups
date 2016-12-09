/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.trace;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannelListener;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;

/**
 * The {@link InterceptorProtocol} represents a protocol that intercepts incoming and outgoing messages.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class InterceptorProtocol extends AbstractProtocol implements IChannelListener
{
    private final String channelName;
    private int id;
    
    public InterceptorProtocol(String channelName, IMessageFactory messageFactory)
    {
        super(channelName, null, messageFactory);
        
        this.channelName = channelName;
    }
    
    @Override
    public void start()
    {
        super.start();
        
        id = ChannelInterceptor.INSTANCE.onStarted(channelName);
    }

    @Override
    public void stop()
    {
        ChannelInterceptor.INSTANCE.onStopped(id);
        
        super.stop();
    }
    
    @Override
    public void onNodeConnected(IAddress node)
    {
        ChannelInterceptor.INSTANCE.onNodeConnected(id, node.toString());
    }

    @Override
    public void onNodeFailed(IAddress node)
    {
        ChannelInterceptor.INSTANCE.onNodeFailed(id, node.toString());
    }

    @Override
    public void onNodeDisconnected(IAddress node)
    {
        ChannelInterceptor.INSTANCE.onNodeDisconnected(id, node.toString());
    }

    @Override
    protected void doSend(ISender sender, IMessage message)
    {
        ChannelInterceptor.INSTANCE.onMessageSent(id, message.getSize());
        
        super.doSend(sender, message);
    }
    
    @Override
    protected boolean doSend(IFeed feed, ISink sink, IMessage message)
    {
        ChannelInterceptor.INSTANCE.onMessageSent(id, message.getSize());
        
        return super.doSend(feed, sink, message);
    }

    @Override
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        ChannelInterceptor.INSTANCE.onMessageReceived(id, message.getSize());
        
        super.doReceive(receiver, message);
    }

    @Override
    protected boolean supportsPullSendModel()
    {
        return true;
    }
}
