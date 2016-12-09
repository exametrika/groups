/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.factory.threadlocal;

import com.exametrika.common.component.container.FactoryNotFoundException;
import com.exametrika.common.component.factory.AbstractComponentFactory;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;

/**
 * The {@link AbstractThreadComponentFactory} is an abstract component factory for thread-local components. 
 * 
 * @param <T> type name of component
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractThreadComponentFactory<T> extends AbstractComponentFactory<T> implements IThreadComponentListener
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AbstractThreadComponentFactory.class);
    private final String threadManagerName;
    private final boolean lazyInitialization;
    private final ThreadLocal<T> instance = new ThreadLocal<T>();
    private IThreadComponentManager manager;
    
    /**
     * Creates a new object.
     *
     * @param manager thread component manager
     * @param lazyInitialization lazy initialization of thread-local component, if true - component will be initialized on demand.
     */
    public AbstractThreadComponentFactory(IThreadComponentManager manager, boolean lazyInitialization)
    {
        Assert.notNull(manager);
        
        this.manager = manager;
        this.threadManagerName = null;
        this.lazyInitialization = lazyInitialization;
    }
    
    /**
     * Creates a new object. For use in container only.
     *
     * @param threadManagerName name of thread component manager component
     * @param lazyInitialization lazy initialization of thread-local component, if true - component will be initialized on demand.
     */
    public AbstractThreadComponentFactory(String threadManagerName, boolean lazyInitialization)
    {
        Assert.notNull(threadManagerName);
        
        this.threadManagerName = threadManagerName;
        this.lazyInitialization = lazyInitialization;
    }
 
    @Override
    public final T createComponent()
    {
        T component = instance.get();
        if (component == null)
        {
            // Initialize factory, if not initialized
            init();

            // Create instance and publish it to current thread
            component = createInstance();
            instance.set(component);
            
            // Set component instance dependencies. Circular dependencies can be resolved here because instance already published
            // to current thread
            setComponentDependencies(component);
            
            // Pre-process component instance
            component = processBeforeStart(component);
            
            // Start instance
            if (component instanceof ILifecycle)
                ((ILifecycle)component).start();
            
            // Post-process component instance
            component = processAfterStart(component);
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.componentStarted(component));
        }
        
        return component;
    }
    
    @Override
    public final void onCreateThreadComponents()
    {
        if (!lazyInitialization)
            createComponent(); 
    }
    
    @Override
    public final void onDestroyThreadComponents()
    {
        T component = instance.get();
        if (component instanceof ILifecycle)
        {
            ((ILifecycle)component).stop();
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.componentStopped(component));
        }
        instance.remove();
    }
    
    @Override
    public final void start()
    {
        synchronized (getSync())
        {
            super.start();
            
            manager.addThreadComponentListener(this);
        }
    }
    
    @Override
    public final void stop()
    {
        synchronized (getSync())
        {
            manager.removeThreadComponentListener(this);
            
            super.stop();
        }
    }

    @Override
    protected void setFactoryDependencies()
    {
        if (manager == null)
        {
            if (getContainer() != null)
                manager = getContainer().getComponent(threadManagerName);
            else
                throw new FactoryNotFoundException(messages.threadManagerNotFound(threadManagerName));
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
        // By default does nothing.
    }
    
    private interface IMessages
    {
        @DefaultMessage("Thread component manager ''{0}'' is not found.")
        ILocalizedMessage threadManagerNotFound(Object threadManagerName);
        @DefaultMessage("Component ''{0}'' is started.")
        ILocalizedMessage componentStarted(Object name);
        @DefaultMessage("Component ''{0}'' is stopped.")
        ILocalizedMessage componentStopped(Object name);
    }
}
