/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.net.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentDispatcher;
import com.exametrika.common.compartment.impl.Compartment;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.net.ITcpChannel;
import com.exametrika.common.net.ITcpChannelAware;
import com.exametrika.common.net.ITcpDispatcher;
import com.exametrika.common.net.ITcpFactory;
import com.exametrika.common.net.ITcpPacketChannel;
import com.exametrika.common.net.ITcpServer;
import com.exametrika.common.net.TcpAbstractChannel;
import com.exametrika.common.net.TcpChannelException;
import com.exametrika.common.net.TcpException;
import com.exametrika.common.net.nio.socket.ITcpSelector;
import com.exametrika.common.net.nio.socket.TcpSelector;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.IOs;



/**
 * The {@link TcpNioDispatcher} is a TCP NIO dispatcher.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class TcpNioDispatcher implements ITcpDispatcher, ITcpFactory, ICompartmentDispatcher, ITimeService
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TcpNioDispatcher.class);
    private final long channelTimeout;
    private final long maxChannelIdlePeriod;
    private final ITcpSelector selector;
    private Compartment compartment;
    private final String name;
    private final IMarker marker;
    private final long cleanupPeriod;
    private long lastCleanupTime;
    private volatile boolean stopped;
    private volatile int suspendCount;
    private final AtomicInteger serverCount = new AtomicInteger();

    /**
     * Creates a new object.
     *
     * @param channelTimeout channel timeout in milliseconds. Channel is forcefully closed when connect or disconnect time exceeds this timeout.
     *        0 means infinite timeout
     * @param maxChannelIdlePeriod maximal period in milliseconds when channel is idle (unused).
     *        channel is gracefully disconnected when idle time exceeds this period. 0 means infinite period
     * @param name dispatcher's name. Can be null
     */
    public TcpNioDispatcher(long channelTimeout, long maxChannelIdlePeriod, String name)
    {
        this.channelTimeout = channelTimeout;
        this.maxChannelIdlePeriod = maxChannelIdlePeriod;
        this.cleanupPeriod = 500;
        
        try
        {
            this.selector = new TcpSelector();
        }
        catch (IOException e)
        {
            throw new TcpException(e);
        }
        
        this.name = name;
        this.marker = Loggers.getMarker("local:" + this);
    }
    
    public ITcpSelector getSelector()
    {
        return selector;
    }
    
    public IMarker getMarker()
    {
        return marker;
    }
    
    public String getName()
    {
        return name != null ? name : messages.unknown().toString();
    }
    
    void decrementServerCount()
    {
        serverCount.decrementAndGet();
    }
    
    public boolean isMainThread()
    {
        return compartment.isMainThread();
    }
    
    void addDisconnectEvent(ITcpChannel channel)
    {
        compartment.offer(new DisconnectEvent(channel));
    }
    
    void addCloseEvent(ITcpChannel channel)
    {
        compartment.offer(new CloseEvent(channel));
    }

    public void suspend()
    {
        synchronized (compartment)
        {
            if (isMainThread())
                return;
            
            suspendCount++;
            
            if (suspendCount == 1);
                selector.wakeup();
        }
        
    }
    
    public void resume()
    {
        synchronized (compartment)
        {
            if (isMainThread())
                return;
            
            suspendCount--;
            if (suspendCount == 0)
                compartment.notify();
        }
    }
    
    @Override
    public long getCurrentTime()
    {
        return compartment.getCurrentTime();
    }

    @Override
    public int getServerCount()
    {
        return serverCount.get();
    }

    @Override
    public int getChannelCount()
    {
        return selector.keys().size() - serverCount.get();
    }    

    @Override
    public void beforeClose()
    {
        synchronized (compartment)
        {
            suspendCount = 0;
            compartment.notify();
        }
    }

    @Override
    public void close()
    {
        synchronized (compartment)
        {
            Assert.isTrue(selector.keys().isEmpty());
            
            IOs.close(selector);
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.dispatcherClosed());
        }
    }

    @Override
    public ITcpServer createServer(ITcpServer.Parameters parameters)
    {
        serverCount.incrementAndGet();
        
        if (!stopped)
        {
            try
            {
                suspend();
                return new TcpNioServer(parameters, this);
            }
            finally
            {
                resume();
            }
        }

        throw new TcpException(messages.dispatcherClosed());
    }
    
    @Override
    public ITcpChannel createClient(InetSocketAddress remoteAddress, InetAddress bindAddress, ITcpChannel.Parameters parameters)
    {
        if (!stopped)
        {
            try
            {
                suspend();
                
                ITcpChannel channel = new TcpNioPacketChannel(remoteAddress, bindAddress, 
                    (ITcpPacketChannel.Parameters)parameters, this);
                
                if (parameters.data instanceof ITcpChannelAware)
                    ((ITcpChannelAware)parameters.data).setChannel(channel);
                
                return channel;
            }
            finally
            {
                resume();
            }
        }
        
        throw new TcpException(messages.dispatcherClosed());
    }

    @Override
    public void setCompartment(ICompartment compartment)
    {
        Assert.notNull(compartment);
        Assert.isNull(this.compartment);
        
        this.compartment = (Compartment)compartment;
    }

    @Override
    public void block(long period)
    {
        if (suspendCount > 0)
        {
            waitSuspended(period);
            return;
        }

        try
        {
            dispatch(period);
            
            cleanupChannels();
        }
        catch (ThreadInterruptedException e)
        {
            throw e;
        }
        catch (TcpChannelException e)
        {
            Exceptions.checkInterrupted(e);
            
            // Isolate exception
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, ((TcpAbstractChannel)e.getChannel()).getMarker(), e);
        }
        catch (Exception e)
        {
            Exceptions.checkInterrupted(e);
            
            // Isolate exception
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        }
    }

    @Override
    public void wakeup()
    {
        selector.wakeup();
    }

    @Override
    public boolean canFinish(boolean stopRequested)
    {
        if (stopRequested)
            return canStop();
        
        return false;
    }

    @Override 
    public String toString()
    {
        return getName();
    }

    private void dispatch(long period)
    {
        try
        {
            if (period > 0)
                selector.select(period);
            else
                selector.selectNow();
        }
        catch (IOException e)
        {
            throw new TcpException(e);
        }
        
        if (!selector.selectedKeys().isEmpty())
        {
            for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext();)
            {
                SelectionKey key = it.next();
                
                if (key.attachment() instanceof TcpNioAbstractChannel)
                {
                    TcpNioAbstractChannel channel = (TcpNioAbstractChannel)key.attachment();
                    boolean canRemove = true;
                       
                    if (!key.isValid())
                        channel.close();
                    else if (key.isConnectable())
                        channel.handleConnect();
                    else 
                    {
                        if (key.isReadable() || channel.hasReadData())
                            canRemove = channel.handleRead();
                        
                        if (key.isValid() && key.isWritable())
                            channel.handleWrite();
                    }
                    
                    if (canRemove)
                        it.remove();
                }
                else if (key.attachment() instanceof TcpNioServer)
                {
                    TcpNioServer server = (TcpNioServer)key.attachment();
                    
                    if (!key.isValid())
                        server.close();
                    else if (key.isAcceptable() && !compartment.isStopRequested())
                        server.handleAccept();
                    
                    it.remove();
                }
                else
                    Assert.error();
            }
        }
    }

    private boolean canStop()
    {
        try
        {
            for (SelectionKey key : selector.keys())
            {
                if (key.attachment() instanceof ITcpChannel)
                {
                    ITcpChannel channel = (ITcpChannel)key.attachment();
                    if (channel.isConnected())
                        channel.disconnect();
                }
                else if (key.attachment() instanceof ITcpServer)
                {
                    ITcpServer server = (ITcpServer)key.attachment();
                    if (server.isOpened())
                        server.close();
                }
                else
                    Assert.error();
            }
        }
        catch (ThreadInterruptedException e)
        {
            throw e;
        }
        catch (TcpChannelException e)
        {
            Exceptions.checkInterrupted(e);
            
            // Isolate exception
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, ((TcpAbstractChannel)e.getChannel()).getMarker(), e);
        }
        catch (Exception e)
        {
            Exceptions.checkInterrupted(e);
            
            // Isolate exception
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        }
        
        return selector.keys().isEmpty();
    }

    private void waitSuspended(long period)
    {
        if (period == 0)
            return;
        
        synchronized (compartment)
        {
            try
            {
                compartment.wait(period);
            }
            catch (InterruptedException e)
            {
                throw new ThreadInterruptedException(e);
            }
        }
    }
    
    private void cleanupChannels()
    {
        long currentTime = compartment.getCurrentTime();
        if (currentTime - lastCleanupTime < cleanupPeriod)
            return;
        
        lastCleanupTime = currentTime;
            
        for (SelectionKey key : selector.keys())
        {
            if (!(key.attachment() instanceof TcpAbstractChannel))
                continue;
            
            TcpAbstractChannel channel = (TcpAbstractChannel)key.attachment();
            
            channel.onTimer(this);
            
            long lastAccessTime = Math.max(channel.getLastReadTime(), channel.getLastWriteTime());
            if (channelTimeout > 0 && !channel.isConnected() && currentTime - lastAccessTime > channelTimeout)
            {
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, channel.getMarker(), messages.timedOutChannelClosing());
                channel.close();
            }
                
            if (maxChannelIdlePeriod > 0 && channel.isConnected() && currentTime - lastAccessTime > maxChannelIdlePeriod)
            {
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, channel.getMarker(), messages.idleChannelDisconnecting());
                channel.disconnect();
            }
        }
    }

    private static class DisconnectEvent implements Runnable
    {
        private final ITcpChannel channel;

        public DisconnectEvent(ITcpChannel channel)
        {
            this.channel = channel;
        }

        @Override
        public void run()
        {
            channel.disconnect();
        }
    }
    
    private static class CloseEvent implements Runnable
    {
        private final ITcpChannel channel;

        public CloseEvent(ITcpChannel channel)
        {
            this.channel = channel;
        }

        @Override
        public void run()
        {
            channel.close();
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("TCP event dispatcher has been closed.")
        ILocalizedMessage dispatcherClosed();
        @DefaultMessage("TCP channel is idle. Disconnecting...")
        ILocalizedMessage idleChannelDisconnecting();
        @DefaultMessage("TCP channel is timed out. Closing...")
        ILocalizedMessage timedOutChannelClosing();
        @DefaultMessage("(unknown)")
        ILocalizedMessage unknown();
    }
}
