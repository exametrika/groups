/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.scope;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ScopeManager} is an implementation of {@link IScopeManager}.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class ScopeManager implements IScopeManager
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ScopeManager.class);
    private final ThreadLocal<IScope> scope = new ThreadLocal<IScope>();
    
    @Override
    public IScope getScope()
    {
        IScope scope = this.scope.get();
        if (scope != null)
            return scope;
        
        throw new MissingScopeException(messages.missingScope(Thread.currentThread()));
    }

    @Override
    public void attach(IScope scope)
    {
        Assert.notNull(scope);
        
        if (this.scope.get() != null)
            throw new ScopeAlreadyAttachedException(messages.scopeAlreadyAttached(scope, Thread.currentThread()));
        
        this.scope.set(scope);
        
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.scopeAttached(scope, Thread.currentThread()));
    }

    @Override
    public IScope detach()
    {
        IScope scope = this.scope.get();
        this.scope.remove();
        
        if (logger.isLogEnabled(LogLevel.DEBUG) && scope != null)
            logger.log(LogLevel.DEBUG, messages.scopeDetached(scope, Thread.currentThread()));
        
        return scope;
    }
    
    private interface IMessages
    {
        @DefaultMessage("Scope is missing in thread ''{0}''.")
        ILocalizedMessage missingScope(Object thread);
        @DefaultMessage("Scope ''{0}'' is already attached to thread ''{1}''.")
        ILocalizedMessage scopeAlreadyAttached(Object scope, Object thread);
        @DefaultMessage("Scope ''{0}'' is attached to thread ''{1}''.")
        ILocalizedMessage scopeAttached(Object scope, Object thread);
        @DefaultMessage("Scope ''{0}'' is detached from thread ''{1}''.")
        ILocalizedMessage scopeDetached(Object scope, Object thread);
    }
}
