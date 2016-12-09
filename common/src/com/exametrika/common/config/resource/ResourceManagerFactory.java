/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.resource;

import java.util.Map;

import com.exametrika.common.component.container.FactoryNotFoundException;
import com.exametrika.common.component.factory.singleton.AbstractSingletonComponentFactory;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;


/**
 * The {@link ResourceManagerFactory} is a factory for {@link ResourceManager}.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ResourceManagerFactory extends AbstractSingletonComponentFactory<ResourceManager>
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String parentManagerName;
    private final Map<String, IResourceLoader> resourceLoaders;
    private IResourceManager parentManager;
    private String defaultSchema;

    /**
     * Create a new object.
     *
     * @param resourceLoaders mapping of schema prefixes to resource loaders
     * @param parentManager parent resource manager, that is used when resource is not found in resource manager's
     * resource loaders. Can be null
     * @param defaultSchema default schema
     */
    public ResourceManagerFactory(Map<String, IResourceLoader> resourceLoaders, String defaultSchema, IResourceManager parentManager)
    {
        super(true);
        
        Assert.notNull(resourceLoaders);
        Assert.notNull(defaultSchema);
        
        this.resourceLoaders = resourceLoaders;
        this.defaultSchema = defaultSchema;
        this.parentManagerName = null;
        this.parentManager = parentManager;
    }
    
    /**
     * Create a new object. For use in container only. 
     *
     * @param resourceLoaders mapping of schema prefixes to resource loaders
     * @param parentManagerName name of parent resource manager, that is used when resource is not found in resource manager's
     * resource loaders. Can be null
     * @param defaultSchema default schema
     */
    public ResourceManagerFactory(Map<String, IResourceLoader> resourceLoaders, String defaultSchema, String parentManagerName)
    {
        super(true);
        
        Assert.notNull(resourceLoaders);
        Assert.notNull(defaultSchema);
        
        this.resourceLoaders = resourceLoaders;
        this.parentManagerName = parentManagerName;
        this.defaultSchema = defaultSchema;
    }
    
    @Override
    protected ResourceManager createInstance()
    {
        return new ResourceManager(resourceLoaders, defaultSchema, parentManager);
    }

    @Override
    protected void setFactoryDependencies()
    {
        if (parentManager == null && parentManagerName != null)
        {
            if (getContainer() != null)
                parentManager = getContainer().getComponent(parentManagerName);
            else
                throw new FactoryNotFoundException(messages.parentManagerNotFound(parentManagerName));
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Parent resource manager ''{0}'' is not found.")
        ILocalizedMessage parentManagerNotFound(String managerName);
    }
}
