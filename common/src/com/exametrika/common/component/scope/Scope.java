/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.scope;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;



/**
 * The {@link Scope} is an implementation of {@link IScope}.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class Scope implements IScope, ILifecycle
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(Scope.class);
    private final Map<String, Object> components = new LinkedHashMap<String, Object>();
    
    @Override
    public <T> T get(String componentName)
    {
        Assert.notNull(componentName);
        
        return (T)components.get(componentName);
    }

    @Override
    public void add(String componentName, Object component)
    {
        Assert.notNull(componentName);
        Assert.notNull(component);
        
        if (components.containsKey(componentName))
            throw new ComponentAlreadyInScopeException(messages.componentAlreadyInScope(componentName, this));
        
        components.put(componentName, component);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.componentAdded(component, this));
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
        for (Map.Entry<String, Object> entry : components.entrySet())
        {
            Object component = entry.getValue();
            if (component instanceof ILifecycle)
                ((ILifecycle)component).stop();
            
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.componentStopped(component, this));
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Component ''{0}'' already exists in scope ''{1}''.")
        ILocalizedMessage componentAlreadyInScope(Object component, Object scope);
        @DefaultMessage("Component ''{0}'' is added to scope ''{1}''.")
        ILocalizedMessage componentAdded(Object component, Object scope);
        @DefaultMessage("Component ''{0}'' is stopped and removed from scope ''{1}''.")
        ILocalizedMessage componentStopped(Object component, Object scope);
    }
}
