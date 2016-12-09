/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.services.impl;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.exametrika.common.services.IServiceProvider;
import com.exametrika.common.services.IServiceRegistrar;
import com.exametrika.common.services.Services;



/**
 * The {@link ServiceContainer} is a root service container.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class ServiceContainer extends AbstractServiceContainer
{
    public ServiceContainer(Map<String, Object> parameters, List<File> libraries)
    {
        super("root", parameters);
        
        for (File library : libraries)
        {
            if (library.exists() && library.isFile())
                System.load(library.toString());
        }
    }
    
    @Override
    protected void loadProviders(IServiceRegistrar registrar)
    {
        for (IServiceProvider provider : Services.loadProviders(IServiceProvider.class))
            provider.register(registrar);
    }
}
