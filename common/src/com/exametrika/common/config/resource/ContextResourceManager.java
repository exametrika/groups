/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.resource;

import java.io.File;
import java.io.InputStream;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.InvalidArgumentException;


/**
 * The {@link ContextResourceManager} is used to load resources by location relative to specified context path. 
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ContextResourceManager implements IResourceManager, IResourceLoader
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String contextPath;
    private final IResourceManager resourceManager;
    
    /**
     * Create a new object with resource context.
     *
     * @param contextPath resource context path of resource manager. Resource context path must start with 
     * 'schema:' and must not end with '/'
     * @param resourceManager resource manager that actually loads resource
     */
    public ContextResourceManager(String contextPath, IResourceManager resourceManager)
    {
        Assert.notNull(contextPath);
        Assert.notNull(resourceManager);
        Assert.isTrue(resourceManager instanceof IResourceLoader);
        
        contextPath = contextPath.replace(File.separatorChar, '/');
        
        if (contextPath != null && 
            (contextPath.indexOf(':') == -1 || contextPath.lastIndexOf('/') == (contextPath.length() - 1)))
            throw new InvalidArgumentException(messages.illegalContextPath(contextPath));
        
        this.contextPath = contextPath;
        this.resourceManager = resourceManager;
    }
    
    @Override
    public boolean hasSchema(String resourceLocation)
    {
        return resourceManager.hasSchema(resourceLocation);
    }
    
    @Override
    public InputStream getResource(String resourceLocation)
    {
        resourceLocation = resourceLocation.replace(File.separatorChar, '/');
        InputStream stream = loadResource(resourceLocation);
        if (stream != null)
            return stream;
        
        throw new ResourceNotFoundException(messages.resourceNotFound(resourceLocation, contextPath));
    }

    @Override
    public InputStream loadResource(String resourceLocation)
    {
        Assert.notNull(resourceLocation);
        
        if (resourceLocation.indexOf(":") == -1)
        {
            // Relative resource specified
            if (resourceLocation.startsWith("/"))
                throw new InvalidArgumentException(messages.illegalResourceLocation(resourceLocation));
            
            resourceLocation = contextPath + "/" + resourceLocation; 
        }
        
        return ((IResourceLoader)resourceManager).loadResource(resourceLocation);
    }
    
    private interface IMessages
    {
        @DefaultMessage("Illegal resource context path ''{0}''. Resource context path must start with '<schema>:' and must not end with '/'.")
        ILocalizedMessage illegalContextPath(String contextPath);
        @DefaultMessage("Resource ''{0}'' is not found in context ''{1}''.")
        ILocalizedMessage resourceNotFound(String location, String contextPath);
        @DefaultMessage("Illegal resource location ''{0}''. Absolute resource location must start with '<schema>:', relative resource location must not start with '<schema>:' and '/'.")
        ILocalizedMessage illegalResourceLocation(String location); 
    }
}
