/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.resource;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;


/**
 * The {@link ResourceManager} is matches particular resource loader by schema in resource location and delegates
 * resource resolution to that loader. 
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ResourceManager implements IResourceManager, IResourceLoader
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Map<String, IResourceLoader> resourceLoaders;
    private final String defaultSchema;
    private final IResourceManager parentManager;
    
    /**
     * Create a new object.
     *
     * @param resourceLoaders mapping of schema prefixes to resource loaders
     * @param defaultSchema default schema
     * 
     */
    public ResourceManager(Map<String, IResourceLoader> resourceLoaders, String defaultSchema)
    {
        this(resourceLoaders, defaultSchema, null);
    }
    
    /**
     * Create a new object with resource context.
     *
     * @param resourceLoaders mapping of schema prefixes to resource loaders
     * @param parentManager parent resource manager, that is used when resource is not found in resource manager's
     * resource loaders. Can be null
     * @param defaultSchema default schema
     */
    public ResourceManager(Map<String, IResourceLoader> resourceLoaders, String defaultSchema, IResourceManager parentManager)
    {
        Assert.notNull(resourceLoaders);
        Assert.notNull(defaultSchema);
        Assert.isTrue(resourceLoaders.containsKey(defaultSchema) || (parentManager != null && parentManager.hasSchema(defaultSchema + ":")));
        Assert.isTrue(parentManager == null || parentManager instanceof IResourceLoader);
        
        this.resourceLoaders = resourceLoaders;
        this.defaultSchema = defaultSchema;
        this.parentManager = parentManager;
    }
    
    @Override
    public boolean hasSchema(String resourceLocation)
    {
        int pos = resourceLocation.indexOf(":");
        if (pos == -1)
            return false;
        
        String schemaPrefix = resourceLocation.substring(0, pos);
        if (resourceLoaders.containsKey(schemaPrefix))
            return true;
        
        if (parentManager != null)
            return parentManager.hasSchema(resourceLocation);
        else
            return false;
    }
    
    @Override
    public InputStream getResource(String resourceLocation)
    {
        resourceLocation = resourceLocation.replace(File.separatorChar, '/');
        InputStream stream = loadResource(resourceLocation);
        if (stream != null)
            return stream;
        
        throw new ResourceNotFoundException(messages.resourceNotFound(resourceLocation));
    }

    @Override
    public InputStream loadResource(String resourceLocation)
    {
        Assert.notNull(resourceLocation);
        
        if (!hasSchema(resourceLocation))
            resourceLocation = defaultSchema + ":" + resourceLocation;
        
        int pos = resourceLocation.indexOf(":");
        
        String schemaPrefix = resourceLocation.substring(0, pos);
        IResourceLoader loader = resourceLoaders.get(schemaPrefix);
        if (loader == null)
        {
            if (parentManager != null)
                return ((IResourceLoader)parentManager).loadResource(resourceLocation);
            else
                return null;
        }
        
        return loader.loadResource(resourceLocation);
    }
    
    private interface IMessages
    {
        @DefaultMessage("Resource ''{0}'' is not found.")
        ILocalizedMessage resourceNotFound(String location);
    }
}
