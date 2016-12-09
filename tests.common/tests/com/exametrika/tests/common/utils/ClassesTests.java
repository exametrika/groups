/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.utils.Classes;


/**
 * The {@link ClassesTests} are tests for {@link Classes}.
 * 
 * @see Classes
 * @author Medvedev-A
 */
public class ClassesTests
{
    @Test
    public void testClassUtils() throws Throwable
    {
        assertThat(Classes.loadClass(TestClass.class.getName(), getClass().getClassLoader()) == TestClass.class, is(true));
        
        TestClass[] arr = new TestClass[1];
        assertThat(Classes.loadClass(arr.getClass().getName(), getClass().getClassLoader()) == arr.getClass(), is(true));
        
        String[] arr2 = new String[1];
        assertThat(Classes.loadClass(arr2.getClass().getName(), getClass().getClassLoader()) == arr2.getClass(), is(true));
        
        byte[] arr3 = new byte[1];
        assertThat(Classes.loadClass(arr3.getClass().getName(), getClass().getClassLoader()) == arr3.getClass(), is(true));
    }
    
    private static class TestClass
    {
    }
}
