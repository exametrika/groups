/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.groups.core.multicast;

import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.groups.core.membership.GroupAddress;

/**
 * The {@link MulticastSink} represents a multicast sink.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class MulticastSink implements ISink
{
    private final SendQueue sendQueue;
    private final GroupAddress destination;
    private final IFeed feed;
    private final IMessageFactory messageFactory;
    private volatile boolean valid = true;
    private volatile boolean ready = true;

    public MulticastSink(SendQueue sendQueue, GroupAddress destination, IFeed feed, IMessageFactory messageFactory)
    {
        Assert.notNull(sendQueue);
        Assert.notNull(destination);
        Assert.notNull(feed);
        Assert.notNull(messageFactory);
        
        this.sendQueue = sendQueue;
        this.destination = destination;
        this.feed = feed;
        this.messageFactory = messageFactory;
    }

    public void setInvalid()
    {
        valid = false;
    }
    
    @Override
    public GroupAddress getDestination()
    {
        return destination;
    }

    @Override
    public void setReady(boolean ready)
    {
        this.ready = ready;
        sendQueue.updateWriteStatus();
    }

    @Override
    public IMessageFactory getMessageFactory()
    {
        return messageFactory;
    }
    
    @Override
    public boolean send(IMessage message)
    {
        Assert.isTrue(message.getDestination().equals(destination));
        
        return sendQueue.send(message);
    }
    
    public boolean canWrite()
    {
        return valid && ready;
    }

    public void onWrite()
    {
        if (valid && ready)
            feed.feed(this);
    }
}
