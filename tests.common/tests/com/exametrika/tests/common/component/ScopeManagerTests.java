/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.component;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.component.scope.IScope;
import com.exametrika.common.component.scope.MissingScopeException;
import com.exametrika.common.component.scope.Scope;
import com.exametrika.common.component.scope.ScopeAlreadyAttachedException;
import com.exametrika.common.component.scope.ScopeManager;
import com.exametrika.common.tests.Expected;


/**
 * The {@link ScopeManagerTests} are tests for {@link ScopeManager}.
 * 
 * @see ScopeManager
 * @author Medvedev-A
 */
public class ScopeManagerTests
{
    @Test
    public void testScopeManager() throws Throwable
    {
        final ScopeManager manager = new ScopeManager();
        
        new Expected(MissingScopeException.class, new Runnable()
        {
            @Override
            public void run()
            {
                manager.getScope();
            }
        });
        
        IScope scope = new Scope();
        manager.attach(scope);
        
        assertThat(manager.getScope(), is(scope));
        
        new Expected(ScopeAlreadyAttachedException.class, new Runnable()
        {
            @Override
            public void run()
            {
                manager.attach(new Scope());
            }
        });
        
        assertThat(manager.detach(), is(scope));
        assertThat(manager.detach(), nullValue());
    }
}