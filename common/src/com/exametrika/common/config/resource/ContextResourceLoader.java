/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.resource;

import java.io.InputStream;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;


/**
 * The {@link ContextResourceLoader} is and implementation of {@link IResourceLoader} that loads resource from
 * specified context path using given resource loader.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ContextResourceLoader implements IResourceLoader
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String contextPath;
    private final IResourceLoader resourceLoader;

    /**
     * Creates a new object.
     *
     * @param contextPath resource context path of resource manager. Resource context path must start with 
     * 'schema:' and must not end with '/'
     * @param resourceLoader resource loader that actually loads resource
     */
    public ContextResourceLoader(String contextPath, IResourceLoader resourceLoader)
    {
        Assert.notNull(contextPath);
        Assert.notNull(resourceLoader);
        if (contextPath != null && 
                (contextPath.indexOf(':') == -1 || contextPath.lastIndexOf('/') == (contextPath.length() - 1)))
                throw new InvalidArgumentException(messages.illegalContextPath(contextPath));
        
        this.contextPath = contextPath;
        this.resourceLoader = resourceLoader;
    }
    
    @Override
    public InputStream loadResource(String resourceLocation)
    {
        Assert.notNull(resourceLocation);
        
        // Remove prefix
        resourceLocation = resourceLocation.substring(resourceLocation.indexOf(':') + 1);

        if (resourceLocation.startsWith("/"))
            throw new InvalidArgumentException(messages.illegalResourceLocation(resourceLocation));
            
        resourceLocation = contextPath + "/" + resourceLocation; 
        
        return resourceLoader.loadResource(resourceLocation);
    }
    
    private interface IMessages
    {
        @DefaultMessage("Illegal resource context path ''{0}''. Resource context path must start with '<schema>:' and must not end with '/'.")
        ILocalizedMessage illegalContextPath(String contextPath);
        @DefaultMessage("Illegal resource location ''{0}''. Resource location without schema must not start with '/'.")
        ILocalizedMessage illegalResourceLocation(String location); 
    }
}
