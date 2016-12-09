/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.factory.scope;

import java.util.List;

import com.exametrika.common.component.container.FactoryNotFoundException;
import com.exametrika.common.component.factory.singleton.AbstractSingletonComponentFactory;
import com.exametrika.common.component.proxy.IInterceptor;
import com.exametrika.common.component.proxy.IProxyFactory;
import com.exametrika.common.component.scope.IScopeManager;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;


/**
 * The {@link ScopedComponentFactory} is an component factory for proxy parts of components with scope. 
 * 
 * @param <T> type name of component
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ScopedComponentFactory<T> extends AbstractSingletonComponentFactory<T>
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String proxyName;
    private final String scopeName;
    private final List<Class<?>> interfaces;
    private final String componentName;
    private final String interceptorName;
    private IProxyFactory proxyFactory;
    private IScopeManager scopeManager;
    private IInterceptor interceptor;
    
    
    /**
     * Creates a new object.
     * 
     * @param proxyFactory proxy factory
     * @param scopeManager scope manager
     * @param interfaces list of interfaces proxy must implement
     * @param componentName name of scoped component in container and scope
     * @param interceptor additional interceptor. Can be <c>null<c>
     */
    public ScopedComponentFactory(IProxyFactory proxyFactory, IScopeManager scopeManager, List<Class<?>> interfaces,
        String componentName, IInterceptor interceptor)
    {
        super(true);
        
        Assert.notNull(proxyFactory);
        Assert.notNull(scopeManager);
        Assert.notNull(interfaces);
        Assert.notNull(componentName);
        
        this.proxyFactory = proxyFactory;
        this.scopeManager = scopeManager;
        this.proxyName = null;
        this.scopeName = null;
        this.interfaces = interfaces;
        this.componentName = componentName;
        this.interceptor = interceptor;
        this.interceptorName = null;
    }
    
    /**
     * Creates a new object. For use in container only.
     * 
     * @param proxyName name of proxy component
     * @param scopeName name of scope manager component
     * @param interfaces list of interfaces proxy must implement
     * @param componentName name of scoped component in container and scope
     * @param interceptorName name of additional interceptor component. Can be <c>null<c>
     */
    public ScopedComponentFactory(String proxyName, String scopeName, List<Class<?>> interfaces,
        String componentName, String interceptorName)
    {
        super(true);
        
        Assert.notNull(proxyName);
        Assert.notNull(scopeName);
        Assert.notNull(interfaces);
        Assert.notNull(componentName);
        
        this.proxyName = proxyName;
        this.scopeName = scopeName;
        this.interfaces = interfaces;
        this.componentName = componentName;
        this.interceptorName = interceptorName;
    }
 
    @Override
    protected T createInstance()
    {
        return proxyFactory.createProxy(getClass().getClassLoader(), interfaces, new ScopeInterceptor(getContainer(), 
            scopeManager, componentName, interceptor));
    }
    
    @Override
    protected void setFactoryDependencies()
    {
        if (proxyFactory == null)
        {
            if (getContainer() != null)
                proxyFactory = (IProxyFactory)getContainer().getFactory(proxyName);
            else
                throw new FactoryNotFoundException(messages.proxyFactoryNotFound(proxyName));
        }
        
        if (scopeManager == null)
        {
            if (getContainer() != null)
                scopeManager = getContainer().getComponent(scopeName);
            else
                throw new FactoryNotFoundException(messages.scopeNotFound(scopeName));
        }
        
        if (interceptor == null && interceptorName != null)
        {
            if (getContainer() != null)
                interceptor = getContainer().getComponent(interceptorName);
            else
                throw new FactoryNotFoundException(messages.interceptorNotFound(interceptorName));
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Proxy factory ''{0}'' is not found.")
        ILocalizedMessage proxyFactoryNotFound(String proxyName);
        @DefaultMessage("Scope ''{0}'' is not found.")
        ILocalizedMessage scopeNotFound(String scopeName);
        @DefaultMessage("Interceptor ''{0}'' is not found.")
        ILocalizedMessage interceptorNotFound(String interceptorName);
    }
}
