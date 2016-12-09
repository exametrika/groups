/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.json.Json;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.MessageFlags;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpChannelAware;
import com.exametrika.common.net.ITcpChannelReader;
import com.exametrika.common.net.ITcpChannelWriter;
import com.exametrika.common.net.ITcpPacketChannel;
import com.exametrika.common.net.TcpPacket;
import com.exametrika.common.net.nio.TcpNioPacketChannel;
import com.exametrika.common.net.utils.ITcpPacketDiscardPolicy;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Debug;

/**
 * The {@link TcpConnection} represents a tcp connection.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpConnection implements ITcpChannelAware
{
    private final TcpTransport transport;
    private final ISerializationRegistry serializationRegistry;
    private final IMessageFactory messageFactory;
    private final ITcpReceiveQueue receiveQueue;
    private ITcpSendQueue sendQueue;
    private final SwitchableChannelWriter channelWriter = new SwitchableChannelWriter();
    private volatile ArrayList<TcpSink> sinks = new ArrayList<TcpSink>();
    private final boolean client;
    private TcpChannelHandshaker channelHandshaker;
    private TcpAddress localAddress;
    private volatile TcpAddress remoteAddress;
    private volatile InetSocketAddress remoteInetAddress;
    private ITcpPacketChannel<TcpPacket> channel;
    private volatile boolean connected;
    private volatile boolean closed;
    private long closeTime;
    private volatile boolean duplicate;
    private boolean added;
    
    public TcpConnection(TcpTransport transport, ITcpIncomingMessageHandler incomingMessageHandler,  
        ISerializationRegistry serializationRegistry, IMessageFactory messageFactory, ITcpThreadingModel threadingModel,
        int maxUnlockSendQueueCapacity, int minLockSendQueueCapacity, boolean client,
        IFlowController<IAddress> flowController, ITcpPacketDiscardPolicy<IMessage> discardPolicy)
    {
        Assert.notNull(transport);
        Assert.notNull(incomingMessageHandler);
        Assert.notNull(serializationRegistry);
        Assert.notNull(messageFactory);
        
        this.transport = transport;
        this.serializationRegistry = serializationRegistry;
        this.receiveQueue = threadingModel.createReceiveQueue(this, incomingMessageHandler);
        this.sendQueue = new TcpStSendQueue(this, maxUnlockSendQueueCapacity, minLockSendQueueCapacity, flowController, discardPolicy);
        this.channelWriter.sendQueue = sendQueue;
        this.client = client;
        this.messageFactory = messageFactory;
    }
    
    public ISerializationRegistry getSerializationRegistry()
    {
        return serializationRegistry;
    }
    
    public ITcpPacketChannel<TcpPacket> getChannel()
    {
        return channel;
    }
    
    public List<TcpSink> getSinks()
    {
        return sinks;
    }
    
    public void lockFlow()
    {
        synchronized (channel)
        {
            receiveQueue.lockFlow();
        }
    }
    
    public void unlockFlow()
    {
        synchronized (channel)
        {
            receiveQueue.unlockFlow();
        }
    }
    
    @Override
    public void setChannel(ITcpChannel channel)
    {
        Assert.notNull(channel);
        Assert.isNull(this.channel);
        
        this.channel = (ITcpPacketChannel<TcpPacket>)channel;
        sendQueue.setChannel(this.channel);
        channelHandshaker.logState();
    }
    
    public void setChannelHandshaker(TcpChannelHandshaker channelHandshaker)
    {
        Assert.notNull(channelHandshaker);
        Assert.isNull(this.channelHandshaker);
        
        this.channelHandshaker = channelHandshaker;
    }
    
    public boolean isDuplicate()
    {
        return duplicate;
    }
    
    public void setDuplicateSend(TcpConnection retainedConnection)
    {
        synchronized (channel)
        {
            if (retainedConnection != null)
            {
                ITcpSendQueue sendQueue = retainedConnection.sendQueue;
                ArrayList<TcpSink> sinks = retainedConnection.sinks;
                
                retainedConnection.sendQueue = this.sendQueue;
                retainedConnection.sendQueue.setConnection(retainedConnection);
                retainedConnection.channelWriter.sendQueue = this.sendQueue;
                
                retainedConnection.sinks = this.sinks;
                for (TcpSink sink : retainedConnection.sinks)
                    sink.setConnection(retainedConnection);
                
                this.sendQueue = sendQueue;
                this.sendQueue.setConnection(this);
                this.channelWriter.sendQueue = sendQueue;
                
                this.sinks = sinks;
                for (TcpSink sink : this.sinks)
                    sink.setConnection(this);
            }
            
            if (!duplicate)
            {
                duplicate = true;
                channelHandshaker.setDuplicate();
            }
        }
    }
    
    public void setDuplicate()
    {
        duplicate = true;
    }
    
    public boolean isAdded()
    {
        return added;
    }
    
    public void setAdded()
    {
        added = true;
    }
    
    public TcpTransport getTransport()
    {
        return transport;
    }
    
    public TcpAddress getLocalAddress()
    {
        Assert.notNull(localAddress);
        return localAddress;
    }
    
    public void setLocalAddress(TcpAddress localAddress)
    {
        Assert.notNull(localAddress);
        Assert.isNull(this.localAddress);
        
        this.localAddress = localAddress;
    }

    public TcpAddress getRemoteAddress()
    {
        Assert.notNull(remoteAddress);
        return remoteAddress;
    }
    
    public void setRemoteAddress(TcpAddress remoteAddress)
    {
        Assert.notNull(remoteAddress);
        Assert.isTrue(remoteInetAddress == null || remoteAddress.getAddress().equals(remoteInetAddress));
        Assert.isTrue(this.remoteAddress == null || this.remoteAddress.equals(remoteAddress));
        
        this.remoteAddress = remoteAddress;
        this.remoteInetAddress = remoteAddress.getAddress();
    }
    
    public InetSocketAddress getRemoteInetAddress()
    {
        return remoteInetAddress;
    }
    
    public void setRemoteInetAddress(InetSocketAddress remoteInetAddress)
    {
        Assert.notNull(remoteInetAddress);
        Assert.isNull(this.remoteInetAddress);
        
        this.remoteInetAddress = remoteInetAddress;
    }

    public boolean isClient()
    {
        return client;
    }
    
    public boolean isConnected()
    {
        return connected;
    }
    
    public boolean isClosed()
    {
        return closed;
    }
    
    public long getCloseTime()
    {
        return closeTime;
    }
    
    public ITcpChannelReader getChannelReader()
    {
        return receiveQueue;
    }
    
    public ITcpChannelWriter getChannelWriter()
    {
        return channelWriter;
    }
    
    public void enqueue(IMessage message)
    {
        if (message.hasOneOfFlags(MessageFlags.PARALLEL | MessageFlags.HIGH_PRIORITY))
            sendQueue.offer(message);
        else
            sendQueue.put(message);
    }
    
    public void updateReadStatus()
    {
        channel.updateReadStatus();
    }
    
    public void updateWriteStatus()
    {
        if (connected)
            channel.updateWriteStatus();
    }
    
    public TcpSink register(TcpAddress destination, IFeed feed)
    {
        Assert.notNull(destination);
        Assert.notNull(feed);
        
        TcpSink sink = new TcpSink(this, destination, feed, sendQueue, messageFactory);
        
        synchronized (channel)
        {
            TcpAddress remoteAddress = this.remoteAddress;
            if (remoteAddress != null && !remoteAddress.equals(destination))
                return null;
            
            ArrayList<TcpSink> sinks = (ArrayList<TcpSink>)this.sinks.clone();
            sinks.add(sink);
            
            this.sinks = sinks;
        }
        
        return sink;
    }
    
    public void unregister(TcpSink sink)
    {
        Assert.notNull(sink);
        
        synchronized (channel)
        {
            sink.setInvalid();
            
            ArrayList<TcpSink> sinks = (ArrayList<TcpSink>)this.sinks.clone();
            sinks.remove(sink);
            
            this.sinks = sinks;
        }
    }
    
    public void onConnected()
    {
        Assert.notNull(channel);
        
        synchronized (channel)
        {
            connected = true;
            channel.setName(remoteAddress.getName());
        }
    }
    
    public void onClosed()
    {
        synchronized (channel)
        {
            closeTime = transport.getTimeService().getCurrentTime();
            closed = true;
            
            sendQueue.close();
            
            for (TcpSink sink : sinks)
                sink.setInvalid();
            
            sinks = new ArrayList<TcpSink>();
        }
    }
    
    public void disconnect()
    {
        channel.disconnect();
    }
    
    public void close()
    {
        channel.close();
    }
    
    public void dump()
    {
        Json json = Json.object();
        json.put("remote", remoteAddress.getAddress())
            .put("local", localAddress.getAddress());
        ((TcpNioPacketChannel)channel).dump(json);
        Debug.print(json.toObject().toString());
    }

    private static class SwitchableChannelWriter implements ITcpChannelWriter
    {
        private ITcpSendQueue sendQueue;
        
        @Override
        public boolean canWrite(ITcpChannel channel)
        {
            return sendQueue.canWrite(channel);
        }

        @Override
        public void onWrite(ITcpChannel channel)
        {
            sendQueue.onWrite(channel);
        }
    }
}
