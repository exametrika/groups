/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.tests;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.tests.Tests;


/**
 * The {@link TestsTests} are tests for {@link Tests}. 
 * 
 * @see Tests
 * @author medvedev
 */
@SuppressWarnings("unused")
public class TestsTests
{
    @Test
    public void testFieldAccess() throws Throwable
    {
        TestA a = new TestA(10);
        assertThat((Integer)Tests.get(a, "field1"), is(10));
        
        TestB b = new TestB(20, "test");
        assertThat((Integer)Tests.get(b, "field1"), is(20));
        assertThat((String)Tests.get(b, "field2"), is("test"));
    }
    
    private static class TestA
    {
        private int field1;
        
        public TestA(int value)
        {
            field1 = value;
        }
    }
    
    private static class TestB extends TestA
    {
        private String field2;
        
        public TestB(int value1, String value2)
        {
            super(value1);
            field2 = value2;
        }
    }
}
