/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.factory.singleton;

import com.exametrika.common.component.factory.AbstractComponentFactory;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.ILifecycle;

/**
 * The {@link AbstractSingletonComponentFactory} is an abstract component factory for singleton components. 
 * 
 * @param <T> type name of component
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractSingletonComponentFactory<T> extends AbstractComponentFactory<T>
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AbstractSingletonComponentFactory.class);
    private final boolean lazyInitialization;
    private volatile boolean created = false;
    private volatile T instance;

    /**
     * Creates a new object.
     *
     * @param lazyInitialization lazy initialization of singleton, if true component will be initialized on demand.
     */
    public AbstractSingletonComponentFactory(boolean lazyInitialization)
    {
        this.lazyInitialization = lazyInitialization;
    }
 
    @Override
    public final T createComponent()
    {
        if (created)
            // If instance is fully created, return it without locks
            return instance;
        
        // Synchronize access when instance is not created or partially created
        synchronized (getSync())
        {
            if (instance != null)
                // Instance already created by another thread
                return instance;

            // Initialize factory, if not initialized
            init();
            
            // Create instance and publish it to current thread. Other threads will wait on synchronization lock
            instance = createInstance();
            
            // Set component instance dependencies. Circular dependencies can be resolved here because instance already published
            // to current thread
            setComponentDependencies(instance);
            
            // Pre-process component instance
            instance = processBeforeStart(instance);
            
            // Start instance
            if (instance instanceof ILifecycle)
                ((ILifecycle)instance).start();
            
            // Post-process component instance
            instance = processAfterStart(instance);
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.componentStarted(instance.toString()));
            
            // Instance is fully created. Publish it to other threads
            created = true;
            return instance;
        }
    }
    
    @Override
    public final void start()
    {
        synchronized (getSync())
        {
            super.start();
            
            if (!lazyInitialization)
                createComponent();
        }
    }
    
    @Override
    public final void stop()
    {
        synchronized (getSync())
        {
            if (instance instanceof ILifecycle)
            {
                ((ILifecycle)instance).stop();
                
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.componentStopped(instance.toString()));
            }
            
            super.stop();
        }
    }

    /**
     * Creates a component instance. If instance has circular dependencies, dependencies must be set in
     * {@link #setComponentDependencies} method. If instance does not have circular dependencies, dependencies can be set
     * in this method and in {@link #setComponentDependencies} method. 
     *
     * @return component instance
     */
    protected abstract T createInstance();
    
    /**
     * Sets component instance dependencies. Primarily used when instance has circular dependencies.
     *
     * @param instance component instance to set dependencies
     */
    protected void setComponentDependencies(T instance)
    {
        // By default does nothing
    }
    
    private interface IMessages
    {
        @DefaultMessage("Component ''{0}'' is started.")
        ILocalizedMessage componentStarted(String component);
        @DefaultMessage("Component ''{0}'' is stopped.")
        ILocalizedMessage componentStopped(String component);
    }
}
