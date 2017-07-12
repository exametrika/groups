/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl;

import java.util.List;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IChannelObserver;
import com.exametrika.common.messaging.ICompositeChannel;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.SyncCompletionHandler;

/**
 * The {@link CompositeChannel} represents a message-oriented composite channel.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class CompositeChannel implements ICompositeChannel
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final long TIMEOUT = 10000;
    protected final String channelName;
    protected final ILogger logger = Loggers.get(CompositeChannel.class);
    protected final IMarker marker;
    protected final LiveNodeManager liveNodeManager;
    protected final ChannelObserver channelObserver;
    protected final List<IChannel> subChannels;
    protected final IChannel mainSubChannel;
    protected final ICompartment compartment;
    protected volatile boolean started;
    protected volatile boolean stopped;
    
    public CompositeChannel(String channelName, LiveNodeManager liveNodeManager, ChannelObserver channelObserver, 
        List<IChannel> subChannels, IChannel mainSubChannel, ICompartment compartment)
    {
        Assert.notNull(channelName);
        Assert.notNull(liveNodeManager);
        Assert.notNull(channelObserver);
        Assert.notNull(subChannels);
        Assert.notNull(mainSubChannel);
        Assert.notNull(compartment);
        
        this.channelName = channelName;
        this.marker = Loggers.getMarker(channelName);
        this.liveNodeManager = liveNodeManager;
        this.channelObserver = channelObserver;
        this.subChannels = Immutables.wrap(subChannels);
        this.mainSubChannel = mainSubChannel;
        this.compartment = compartment;
    }

    @Override
    public  List<IChannel> getSubChannels()
    {
        return subChannels;
    }
    
    @Override
    public  IChannel getMainSubChannel()
    {
        return mainSubChannel;
    }
    
    @Override
    public ICompartment getCompartment()
    {
        return compartment;
    }
    
    @Override
    public ILiveNodeProvider getLiveNodeProvider()
    {
        return liveNodeManager;
    }
    
    @Override
    public IChannelObserver getChannelObserver()
    {
        return channelObserver;
    }

    @Override
    public void send(IMessage message)
    {
        mainSubChannel.send(message);
    }

    @Override
    public void send(List<IMessage> messages)
    {
        mainSubChannel.send(messages);
    }

    @Override
    public ISink register(IAddress destination, IFeed feed)
    {
        return mainSubChannel.register(destination, feed);
    }

    @Override
    public void unregister(ISink sink)
    {
        mainSubChannel.unregister(sink);
    }

    @Override
    public void connect(String connection)
    {
        mainSubChannel.connect(connection);
    }

    @Override
    public void connect(IAddress connection)
    {
        mainSubChannel.connect(connection);
    }

    @Override
    public void disconnect(String connection)
    {
        mainSubChannel.disconnect(connection);
    }

    @Override
    public void disconnect(IAddress connection)
    {
        mainSubChannel.disconnect(connection);
    }

    @Override
    public String canonicalize(String connection)
    {
        return mainSubChannel.canonicalize(connection);
    }
    
    @Override
    public void start()
    {
        synchronized (this)
        {
            Assert.checkState(!started);
            
            started = true;
        }
        
        compartment.start();
        channelObserver.start();
        
        final SyncCompletionHandler startHandler = new SyncCompletionHandler();
        compartment.offer(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    for (IChannel subChannel : subChannels)
                        subChannel.start();
                    
                    liveNodeManager.start();
                    
                    doStart();
                    
                    startHandler.onSucceeded(null);
                }
                catch (Throwable e)
                {
                    startHandler.onFailed(e);
                }
            }
        });
        startHandler.await(TIMEOUT);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.channelStarted());
    }

    @Override
    public void stop()
    {
        synchronized (this)
        {
            if (!started || stopped)
                return;
            
            stopped = true;
        }
        
        final SyncCompletionHandler stopHandler = new SyncCompletionHandler();
        compartment.offer(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    for (IChannel subChannel : subChannels)
                        subChannel.stop();
                    
                    liveNodeManager.stop();
                    
                    doStop();
                    
                    stopHandler.onSucceeded(null);
                }
                catch (Throwable e)
                {
                    stopHandler.onFailed(e);
                }
            }
        });
        stopHandler.await(TIMEOUT);
        
        channelObserver.stop();
        compartment.stop();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.channelStopped());
    }

    @Override
    public String toString()
    {
        return channelName;
    }
    
    protected void doStart()
    {
    }
    
    protected void doStop()
    {
    }
    
    private interface IMessages
    {
        @DefaultMessage("Channel has been started.")
        ILocalizedMessage channelStarted();
        @DefaultMessage("Channel has been stopped.")
        ILocalizedMessage channelStopped();
    }
}
