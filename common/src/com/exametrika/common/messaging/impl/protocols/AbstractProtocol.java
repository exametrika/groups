/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols;

import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.io.ISerializationRegistrar;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.IPullableSender;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.messaging.ISender;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ICleanupManager;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.ILifecycle;

/**
 * The {@link AbstractProtocol} represents an abstract protocol implementation.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractProtocol implements ISender, IReceiver, IPullableSender, ILifecycle, ICompartmentTimerProcessor,
    ISerializationRegistrar
{
    private static final IMessages messages = Messages.get(IMessages.class);
    protected final ILogger logger;
    protected final IMarker marker;
    protected final IMessageFactory messageFactory;
    private IReceiver receiver;
    private ISender sender;
    private IPullableSender pullableSender;
    protected ITimeService timeService;
    protected IConnectionProvider connectionProvider;
    private boolean enabled = true;
    
    /**
     * Creates an object.
     *
     * @param channelName channel name
     * @param messageFactory message factory
     */
    public AbstractProtocol(String channelName, IMessageFactory messageFactory)
    {
        this(channelName, null, messageFactory);
    }
    
    /**
     * Creates an object.
     *
     * @param channelName channel name
     * @param loggerName logger name. Can be null. If null actual class name is used as logger name
     * @param messageFactory message factory
     */
    public AbstractProtocol(String channelName, String loggerName, IMessageFactory messageFactory)
    {
        Assert.notNull(channelName);
        Assert.notNull(messageFactory);
        
        if (loggerName == null)
            loggerName = getClass().getName();
        
        logger = Loggers.get(loggerName);
        marker = Loggers.getMarker(channelName);
        this.messageFactory = messageFactory;
    }

    public final IMessageFactory getMessageFactory()
    {
        return messageFactory;
    }

    public final ILogger getLogger()
    {
        return logger;
    }
    
    public final IMarker getMarker()
    {
        return marker;
    }
    
    public final IReceiver getReceiver()
    {
        Assert.checkState(receiver != null);
        return receiver;
    }
    
    public final void setReceiver(IReceiver receiver)
    {
        Assert.notNull(receiver);
        Assert.isNull(this.receiver);
        
        this.receiver = receiver;
    }

    public final ISender getSender()
    {
        Assert.checkState(sender != null);
        return sender;
    }

    public final void setSender(ISender sender)
    {
        Assert.notNull(sender);
        Assert.isNull(this.sender);
        
        this.sender = sender;
    }

    public final IPullableSender getPullableSender()
    {
        Assert.checkState(pullableSender != null);
        return pullableSender;
    }

    public final void setPullableSender(IPullableSender sender)
    {
        Assert.notNull(sender);
        Assert.isNull(this.pullableSender);
        
        this.pullableSender = sender;
    }

    public final ITimeService getTimeService()
    {
        return timeService;
    }
    
    public void setTimeService(ITimeService timeService)
    {
        Assert.notNull(timeService);
        Assert.isNull(this.timeService);
        
        this.timeService = timeService;
    }
    
    public void setConnectionProvider(IConnectionProvider connectionProvider)
    {
        Assert.notNull(connectionProvider);
        Assert.isNull(this.connectionProvider);
        
        this.connectionProvider = connectionProvider;
    }
    
    public boolean isEnabled()
    {
        return enabled;
    }
    
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
    
    @Override
    public final void send(IMessage message)
    {
        Assert.notNull(message);
        Assert.checkState(sender != null);
     
        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.send(message));
        
        try
        {
            if (isEnabled())
                doSend(sender, message);
            else
                sender.send(message);
        }
        catch (ThreadInterruptedException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Exceptions.checkInterrupted(e);
            
            // Isolate exception
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, marker, e);
        }
    }

    @Override
    public final void receive(IMessage message)
    {
        Assert.notNull(message);
        Assert.checkState(receiver != null);
        
        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.receive(message));
        
        try
        {
            if (isEnabled())
                doReceive(receiver, message);
            else
                receiver.receive(message);
        }
        catch (ThreadInterruptedException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Exceptions.checkInterrupted(e);
            
            // Isolate exception
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, marker, e);
        }
    }

    @Override
    public final ISink register(IAddress destination, IFeed feed)
    {
        Assert.checkState(pullableSender != null);
        
        if (supportsPullSendModel())
        {
            Feed protocolFeed = new Feed(feed);
            ISink sink = pullableSender.register(destination, protocolFeed);
            if (sink != null)
                return new Sink(sink, protocolFeed);
            else
                return null;
        }
        else
            return doRegister(pullableSender, destination, feed);
    }

    @Override
    public final void unregister(ISink sink)
    {
        Assert.notNull(sink);
        Assert.checkState(pullableSender != null);
        
        if (supportsPullSendModel())
        {
            Assert.isInstanceOf(Sink.class, sink);
        
            Sink messageSink = (Sink)sink;
            pullableSender.unregister(messageSink.sink);
        }
        else
            doUnregister(pullableSender, sink);
    }
    
    @Override
    public void start()
    {
        Assert.checkState(receiver != null && sender != null && pullableSender != null);
        Assert.notNull(timeService);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.protocolStarted());
    }

    @Override
    public void stop()
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.protocolStopped());
    }

    @Override
    public void onTimer(long currentTime)
    {
    }
    
    @Override
    public void register(ISerializationRegistry registry)
    {
    }

    @Override
    public void unregister(ISerializationRegistry registry)
    {
    }
    
    /**
     * Cleans up stale protocol data.
     *
     * @param cleanupManager cleanup manager
     * @param liveNodeProvider live node provider
     * @param currentTime current time
     */
    public void cleanup(ICleanupManager cleanupManager, ILiveNodeProvider liveNodeProvider, long currentTime)
    {
    }

    /**
     * Blanks off protocol receiver.
     */
    protected final void blankOffReceiver()
    {
        receiver = this;
    }

    /**
     * Blanks off protocol sender.
     */
    protected final void blankOffSender()
    {
        sender = this;
        pullableSender = this;
    }
    
    /**
     * Blanks off protocol receiver and sender.
     */
    protected final void blankOff()
    {
        blankOffReceiver();
        blankOffSender();
    }
    
    /**
     * Returns true if pull send model is supported by protocol, false if it is not supported.
     *
     * @return true if pull send model is supported by protocol, false if it is not supported
     */
    protected boolean supportsPullSendModel()
    {
        return false;
    }
    
    /**
     * Sends message to specified sender. Can be overriden.
     *
     * @param sender sender
     * @param message message to send
     */
    protected void doSend(ISender sender, IMessage message)
    {
        sender.send(message);
    }

    /**
     * Performs custom registration of specified feed in specified pullable sender. 
     * Custom registration is performed only if current protocol does not support pullable send model.
     *
     * @param pullableSender pullable sender
     * @param destination destination
     * @param feed feed
     * @return sink
     */
    protected ISink doRegister(IPullableSender pullableSender, IAddress destination, IFeed feed)
    {
        return pullableSender.register(destination, feed);
    }
    
    /**
     * Performs custom unregistration of specified sink in specified pullable sender. 
     * Custom unregistration is performed only if current protocol does not support pullable send model.
     *
     * @param pullableSender pullable sender
     * @param sink sink
     */
    protected void doUnregister(IPullableSender pullableSender, ISink sink)
    {
        pullableSender.unregister(sink);
    }
    
    /**
     * Sends message to specified sender. Can be overriden.
     *
     * @param feed feed
     * @param sink sink
     * @param message message to send
     * @return true if full message has been sent, false if partial message has been sent and additional sends are required
     */
    protected boolean doSend(IFeed feed, ISink sink, IMessage message)
    {
        return sink.send(message);
    }

    /**
     * Sends pending messages buffered from previous partial sends.
     *
     * @param feed feed
     * @param sink sink to send
     * @return true if all pending messages are sent and internal buffers are empty, or if protocol does not support partial sends
     * false if some pending messages are waiting to send and additional sends are required
     */
    protected boolean doSendPending(IFeed feed, ISink sink)
    {
        return true;
    }
    
    /**
     * Delivers message to specified receiver. Can be overriden.
     *
     * @param receiver receiver
     * @param message message to receive
     */
    protected void doReceive(IReceiver receiver, IMessage message)
    {
        receiver.receive(message);
    }
    
    private class Sink implements ISink
    {
        private final ISink sink;
        private final Feed feed;

        public Sink(ISink sink, Feed feed)
        {
            Assert.notNull(sink);
            Assert.notNull(feed);
            
            this.sink = sink;
            this.feed = feed;
        }
        
        @Override
        public IAddress getDestination()
        {
            return sink.getDestination();
        }
        
        @Override
        public IMessageFactory getMessageFactory()
        {
            return messageFactory;
        }
        
        @Override
        public void setReady(boolean ready)
        {
            feed.ready = ready;
            feed.updateReadyStatus(sink);
        }

        @Override
        public boolean send(IMessage message)
        {
            Assert.isTrue(message.getDestination().equals(sink.getDestination()));
            Assert.checkState(!feed.hasPending);
            
            if (logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, marker, messages.send(message));
            
            try
            {
                if (!AbstractProtocol.this.doSend(feed, sink, message))
                {
                    feed.hasPending = true;
                    feed.updateReadyStatus(sink);
                    return false;
                }
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, marker, e);
            }
            
            return true;
        }
    }
    
    private class Feed implements IFeed
    {
        private final IFeed feed;
        private volatile boolean ready = true;
        private volatile boolean hasPending;

        public Feed(IFeed feed)
        {
            Assert.notNull(feed);
            
            this.feed = feed;
        }

        @Override
        public void feed(ISink sink)
        {
            try
            {
                if (hasPending)
                {
                    if (AbstractProtocol.this.doSendPending(this, sink))
                    {
                        hasPending = false;
                        updateReadyStatus(sink);
                        if (ready)
                            feed.feed(new Sink(sink, this));
                    }
                }
                else
                    feed.feed(new Sink(sink, this));
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, marker, e);
            }
        }
        
        private void updateReadyStatus(ISink sink)
        {
            sink.setReady(ready | hasPending);
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Protocol has been started.")
        ILocalizedMessage protocolStarted();
        @DefaultMessage("Protocol has been stopped.")
        ILocalizedMessage protocolStopped();
        @DefaultMessage("Message has been sent:\n{0}.")
        ILocalizedMessage send(IMessage message);
        @DefaultMessage("Message has been received:\n{0}.")
        ILocalizedMessage receive(IMessage message);
    }
}
