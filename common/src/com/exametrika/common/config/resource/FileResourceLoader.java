/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.resource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.exametrika.common.utils.Assert;


/**
 * The {@link FileResourceLoader} is implementation of {@link IResourceLoader} that loads resources from file system.
 * Resource location for file resource must start with 'file:'
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class FileResourceLoader implements IResourceLoader
{
    /**
     * Schema for file resource.
     */
    public static final String SCHEMA = "file";
    
    @Override
    public InputStream loadResource(String resourceLocation)
    {
        Assert.notNull(resourceLocation);
        
        // Remove prefix
        resourceLocation = resourceLocation.substring(resourceLocation.indexOf(':') + 1);
        
        File file = new File(resourceLocation);
        if (file.exists())
        {
            try
            {
                return new BufferedInputStream(new FileInputStream(resourceLocation));
            }
            catch (FileNotFoundException e)
            {
                return null;
            }
        }

        return null;
    }
}
