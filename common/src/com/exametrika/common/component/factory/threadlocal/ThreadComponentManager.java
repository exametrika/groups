/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.factory.threadlocal;

import java.util.LinkedHashSet;
import java.util.Set;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;


/**
 * The {@link ThreadComponentManager} is an implementation of {@link IThreadComponentManager}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ThreadComponentManager implements IThreadComponentManager
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ThreadComponentManager.class);
    private volatile LinkedHashSet<IThreadComponentListener> listeners = new LinkedHashSet<IThreadComponentListener>();
    
    @Override
    public void createThreadComponents()
    {
        Set<IThreadComponentListener> listeners = this.listeners;
        
        for (IThreadComponentListener listener : listeners)
        {
            try
            {
                listener.onCreateThreadComponents();
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
                    logger.log(LogLevel.ERROR, e);
            }
        }
    }

    @Override
    public void destroyThreadComponents()
    {
        Set<IThreadComponentListener> listeners = this.listeners;
        
        for (IThreadComponentListener listener : listeners)
        {
            try
            {
                listener.onDestroyThreadComponents();
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
                    logger.log(LogLevel.ERROR, e);
            }
        }
    }

    @Override
    public synchronized void addThreadComponentListener(IThreadComponentListener listener)
    {
        Assert.notNull(listener);
        
        LinkedHashSet<IThreadComponentListener> listeners = (LinkedHashSet<IThreadComponentListener>)this.listeners.clone();
        listeners.add(listener);

        this.listeners = listeners;
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.listenerAdded(listener));
    }

    @Override
    public synchronized void removeThreadComponentListener(IThreadComponentListener listener)
    {
        Assert.notNull(listener);
        
        if (!listeners.contains(listener))
            return;
        
        LinkedHashSet<IThreadComponentListener> listeners = (LinkedHashSet<IThreadComponentListener>)this.listeners.clone();
        listeners.remove(listener);

        this.listeners = listeners;
    
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.listenerRemoved(listener));
    }

    @Override
    public synchronized void removeAllThreadComponentListeners()
    {
        this.listeners = new LinkedHashSet<IThreadComponentListener>();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.allListenersRemoved());
    }
    
    private interface IMessages
    {
        @DefaultMessage("Thread component listener ''{0}'' is added.")
        ILocalizedMessage listenerAdded(Object listener);
        @DefaultMessage("Thread component listener ''{0}'' is removed.")
        ILocalizedMessage listenerRemoved(Object listener);
        @DefaultMessage("All thread component listeners are removed.")
        ILocalizedMessage allListenersRemoved();        
    }
}
