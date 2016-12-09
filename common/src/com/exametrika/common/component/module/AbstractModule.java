/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.module;

import com.exametrika.common.component.container.IComponentContainer;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.InvalidStateException;

/**
 * The {@link AbstractModule} is an abstract implementation of {@link IModule}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractModule implements IModule, ILifecycle
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AbstractModule.class);
    private final IComponentContainer container;
    private final String name;
    private boolean started;
    private boolean stopped;
    
    /**
     * Creates a new object.
     *
     * @param name module name
     * @param container module's component container
     */
    public AbstractModule(String name, IComponentContainer container)
    {
        Assert.notNull(container);
        Assert.notNull(name);
        if (!(container instanceof ILifecycle))
            throw new InvalidArgumentException(messages.mustSupportInterface(name, ILifecycle.class.getName()));
        
        this.name = name;
        this.container = container;
    }

    @Override
    public final String getName()
    {
        return name;
    }
    
    @Override
    public final IComponentContainer getContainer()
    {
        return container;
    }

    @Override
    public synchronized void start()
    {
        if (stopped)
            throw new InvalidStateException(messages.moduleAlreadyStopped(name));
        if (started)
            throw new InvalidStateException(messages.moduleAlreadyStarted(name));
        
        registerDependencies();
        registerFactories();
        ((ILifecycle)container).start();
        publishServices();
        started = true;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.moduleStarted(name));
    }
    
    @Override
    public synchronized void stop()
    {
        if (started && !stopped)
        {
            unpublishServices();
            ((ILifecycle)container).stop();
            unregisterDependencies();
            stopped = true;
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.moduleStopped(name));
        }
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
    @Override
    protected void finalize()
    {
        stop();
    }
    
    /**
     * Registers module external dependencies. External dependencies are services from application service registry,
     * that can be used by module component factories, components an so on. Dependencies can also be registered
     * in component factories.
     */
    protected void registerDependencies()
    {
        // By default, does nothing
    }

    /**
     * Registers module factories in module's component container.
     */
    protected abstract void registerFactories();
    
    /**
     * Unregisters module external dependencies.
     */
    protected void unregisterDependencies()
    {
        // By default, does nothing
    }
    
    /**
     * Publishes module exported services. Module's component factories can also publish their 
     * components as exported services.
     */
    protected void publishServices()
    {
        // By default, does nothing
    }
    
    /**
     * Removes publication of module exported services.
     */
    protected void unpublishServices()
    {
        // By default, does nothing
    }
    
    private interface IMessages
    {
        @DefaultMessage("Component container of module ''{0}'' must support interface ''{1}''.")
        ILocalizedMessage mustSupportInterface(Object moduleName, Object interfaceName);
        @DefaultMessage("Module ''{0}'' is already started.")
        ILocalizedMessage moduleAlreadyStarted(Object name);
        @DefaultMessage("Module ''{0}'' is already stopped.")
        ILocalizedMessage moduleAlreadyStopped(Object name);
        @DefaultMessage("Module ''{0}'' is started.")
        ILocalizedMessage moduleStarted(Object name);
        @DefaultMessage("Module ''{0}'' is stopped.")
        ILocalizedMessage moduleStopped(Object name);
    }
}
