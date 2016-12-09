/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.factory.prototype;

import com.exametrika.common.component.factory.AbstractComponentFactory;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.ILifecycle;

/**
 * The {@link AbstractPrototypeComponentFactory} is an abstract component factory for prototype components.
 * 
 * @param <T> type name of component
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractPrototypeComponentFactory<T> extends AbstractComponentFactory<T>
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AbstractPrototypeComponentFactory.class);
   
    @Override
    public final T createComponent()
    {
        // Initialize factory, if not initialized
        init();
        
        // Create component instance
        T component = createInstance();
        
        // Pre-process component instance
        component = processBeforeStart(component);
        
        // Start instance
        if (component instanceof ILifecycle)
            ((ILifecycle)component).start();
        
        // Post-process component instance
        component = processAfterStart(component);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.componentStarted(component));
        return component;
    }
    
    /**
     * Creates a component instance. 
     *
     * @return component instance
     */
    protected abstract T createInstance();
    
    private interface IMessages
    {
        @DefaultMessage("Component ''{0}'' is started.")
        ILocalizedMessage componentStarted(Object component);
    }
}
