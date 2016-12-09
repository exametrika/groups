/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.factory;

import java.util.List;

import com.exametrika.common.component.container.IComponentContainer;
import com.exametrika.common.component.container.IComponentContainerAware;
import com.exametrika.common.component.container.IComponentFactory;
import com.exametrika.common.component.container.IComponentProcessor;
import com.exametrika.common.component.container.IProcessableComponentFactory;
import com.exametrika.common.component.container.ITypeProcessor;
import com.exametrika.common.utils.ILifecycle;


/**
 * The {@link AbstractComponentFactory} is an abstract implementation of {@link IComponentFactory}.
 * 
 * @param <T> type name of component
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class AbstractComponentFactory<T> implements IProcessableComponentFactory<T>, ILifecycle, 
    IComponentContainerAware
{
    private volatile IComponentContainer container;
    private volatile List<? extends ITypeProcessor> typeProcessors;
    private volatile List<? extends IComponentProcessor> componentProcessors;
    private volatile boolean inited;

    /**
     * Returns component container. 
     *
     * @return component container
     */
    public final IComponentContainer getContainer()
    {
        return container;
    }
    
    @Override
    public abstract T createComponent();
    
    @Override
    public void start()
    {
        init();
    }
    
    @Override
    public void stop()
    {
        
    }
    
    @Override
    public final void setContainer(IComponentContainer container)
    {
        this.container = container;
    }
    
    @Override
    public final void setTypeProcessors(List<? extends ITypeProcessor> typeProcessors)
    {
        this.typeProcessors = typeProcessors;
    }
    
    @Override
    public final void setComponentProcessors(List<? extends IComponentProcessor> componentProcessors)
    {
        this.componentProcessors = componentProcessors;
    }
    
    protected final void init()
    {
        if (inited)
            return;
        
        synchronized (getSync())
        {
            if (inited)
                return;
            
            initializeFactory();
            inited = true;
        }
    }
    
    protected final <K> K processType(Object componentType)
    {
        List<? extends ITypeProcessor> typeProcessors = this.typeProcessors;
        if (typeProcessors != null)
        {
            for (ITypeProcessor processor : typeProcessors)
                componentType = processor.processType(componentType);
        }
        
        return (K)componentType;
    }
    
    protected final <K> K processBeforeStart(Object component)
    {
        List<? extends IComponentProcessor> componentProcessors = this.componentProcessors;
        if (componentProcessors != null)
        {
            for (IComponentProcessor processor : componentProcessors)
                component = processor.processBeforeStart(component);
        }
        
        return (K)component;
    }
    
    protected final <K> K processAfterStart(Object component)
    {
        List<? extends IComponentProcessor> componentProcessors = this.componentProcessors;
        if (componentProcessors != null)
        {
            for (IComponentProcessor processor : componentProcessors)
                component = processor.processAfterStart(component);
        }
        
        return (K)component;
    }

    protected final Object getSync()
    {
        // Use container (if any) as synchronizer to avoid deadlocks
        Object sync = container;
        if (sync == null)
            // If container is not set use factory as synchronizer
            sync = this;
        
        return sync;
    }
    
    /**
     * Initializes a factory.
     */
    protected void initializeFactory()
    {
        processType();
        setFactoryDependencies();
    }
    
    /**
     * Sets factory dependencies, i.e. another factories and singletons. Factory dependencies can be used to speed up
     * component creation. Circular factory dependencies are fully supported.
     */
    protected void setFactoryDependencies()
    {
        // By default does nothing.
    }
    
    /**
     * Processes a component type.
     */
    protected void processType()
    {
        // By default does nothing
    }
}
