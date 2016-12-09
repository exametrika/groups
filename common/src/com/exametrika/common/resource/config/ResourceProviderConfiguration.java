/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.resource.IResourceProvider;





/**
 * The {@link ResourceProviderConfiguration} is a configuration of resource provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public abstract class ResourceProviderConfiguration extends Configuration
{
    public abstract IResourceProvider createProvider();
}
