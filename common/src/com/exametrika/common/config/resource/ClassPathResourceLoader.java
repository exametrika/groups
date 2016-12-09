/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.resource;

import java.io.InputStream;

import com.exametrika.common.utils.Assert;


/**
 * The {@link ClassPathResourceLoader} is an implementation of {@link IResourceLoader} that loads resources from class path,
 * i.e. from specified class loader (if any) or from thread context class loader. Resource location for classpath 
 * resource must start with 'classpath:'
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ClassPathResourceLoader implements IResourceLoader
{
    /**
     * Schema for classpath resource.
     */
    public static final String SCHEMA = "classpath";
    private final ClassLoader loader;

    /**
     * Create a new classpath resource loader that loads resources from thread context class loader.
     */
    public ClassPathResourceLoader()
    {
        this.loader = null;
    }
    
    /**
     * Create a new classpath resource loader that loads resources from specified class loader.
     *
     * @param loader class loader to load resources from
     */
    public ClassPathResourceLoader(ClassLoader loader)
    {
        Assert.notNull(loader);
        
        this.loader = loader;
    }
    
    @Override
    public InputStream loadResource(String resourceLocation)
    {
        Assert.notNull(resourceLocation);
        
        // Remove prefix
        resourceLocation = resourceLocation.substring(resourceLocation.indexOf(':') + 1);
        
        ClassLoader loader = this.loader;
        if (loader == null)
            loader = getClass().getClassLoader();
        if (loader != null)
            return loader.getResourceAsStream(resourceLocation);
        
        return null;
    }
}
