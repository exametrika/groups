/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.utils.Assert;

/**
 * The {@link TcpSink} represents a tcp sink.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpSink implements ISink
{
    private TcpConnection connection;
    private final UnicastAddress destination;
    private final IFeed feed;
    private final ITcpSendQueue sendQueue;
    private final IMessageFactory messageFactory;
    private volatile boolean valid = true;
    private volatile boolean ready = true;

    public TcpSink(TcpConnection connection, UnicastAddress destination, IFeed feed, ITcpSendQueue sendQueue, 
        IMessageFactory messageFactory)
    {
        Assert.notNull(connection);
        Assert.notNull(destination);
        Assert.notNull(feed);
        Assert.notNull(sendQueue);
        Assert.notNull(messageFactory);
        
        this.connection = connection;
        this.destination = destination;
        this.feed = feed;
        this.sendQueue = sendQueue;
        this.messageFactory = messageFactory;
    }

    public TcpConnection getConnection()
    {
        return connection;
    }
    
    public void setConnection(TcpConnection connection)
    {
        Assert.notNull(connection);
        
        this.connection = connection;
    }
    
    public void setInvalid()
    {
        valid = false;
    }
    
    @Override
    public UnicastAddress getDestination()
    {
        return destination;
    }

    @Override
    public void setReady(boolean ready)
    {
        this.ready = ready;
        connection.updateWriteStatus();
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
        
        return sendQueue.offer(message);
    }
    
    public boolean canWrite(ITcpChannel channel)
    {
        return valid && ready;
    }

    public boolean onWrite(ITcpChannel channel)
    {
        if (valid && ready && sendQueue.hasCapacity())
        {
            feed.feed(this);
            return true;
        }
        else
            return false;
    }
}
