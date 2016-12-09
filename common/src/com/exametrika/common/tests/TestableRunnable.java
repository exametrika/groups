/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tests;

import com.exametrika.common.utils.Assert;

/**
 * The {@link TestableRunnable} is an implementation of {@link Runnable} using {@link ITestable}.
 * 
 * @author medvedev
 */
public final class TestableRunnable implements Runnable
{
    private final ITestable testable;

    public TestableRunnable(ITestable testable)
    {
        Assert.notNull(testable);
        this.testable = testable;
    }
    
    @Override
    public void run()
    {
        try
        {
            testable.test();
        }
        catch (Error e)
        {
            throw e;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException();
        }
        catch (Throwable e)
        {
            throw new RuntimeException();
        }
    }
}
