/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.utils.SparseArray;


/**
 * The {@link SparseArrayTests} are tests for {@link SparseArray}.
 * 
 * @see SparseArray
 * @author Medvedev-A
 */
public class SparseArrayTests
{
    @Test
    public void testSparseArray()
    {
        SparseArray<Integer> array = new SparseArray<Integer>();
        
        for (int i = 0; i < 100000; i++)
        {
            assertThat(array.get(i), nullValue());
            assertThat(array.getCount(), is(i));
            array.set(i, i);
            assertThat(array.get(i), is(i));
            assertThat(array.getCount(), is(i + 1));
        }
        
        array.clear();
        assertThat(array.get(0), nullValue());
        assertThat(array.getCount(), is(0));
    }
}
