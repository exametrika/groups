/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.utils.Strings;


/**
 * The {@link StringsTests} are tests for {@link Strings}.
 * 
 * @see Strings
 * @author Medvedev-A
 */
public class StringsTests
{
    @Test
    public void testDigestToString() throws Throwable
    {
        byte[] digest = {0x0, 0x50, 0x7f, (byte)0x80, (byte)0xff};
        String s = Strings.digestToString(digest);
        assertThat(s, is("00507f80ff"));
    }
}
