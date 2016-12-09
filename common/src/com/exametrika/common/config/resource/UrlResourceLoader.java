/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.exametrika.common.component.ComponentException;
import com.exametrika.common.utils.Assert;


/**
 * The {@link UrlResourceLoader} is implementation of {@link IResourceLoader} that loads resources from {@link URL}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class UrlResourceLoader implements IResourceLoader
{
    @Override
    public InputStream loadResource(String resourceLocation)
    {
        Assert.notNull(resourceLocation);
        
        URL url;
        try
        {
            url = new URL(resourceLocation);
        }
        catch (MalformedURLException e)
        {
            throw new ComponentException(e);
        }
        
        try
        {
            return url.openStream();
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
