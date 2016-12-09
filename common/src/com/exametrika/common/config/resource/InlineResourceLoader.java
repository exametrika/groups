/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.resource;

import java.io.InputStream;

import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.utils.Assert;


/**
 * The {@link InlineResourceLoader} is implementation of {@link IResourceLoader} that loads resources inline from location.
 * Resource location for inline resource must start with 'inline:'
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class InlineResourceLoader implements IResourceLoader
{
    /**
     * Schema for inline resource.
     */
    public static final String SCHEMA = "inline";
    
    @Override
    public InputStream loadResource(String resourceLocation)
    {
        Assert.notNull(resourceLocation);
        
        // Remove prefix
        resourceLocation = resourceLocation.substring(resourceLocation.indexOf(':') + 1);
        
        try
        {
            return new ByteInputStream(resourceLocation.getBytes("UTF-8"));
        }
        catch(Exception e)
        {
            return null;
        }
    }
}
