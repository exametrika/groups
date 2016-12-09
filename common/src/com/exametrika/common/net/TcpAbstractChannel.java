/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */

package com.exametrika.common.net;

import java.util.LinkedHashSet;
import java.util.Set;

import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Immutables;



/**
 * The {@link TcpAbstractChannel} is an abstract implementaion of {@link ITcpChannel}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class TcpAbstractChannel implements ITcpChannel
{
    private static final ILogger logger = Loggers.get(TcpAbstractChannel.class);
    protected final Set<ITcpChannelListener> channelListeners;
    protected final ITcpChannelReader channelReader;
    protected final ITcpChannelWriter channelWriter;
    private final IMarker parentMarker;
    private volatile Object data;
    private volatile String name;
    private volatile IMarker marker;
    private volatile boolean admin;
    
    public TcpAbstractChannel(ITcpChannel.Parameters parameters, IMarker parentMarker)
    {
        Assert.notNull(parameters);
        Assert.notNull(parameters.channelReader);
        Assert.notNull(parameters.channelWriter);
        Assert.notNull(parameters.channelListeners);
        Assert.notNull(parentMarker);

        this.channelListeners = Immutables.wrap(new LinkedHashSet<ITcpChannelListener>(parameters.channelListeners));
        this.channelReader = parameters.channelReader;
        this.channelWriter = parameters.channelWriter;
        this.name = parameters.name;
        this.data = parameters.data;
        this.parentMarker = parentMarker;
    }

    public final ITcpChannelWriter getChannelWriter()
    {
        return channelWriter;
    }
    
    public final Set<ITcpChannelListener> getChannelListeners()
    {
        return channelListeners;
    }
    
    public final ITcpChannelReader getChannelReader()
    {
        return channelReader;
    }
    
    @Override
    public String getName()
    {
        return name;
    }
    
    @Override
    public final synchronized void setName(String name)
    {
        Assert.notNull(name);
        this.name = name;
        updateMarker();
    }
    
    @Override
    public final IMarker getMarker()
    {
        return marker;
    }
    
    @Override
    public final <T> T getData()
    {
        return (T)data;
    }
    
    @Override
    public final <T> void setData(T data)
    {
        this.data = data;
    }
    
    @Override 
    public final boolean isAdmin()
    {
        return admin;
    }
    
    @Override 
    public final void setAdmin()
    {
        admin = true;
    }
    
    public abstract void onTimer(ITimeService timeService);
    
    protected final void updateMarker()
    {
        marker = Loggers.getMarker("connection:" + getName(), parentMarker);
        doUpdateMarker(marker);
    }
    
    protected abstract void doUpdateMarker(IMarker marker);
    
    protected final void fireConnected()
    {
        for (ITcpChannelListener listener : channelListeners)
        {
            try
            {
                listener.onConnected(this);
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
                    logger.log(LogLevel.ERROR, getMarker(), e);
            }
        }    
    }
    
    protected final void fireDisconnected()
    {
        for (ITcpChannelListener listener : channelListeners)
        {
            try
            {
                listener.onDisconnected(this);
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
                    logger.log(LogLevel.ERROR, getMarker(), e);
            }
        }    
    }
    
    protected final void fireFailed()
    {
        for (ITcpChannelListener listener : channelListeners)
        {
            try
            {
                listener.onFailed(this);
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
                    logger.log(LogLevel.ERROR, getMarker(), e);
            }
        }    
    }
    
    @Override
    protected void finalize()
    {
        close();
    }
}
