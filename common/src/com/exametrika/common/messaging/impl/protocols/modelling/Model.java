/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.modelling;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;


/**
 * The {@link Model} is an abstract network model.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class Model implements Runnable, ILifecycle
{
    private ILifecycle[] components;
    
    @Override
    public final synchronized void start()
    {
        Assert.checkState(components == null);
            
        components = createComponents();
        
        Assert.checkState(components != null);
        
        for (ILifecycle component : components)
            component.start();
    }
    
    @Override
    public final synchronized void stop()
    {
        if (components == null)
            return;
        
        for (ILifecycle component : components)
            component.stop();
        
        components = null;
    }

    public final void visitModel(IModelVisitor visitor)
    {
        Assert.notNull(visitor);
        
        for (ILifecycle component : components)
            visitor.visitComponent(component);
    }
    
    /**
     * Performs modelling.
     */
    @Override
    public abstract void run();
    
    /**
     * Creates network model components.
     *
     * @return network model components
     */
    protected abstract ILifecycle[] createComponents();
}
