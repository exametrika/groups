/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * The {@link ComponentContainer} is an implementation of {@link IComponentContainer}.
 * 
 * @see IComponentContainer
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ComponentContainer implements IComponentContainer, ILifecycle, IFactoryResolver
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ComponentContainer.class);
    private final IFactoryResolver resolver;
    private final List<? extends ITypeProcessor> typeProcessors;
    private final List<? extends IComponentProcessor> componentProcessors;
    private final LinkedHashMap<String, List<ComponentInfo>> registry = new LinkedHashMap<String, List<ComponentInfo>>();
    private boolean started;
    private boolean stopped;

    /**
     * Creates a new object.
     */
    public ComponentContainer()
    {
        this(null ,null, null);
    }
    
    /**
     * Creates a new object.
     *
     * @param resolver component factory resolver. Can be <c>null<c>
     * @param typeProcessors list of type processors. Can be <c>null<c>
     * @param componentProcessors list of component processors. Can be <c>null<c>
     */
    public ComponentContainer(IFactoryResolver resolver, List<? extends ITypeProcessor> typeProcessors,
        List<? extends IComponentProcessor> componentProcessors)
    {
        this.resolver = resolver;
        
        if (typeProcessors != null)
            this.typeProcessors = typeProcessors;
        else
            this.typeProcessors = Collections.emptyList();
        
        if (componentProcessors != null)
            this.componentProcessors = componentProcessors;
        else
            this.componentProcessors = Collections.emptyList();
    }
    
    @Override
    public <T> void register(String componentName, IComponentFactory<T> factory)
    {
        register(componentName, null, factory);
    }
    
    @Override
    public synchronized <T> void register(String componentName, Map<String, ?> qualifiers, IComponentFactory<T> factory)
    {
        Assert.notNull(componentName);
        Assert.notNull(factory);
        if (stopped)
            throw new InvalidStateException(messages.containerAlreadyStopped());
        
        if (qualifiers == null)
            qualifiers = Collections.emptyMap();
        
        List<ComponentInfo> componentInfos = registry.get(componentName);
        if (componentInfos == null)
        {
            componentInfos = new ArrayList<ComponentInfo>();
            registry.put(componentName, componentInfos);
        }
            
        for (ComponentInfo info : componentInfos)
        {
            if (info.getQualifiers().equals(qualifiers))
                throw new InvalidArgumentException(messages.factoryAlreadyRegistered(componentName, qualifiers));
        }
        
        componentInfos.add(new ComponentInfo(factory, qualifiers));

        if (factory instanceof IComponentContainerAware)
            ((IComponentContainerAware)factory).setContainer(this);

        if (factory instanceof IProcessableComponentFactory)
        {
            IProcessableComponentFactory<T> processableFactory = (IProcessableComponentFactory<T>)factory;
            if (typeProcessors != null)
                processableFactory.setTypeProcessors(typeProcessors);
            if (componentProcessors != null)
                processableFactory.setComponentProcessors(componentProcessors);
        }
        
        if (started && factory instanceof ILifecycle)
            ((ILifecycle)factory).start();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.factoryRegistered(componentName, qualifiers));
    }

    @Override
    public synchronized void unregister(String componentName)
    {
        unregister(componentName, null);
    }
    
    @Override
    public synchronized void unregister(String componentName, Map<String, ?> qualifiers)
    {
        Assert.notNull(componentName);
        if (stopped)
            throw new InvalidStateException(messages.containerAlreadyStopped());
        
        if (qualifiers == null)
            qualifiers = Collections.emptyMap();
        
        List<ComponentInfo> componentInfos = registry.get(componentName);
        if (componentInfos == null)
            return;
            
        IComponentFactory<?> factory = null;
        for (Iterator<ComponentInfo> it  = componentInfos.iterator(); it.hasNext(); )
        {
            ComponentInfo info = it.next();
            if (info.getQualifiers().equals(qualifiers))
            {
                factory = info.getFactory();
                it.remove();
                
                if (componentInfos.isEmpty())
                    registry.remove(componentName);
                break;
            }
        }
        
        if (factory == null)
            return;
        
        if (started && factory instanceof ILifecycle)
            ((ILifecycle)factory).stop();
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.factoryUnregistered(componentName, qualifiers));
    }
    
    @Override
    public <T> T getComponent(String componentName)
    {
        return getComponent(componentName, null, true);
    }

    @Override
    public <T> T getComponent(String componentName, Map<String, ?> qualifiers, boolean required)
    {
        IComponentFactory<T> factory = getFactory(componentName, qualifiers, required);
        if (factory != null)
            return factory.createComponent();
        
        // If optional factory is not found
        return null;
    }

    @Override
    public <T> IComponentFactory<T> getFactory(String componentName)
    {
        return getFactory(componentName, null, true);
    }
    
    @Override
    public <T> IComponentFactory<T> getFactory(String componentName, Map<String, ?> qualifiers, boolean required)
    {
        Assert.notNull(componentName);

        if (qualifiers == null)
            qualifiers = Collections.emptyMap();

        IComponentFactory<T> factory = findFactory(componentName, qualifiers);
        if (factory == null && resolver != null)
            factory = resolver.resolveFactory(componentName, qualifiers);
        
        if (required && factory == null)
            throw new FactoryNotFoundException(messages.factoryNotFound(componentName, qualifiers));
        
        return factory;
    }
    
    @Override
    public synchronized void start()
    {
        if (stopped)
            throw new InvalidStateException(messages.containerAlreadyStopped());
        if (started)
            throw new InvalidStateException(messages.containerAlreadyStarted());
        
        started = true;
        
        // Make a factory snapshot to allow factory registration during component container start
        LinkedHashMap<String, List<ComponentInfo>> registry = (LinkedHashMap<String, List<ComponentInfo>>)this.registry.clone();
        for (Map.Entry<String, List<ComponentInfo>> entry : registry.entrySet())
        {
            for (ComponentInfo info : entry.getValue())
            {
                IComponentFactory<?> factory = info.getFactory();
                if (factory instanceof ILifecycle)
                    ((ILifecycle)factory).start();
            }
        }
    }

    @Override
    public synchronized void stop()
    {
        if (started && !stopped)
        {
            // Make a factory snapshot to allow factory unregistration during component container stop (some factories can
            // be stopped several times, that's why factory must be prepared for that)
            LinkedHashMap<String, List<ComponentInfo>> registry = (LinkedHashMap<String, List<ComponentInfo>>)this.registry.clone();
            for (Map.Entry<String, List<ComponentInfo>> entry : registry.entrySet())
            {
                for (ComponentInfo info : entry.getValue())
                {
                    IComponentFactory<?> factory = info.getFactory();
                    if (factory instanceof ILifecycle)
                        ((ILifecycle)factory).stop();
                }
            }

            // Clear factory registry because component container can not be restarted
            this.registry.clear();
            
            stopped = true;
        }
    }

    @Override
    public <T> IComponentFactory<T> resolveFactory(String componentName, Map<String, ?> qualifiers)
    {
        return getFactory(componentName, qualifiers, false);
    }
    
    @Override
    protected void finalize()
    {
        stop();
    }
    
    private synchronized <T> IComponentFactory<T> findFactory(String componentName, Map<String, ?> qualifiers)
    {
        if (stopped)
            throw new InvalidStateException(messages.containerAlreadyStopped());
        if (!started)
            throw new InvalidStateException(messages.containerNotStartedStarted());

        List<ComponentInfo> componentInfos = registry.get(componentName);
        if (componentInfos == null)
            return null;
        
        for (ComponentInfo info : componentInfos)
        {
            if (matchQualifiers(qualifiers, info.getQualifiers()))
                return (IComponentFactory<T>)info.getFactory();
        }
        
        return null;
    }
    
    private boolean matchQualifiers(Map<String, ?> requiredQualifiers, Map<String, ?> componentQualifiers)
    {
        for (Map.Entry<String, ?> entry : requiredQualifiers.entrySet())
        {
            if (entry.getValue() instanceof IQualifier)
            {
                IQualifier qualifier = (IQualifier)entry.getValue();
                if (!qualifier.match(entry.getKey(), componentQualifiers))
                    return false;
            }
            else
            {
                if (!componentQualifiers.containsKey(entry.getKey()))
                    return false;
                
                Object value = componentQualifiers.get(entry.getKey());
                if (entry.getValue() != value && (entry.getValue() == null || !entry.getValue().equals(value)))
                    return false;
            }
        }
        
        return true;
    }
    
    private static final class ComponentInfo
    {
        private final IComponentFactory<?> factory;
        private final Map<String, ?> qualifiers;

        public ComponentInfo(IComponentFactory<?> factory, Map<String, ?> qualifiers)
        {
            this.factory = factory;
            this.qualifiers = qualifiers;
        }
        
        public IComponentFactory<?> getFactory()
        {
            return factory;
        }
        
        public Map<String, ?> getQualifiers()
        {
            return qualifiers;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Component container is already started.")
        ILocalizedMessage containerAlreadyStarted();
        @DefaultMessage("Component container is already stopped.")
        ILocalizedMessage containerAlreadyStopped();
        @DefaultMessage("Component container is not started.")
        ILocalizedMessage containerNotStartedStarted();
        @DefaultMessage("Component factory ''{0} {1}'' is already registered.")
        ILocalizedMessage factoryAlreadyRegistered(String name, Object  qualifiers);
        @DefaultMessage("Component factory ''{0} {1}'' is not found.")
        ILocalizedMessage factoryNotFound(String name, Object qualifiers);
        @DefaultMessage("Component factory ''{0} {1}'' is registered.")
        ILocalizedMessage factoryRegistered(String name, Object qualifiers);
        @DefaultMessage("Component factory ''{0} {1}'' is unregistered.")
        ILocalizedMessage factoryUnregistered(String name, Object qualifiers);
    }
}
