/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.common;

import com.exametrika.common.config.IConfigurationFactory;
import com.exametrika.common.config.IContextFactory;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.log.config.LoggingConfiguration;





/**
 * The {@link CommonLoadContext} is a helper class that is used to load {@link LoggingConfiguration}.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class CommonLoadContext implements ICommonLoadContext, IContextFactory, IConfigurationFactory
{
    private RuntimeMode runtimeMode = RuntimeMode.DEVELOPMENT;
    
    public void setRuntimeMode(RuntimeMode runtimeMode)
    {
        this.runtimeMode = runtimeMode;
    }
    
    @Override
    public RuntimeMode getRuntimeMode()
    {
        return runtimeMode;
    }
    
    @Override
    public CommonConfiguration createConfiguration(ILoadContext context)
    {
        return new CommonConfiguration(runtimeMode);
    }
    
    @Override
    public IConfigurationFactory createContext()
    {
        return new CommonLoadContext();
    }
}
