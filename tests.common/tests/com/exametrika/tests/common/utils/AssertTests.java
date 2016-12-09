/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import org.junit.Test;

import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.Assert;


/**
 * The {@link AssertTests} are tests for {@link Assert}.
 * 
 * @see Assert
 * @author Medvedev-A
 */
public class AssertTests
{
    @Test()
    public void testAssert() throws Throwable
    {
        Assert.isNull(null);
        Assert.isNull(null, "test");
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.isNull(new Object());        
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.isNull(new Object(), "test");        
            }
        });
        
        Assert.notNull(new Object());
        Assert.notNull(new Object(), "test");
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.notNull(null);        
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.notNull(null, "test");        
            }
        });
        
        Assert.isInstanceOf(String.class, "");
        Assert.isInstanceOf(String.class, "", "test");
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.isInstanceOf(String.class, new Object());        
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.isInstanceOf(String.class, new Object(), "test");        
            }
        });
        
        Assert.isTrue(true);
        Assert.isTrue(true, "test");
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.isTrue(false);        
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.isTrue(false, "test");        
            }
        });
        
        Assert.checkState(true);
        Assert.checkState(true, "test");
        
        new Expected(IllegalStateException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.checkState(false);        
            }
        });
        
        new Expected(IllegalStateException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.checkState(false, "test");        
            }
        });
        
        Assert.hasLength("test");
        Assert.hasLength("test", "test");
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.hasLength(null);        
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.hasLength(null, "test");        
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.hasLength("");        
            }
        });
        
        new Expected(IllegalArgumentException.class, new Runnable()
        {
            @Override
            public void run()
            {
                Assert.hasLength("", "test");        
            }
        });
    }
}
