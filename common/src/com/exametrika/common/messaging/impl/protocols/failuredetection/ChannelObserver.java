/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.failuredetection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannelListener;
import com.exametrika.common.messaging.IChannelObserver;
import com.exametrika.common.tasks.ITaskHandler;
import com.exametrika.common.tasks.ITaskQueue;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.tasks.impl.TaskExecutor;
import com.exametrika.common.tasks.impl.TaskQueue;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.ILifecycle;

/**
 * The {@link ChannelObserver} is a channel observer.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author medvedev
 */
public final class ChannelObserver implements IChannelObserver, IConnectionObserver, IFailureObserver, ILifecycle, ITaskHandler
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ChannelObserver.class);
    private final IMarker marker;
    private final TaskExecutor executor;
    private final ITaskQueue<Event> queue;
    private volatile ArrayList<IChannelListener> channelListeners = new ArrayList<IChannelListener>();
    
    public ChannelObserver(String channelName)
    {
        Assert.notNull(channelName);
        
        marker = Loggers.getMarker(channelName);
        
        TaskQueue<Event> queue = new TaskQueue<Event>();
        executor = new TaskExecutor<Event>(1, queue, this, "[" + channelName + "] channel observer thread");
        this.queue = queue;
    }
    
    @Override
    public synchronized void addChannelListener(IChannelListener listener)
    {
        Assert.notNull(listener);
        
        if (channelListeners.indexOf(listener) != -1)
            return;

        ArrayList<IChannelListener> channelListeners = (ArrayList<IChannelListener>)this.channelListeners.clone();
        channelListeners.add(listener);

        this.channelListeners = channelListeners;
    }

    @Override
    public synchronized void removeChannelListener(IChannelListener listener)
    {
        Assert.notNull(listener);
        
        if (channelListeners.indexOf(listener) == -1)
            return;

        ArrayList<IChannelListener> channelListeners = (ArrayList<IChannelListener>)this.channelListeners.clone();
        channelListeners.remove(listener);

        this.channelListeners = channelListeners;
    }

    @Override
    public synchronized void removeAllChannelListeners()
    {
        this.channelListeners = new ArrayList<IChannelListener>();
    }

    @Override
    public void onNodesConnected(Set<IAddress> nodes)
    {
        queue.offer(new Event(Type.CONNECTED, nodes));
    }

    @Override
    public void onNodesFailed(Set<IAddress> nodes)
    {
        queue.offer(new Event(Type.FAILED, nodes));
    }

    @Override
    public void onNodesLeft(Set<IAddress> nodes)
    {
        queue.offer(new Event(Type.LEFT, nodes));
    }

    @Override
    public void handle(Object task)
    {
        Event event = (Event)task;
        
        for (IAddress node : event.nodes)
        {
            switch (event.type)
            {
            case CONNECTED: 
                fireNodeConnected(node);
                break;
            case LEFT:
                fireNodeDisconnected(node);
                break;
            case FAILED:
                fireNodeFailed(node);
                break;
            default:
                Assert.error();
            }
        }
    }

    @Override
    public synchronized void start()
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.observerStarted());
        
        executor.start();
    }

    @Override
    public void stop()
    {
        executor.stop();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.observerStopped());
    }

    private void fireNodeConnected(IAddress node)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.nodeConnected(node));
        
        List<IChannelListener> channelListeners = this.channelListeners;
        for (IChannelListener listener : channelListeners)
        {
            try
            {
                listener.onNodeConnected(node);
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception from other listeners
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, marker, e);
            }
        }
    }
    
    private void fireNodeFailed(IAddress node)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.nodeFailed(node));
        
        List<IChannelListener> channelListeners = this.channelListeners;
        for (IChannelListener listener : channelListeners)
        {
            try
            {
                listener.onNodeFailed(node);
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception from other listeners
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, marker, e);
            }
        }
    }
    
    private void fireNodeDisconnected(IAddress node)
    {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.nodeDisconnected(node));
        
        List<IChannelListener> channelListeners = this.channelListeners;
        for (IChannelListener listener : channelListeners)
        {
            try
            {
                listener.onNodeDisconnected(node);
            }
            catch (ThreadInterruptedException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                Exceptions.checkInterrupted(e);
                
                // Isolate exception from other listeners
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, marker, e);
            }
        }
    }

    private enum Type
    {
        CONNECTED,
        LEFT,
        FAILED
    }
    
    private static class Event
    {
        private final Type type;
        private final Set<IAddress> nodes;
        
        public Event(Type type, Set<IAddress> nodes)
        {
            Assert.notNull(type);
            Assert.notNull(nodes);
            
            this.type = type;
            this.nodes = nodes;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Node ''{0}'' has been connected.")
        ILocalizedMessage nodeConnected(IAddress node);
        @DefaultMessage("Node ''{0}'' has been disconnected.")
        ILocalizedMessage nodeDisconnected(IAddress node);
        @DefaultMessage("Node ''{0}'' has failed.")
        ILocalizedMessage nodeFailed(IAddress node);
        @DefaultMessage("Channel observer has been started.")
        ILocalizedMessage observerStarted();
        @DefaultMessage("Channel observer has been stopped.")
        ILocalizedMessage observerStopped();
    }
}
