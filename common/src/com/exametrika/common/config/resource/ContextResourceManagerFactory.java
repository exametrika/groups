/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.resource;

import com.exametrika.common.component.container.FactoryNotFoundException;
import com.exametrika.common.component.factory.singleton.AbstractSingletonComponentFactory;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ContextResourceManagerFactory} is a factory for {@link ContextResourceManager}.
 *
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ContextResourceManagerFactory extends AbstractSingletonComponentFactory<ContextResourceManager>
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String resourceManagerName;
    private final String contextPath;
    private IResourceManager resourceManager;

    /**
     * Create a new object with resource context.
     *
     * @param contextPath resource context path of resource manager. Resource context path must start with 
     * 'schema:' and must not end with '/'
     * @param resourceManager resource manager that actually loads resource
     */
    public ContextResourceManagerFactory(String contextPath, IResourceManager resourceManager)
    {
        super(true);
        
        Assert.notNull(contextPath);
        Assert.notNull(resourceManager);

        this.contextPath = contextPath;
        this.resourceManager = resourceManager;
        this.resourceManagerName = null;
    }
    
    /**
     * Create a new object with resource context. For use in container only.
     *
     * @param contextPath resource context path of resource manager. Resource context path must start with 
     * 'schema:' and must not end with '/'
     * @param resourceManagerName name of resource manager component that actually loads resource
     */
    public ContextResourceManagerFactory(String contextPath, String resourceManagerName)
    {
        super(true);
        
        Assert.notNull(contextPath);
        Assert.notNull(resourceManagerName);

        this.contextPath = contextPath;
        this.resourceManagerName = resourceManagerName;
    }
    
    @Override
    protected ContextResourceManager createInstance()
    {
        return new ContextResourceManager(contextPath, resourceManager);
    }

    @Override
    protected void setFactoryDependencies()
    {
        if (resourceManager == null)
        {
            if (getContainer() != null)
                resourceManager = getContainer().getComponent(resourceManagerName);
            else
                throw new FactoryNotFoundException(messages.resourceManagerNotFound(resourceManagerName));
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Resource manager ''{0}'' is not found.")
        ILocalizedMessage resourceManagerNotFound(String managerName);
    }
}
