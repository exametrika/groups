/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tests;

/**
 * The {@link ITestable} is similar to {@link Runnable} but allows arbitrary type of exception to be thrown.
 * 
 * @author medvedev
 */
public interface ITestable
{
    /**
     * Runs execution.
     *
     * @throws Throwable if exception occures
     */
    void test() throws Throwable;
}
