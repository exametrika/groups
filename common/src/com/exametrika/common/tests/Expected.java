/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tests;

import com.exametrika.common.utils.ICondition;

/**
 * The {@link Expected} represents a catch checking for specified expected exception. 
 *
 * @see Runnable
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev_A
 */
public final class Expected
{
    private Throwable exception;
    
    public Expected(Class<?> exceptionClass, Runnable command) throws Throwable
    {
        try
        {
            command.run();
        }
        catch (Throwable e)
        {
            exception = e;
            if (e.getClass().equals(exceptionClass))
                return;
            
            throw e;
        }
        
        throw new AssertionError("Expected exception is not thrown:" + exceptionClass.getName());
    }
    
    public Expected(Class<?> exceptionClass, ITestable command) throws Throwable
    {
        try
        {
            command.test();
        }
        catch (Throwable e)
        {
            exception = e;
            if (e.getClass().equals(exceptionClass))
                return;
            
            throw e;
        }
        
        throw new AssertionError("Expected exception is not thrown:" + exceptionClass.getName());
    }
    
    public Expected(Class<?> exceptionClass, Runnable command, String message) throws Throwable
    {
        try
        {
            command.run();
        }
        catch (Throwable e)
        {
            exception = e;
            if (e.getClass().equals(exceptionClass))
                return;
            
            throw e;
        }
        
        throw new AssertionError(message);
    }
    
    public Expected(Class<?> exceptionClass, ITestable command, String message) throws Throwable
    {
        try
        {
            command.test();
        }
        catch (Throwable e)
        {
            exception = e;
            if (e.getClass().equals(exceptionClass))
                return;
            
            throw e;
        }
        
        throw new AssertionError(message);
    }
    
    public Expected(ICondition<Throwable> condition, Class<?> exceptionClass, Runnable command) throws Throwable
    {
        try
        {
            command.run();
        }
        catch (Throwable e)
        {
            exception = e;
            if (condition.evaluate(e))
                return;
            
            throw e;
        }
        
        throw new AssertionError("Expected exception is not thrown:" + exceptionClass.getName());
    }
    
    public Expected(ICondition<Throwable> condition, Class<?> exceptionClass, ITestable command) throws Throwable
    {
        try
        {
            command.test();
        }
        catch (Throwable e)
        {
            exception = e;
            if (condition.evaluate(e))
                return;
            
            throw e;
        }
        
        throw new AssertionError("Expected exception is not thrown:" + exceptionClass.getName());
    }
    
    public Expected(ICondition<Throwable> condition, Class<?> exceptionClass, Runnable command, String message) throws Throwable
    {
        try
        {
            command.run();
        }
        catch (Throwable e)
        {
            exception = e;
            if (condition.evaluate(e))
                return;
            
            throw e;
        }
        
        throw new AssertionError(message);
    }
    
    public Expected(ICondition<Throwable> condition, Class<?> exceptionClass, ITestable command, String message) throws Throwable
    {
        try
        {
            command.test();
        }
        catch (Throwable e)
        {
            exception = e;
            if (condition.evaluate(e))
                return;
            
            throw e;
        }
        
        throw new AssertionError(message);
    }
    
    public Throwable getException()
    {
        return exception;
    }
}
