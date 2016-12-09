/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.resource;

import java.io.InputStream;

/**
 * The {@link IResourceManager} is used to get external resources.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IResourceManager
{
    /**
     * Does specified resource location have resource schema prefix.
     *
     * @param resourceLocation resource location
     * @return true if resource location has resource schema prefix
     */
    boolean hasSchema(String resourceLocation);
    
    /**
     * Returns resource by location.
     *
     * @param resourceLocation resource location
     * @return found resource
     * @exception ResourceNotFoundException if resource is not found
     */
    InputStream getResource(String resourceLocation);
}
