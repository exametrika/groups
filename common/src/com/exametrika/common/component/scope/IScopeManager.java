/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.scope;

/**
 * The {@link IScopeManager} is a manager of scopes of particular type.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IScopeManager
{
    /**
     * Returns a scope attached to current thread.
     *
     * @return current thread's scope
     * @exception MissingScopeException if scope is not attached to current thread
     */
    IScope getScope();
    
    /**
     * Attaches a scope to current thread.
     *
     * @param scope scope to attach
     * @exception ScopeAlreadyAttachedException if another scope already attached to current thread
     */
    void attach(IScope scope);
    
    /**
     * Detaches a scope from current thread (if any).
     *
     * @return detached scope or null if there are no attached scope.
     */
    IScope detach();
}
