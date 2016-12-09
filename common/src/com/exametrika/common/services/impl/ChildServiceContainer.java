/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.services.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.services.IService;
import com.exametrika.common.services.IServiceProvider;
import com.exametrika.common.services.IServiceRegistrar;
import com.exametrika.common.services.IServiceRegistry;
import com.exametrika.common.services.Services;
import com.exametrika.common.utils.Assert;



/**
 * The {@link ChildServiceContainer} is a child service container.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class ChildServiceContainer extends AbstractServiceContainer
{
    private final Set<String> runModes;
    private final Set<String> qualifiers;
    private final ClassLoader classLoader;
    private final IServiceRegistry parent;

    public ChildServiceContainer(String name, Map<String, Object> parameters, Set<String> runModes, Set<String> qualifiers,
        ClassLoader classLoader, IServiceRegistry parent)
    {
        super(name, parameters);
        
        Assert.notNull(parent);
        
        this.runModes = runModes;
        this.qualifiers = qualifiers;
        this.classLoader = classLoader;
        this.parent = parent;
    }
    
    @Override
    public <T> T findParameter(String name)
    {
        Assert.notNull(name);
        
        T parameter = super.findParameter(name);
        if (parameter != null)
            return parameter;
        else
            return parent.findParameter(name);
    }
    
    @Override
    public <T extends IService> T findService(String name)
    {
        Assert.notNull(name);
        
        T service = super.findService(name);
        if (service != null)
            return service;
        else
            return parent.findService(name);
    }
    
    @Override
    protected void loadProviders(IServiceRegistrar registrar)
    {
        List<IServiceProvider> providers = Services.loadProviders(IServiceProvider.class, runModes, qualifiers, 
            classLoader, true);
        
        for (IServiceProvider provider : providers)
            provider.register(registrar);
    }
}
