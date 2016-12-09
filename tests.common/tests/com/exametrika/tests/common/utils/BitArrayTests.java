/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.utils.BitArray;


/**
 * The {@link BitArrayTests} are tests for {@link BitArray}.
 * 
 * @see BitArray
 * @author Medvedev-A
 */
public class BitArrayTests
{
    @Test
    public void testBitArray() throws Exception
    {
        BitArray array = new BitArray(1000);
        
        // Test get, set, clear
        for (int i = 0; i < array.getLength(); i++)
        {
            assertThat(array.get(i), is(false));
            array.set(i);
            assertThat(array.get(i), is(true));
            array.clear(i);
            assertThat(array.get(i), is(false));
            array.set(i, true);
            assertThat(array.get(i), is(true));
            array.set(i, false);
            assertThat(array.get(i), is(false));
        }
        
        // Test and
        BitArray array2 = new BitArray(1000);
        for (int i = 0; i < array2.getLength(); i++)
            array2.set(i, true);
        
        array = new BitArray(1000);
        for (int i = 0; i < array.getLength(); i++)
            array.set(i, (i % 2) != 0);
        
        array.and(array2);
        
        for (int i = 0; i < array.getLength(); i++)
            assertThat(array.get(i), is((i % 2) != 0));
        
        // Test or
        array = new BitArray(1000);
        for (int i = 0; i < array.getLength(); i++)
            array.set(i, (i % 2) != 0);
        
        array.or(array2);
        
        for (int i = 0; i < array.getLength(); i++)
            assertThat(array.get(i), is(true));
        
        // Test xor
        array = new BitArray(1000);
        for (int i = 0; i < array.getLength(); i++)
            array.set(i, (i % 2) != 0);
        
        array.xor(array2);
        
        for (int i = 0; i < array.getLength(); i++)
            assertThat(array.get(i), is(((i % 2) == 0)));
        
        // Test bit count
        array = new BitArray(100);
        assertThat(array.getBitCount(), is(0));
        for (int i = 0; i < array.getLength(); i++)
            array.set(i, (i % 2) != 0);
        
        assertThat(array.getBitCount(), is(array.getLength() / 2));
        
        for (int i = 0; i < array.getLength(); i++)
            array.set(i, true);
        
        assertThat(array.getBitCount(), is(array.getLength()));
    }
}
