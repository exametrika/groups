/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.resource;

import java.io.InputStream;

/**
 * The {@link IResourceLoader} is used to load external resources.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IResourceLoader
{
    /**
     * Loads resource from specified location.
     *
     * @param resourceLocation resource location
     * @return loaded resource or null if resource is not found
     */
    InputStream loadResource(String resourceLocation);
}
