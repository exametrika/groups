/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.factory.scope;

import com.exametrika.common.component.container.IComponentContainer;
import com.exametrika.common.component.proxy.IInterceptor;
import com.exametrika.common.component.proxy.IInvocation;
import com.exametrika.common.component.scope.IScope;
import com.exametrika.common.component.scope.IScopeManager;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ScopeInterceptor} is an implementation of {@link IInterceptor} for dynamic lookup
 * of scoped component instances. 
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class ScopeInterceptor implements IInterceptor
{
    private final IComponentContainer container;
    private final IScopeManager scopeManager;
    private final String componentName;
    private final IInterceptor interceptor;

    /**
     * Creates a new object.
     *
     * @param container component container
     * @param scopeManager scope manager
     * @param componentName name of scoped component in container and scope
     * @param interceptor additional interceptor. Can be <c>null<c>
     */
    public ScopeInterceptor(IComponentContainer container, IScopeManager scopeManager, String componentName, 
        IInterceptor interceptor)
    {
        Assert.notNull(container);
        Assert.notNull(scopeManager);
        Assert.notNull(componentName);
        
        this.container = container;
        this.scopeManager = scopeManager;
        this.componentName = componentName;
        this.interceptor = interceptor;
    }
    
    @Override
    public <T> T invoke(IInvocation invocation)
    {
        IScope scope = scopeManager.getScope();
        
        Object target = scope.get(componentName);
        if (target == null)
        {
            target = container.getComponent(componentName);
            scope.add(componentName, target);
        }
        
        invocation.setTarget(target);

        if (interceptor != null)
            return interceptor.invoke(invocation);
    
        return invocation.proceed();
    }
}
