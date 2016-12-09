/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.component;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.component.scope.ComponentAlreadyInScopeException;
import com.exametrika.common.component.scope.Scope;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.ILifecycle;


/**
 * The {@link ScopeTests} are tests for {@link Scope}.
 * 
 * @see Scope
 * @author Medvedev-A
 */
public class ScopeTests
{
    @Test
    public void testScope() throws Throwable
    {
        final Scope scope = new Scope();
        scope.start();
        
        assertThat(scope.get("test"), nullValue());
        
        TestComponent component = new TestComponent();
        
        scope.add("test", component);
        assertThat(scope.get("test"), is((Object)component));
        
        new Expected(ComponentAlreadyInScopeException.class, new Runnable()
        {
            @Override
            public void run()
            {
                scope.add("test", new TestComponent());
            }
        });
        
        scope.stop();
        
        assertThat(component.stopped, is(true));
    }
    
    private static class TestComponent implements ILifecycle
    {
        boolean stopped;
        
        @Override
        public void start()
        {
        }

        @Override
        public void stop()
        {
            stopped = true;
        }
    }
}