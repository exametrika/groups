/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IPullableSender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.utils.Pair;

/**
 * The {@link PullableSenderMock} is a mock implementation of {@link IPullableSender}.
 * 
 * @author medvedev
 */
public class PullableSenderMock implements IPullableSender
{
    List<Pair<IAddress, IMessage>> messages = new ArrayList<Pair<IAddress,IMessage>>();
    List<Pair<IAddress, Boolean>> readinesses = new ArrayList<Pair<IAddress, Boolean>>();
    Map<IAddress, SinkMock> sinks = new HashMap<IAddress, SinkMock>();
    int blockCount;
    
    public void setFull(ISink sink, boolean full)
    {
        SinkMock mock = sinks.get(sink.getDestination());
        mock.full = full;
    }
    
    public void send(IAddress destination)
    {
        SinkMock sink = sinks.get(destination);
        if (sink != null && sink.ready)
            sink.feed.feed(sink);
    }
    
    @Override
    public ISink register(IAddress destination, IFeed feed)
    {
        SinkMock sink = new SinkMock(destination, feed);
        sinks.put(destination, sink);
        return sink;
    }

    @Override
    public void unregister(ISink sink)
    {
        sinks.remove(sink.getDestination());
    }

    private class SinkMock implements ISink
    {
        private final IAddress destination;
        private final IFeed feed;
        private boolean full;
        private boolean ready = true;

        public SinkMock(IAddress destination, IFeed feed)
        {
            this.destination = destination;
            this.feed = feed;
        }
        
        @Override
        public IAddress getDestination()
        {
            return destination;
        }

        @Override
        public void setReady(boolean ready)
        {
            readinesses.add(new Pair<IAddress, Boolean>(destination, ready));
            this.ready = ready;
        }
        
        @Override
        public boolean send(IMessage message)
        {
            messages.add(new Pair<IAddress, IMessage>(destination, message));
            return !full && (blockCount == 0 || messages.size() < blockCount);
        }

        @Override
        public IMessageFactory getMessageFactory()
        {
            return null;
        }
    }
}