/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.resource;

import java.io.InputStream;
import java.util.List;

import com.exametrika.common.utils.Assert;


/**
 * The {@link CompositeResourceLoader} is and implementation of {@link IResourceLoader} that delegates resource loading
 * to specified resource loaders.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class CompositeResourceLoader implements IResourceLoader
{
    private final List<? extends IResourceLoader> resourceLoaders;

    /**
     * Creates a new object.
     *
     * @param resourceLoaders resource loaders
     */
    public CompositeResourceLoader(List<? extends IResourceLoader> resourceLoaders)
    {
        Assert.notNull(resourceLoaders);
        
        this.resourceLoaders = resourceLoaders;
    }
    
    @Override
    public InputStream loadResource(String resourceLocation)
    {
        for (IResourceLoader loader : resourceLoaders)
        {
            InputStream stream = loader.loadResource(resourceLocation);
            if (stream != null)
                return stream;
        }
        
        return null;
    }
}
