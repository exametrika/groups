/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.perf.Benchmark;
import com.exametrika.common.perf.IProbe;
import com.exametrika.common.perf.Probe;
import com.exametrika.common.utils.Fields;
import com.exametrika.common.utils.Methods;


/**
 * The {@link MethodsTests} are tests for {@link Methods}.
 * 
 * @see Fields
 * @author Medvedev-A
 */
public class MethodsTests
{
    @Test
    public void testMethods()
    {
        String value = "ooooobbbbd";
        assertThat(Methods.getMethod(value, "length") != null, is(true));
        assertThat(Methods.getMethod(value, "startsWith", String.class, int.class) != null, is(true));
        assertThat(Methods.<Integer>invoke(value, "length"), is(Integer.valueOf(10)));
        assertThat(Methods.getConstructor(String.class, String.class) != null, is(true));
        assertThat(Methods.<String>newInstance(String.class), is(""));
        
        assertThat(Methods.builder(value)
            .method("substring")
                .param(int.class, 5)
                .invoke()
            .method("substring")
                .param(int.class, 4)
                .invoke()
            .<String>toResult(), is("d"));
        
        assertThat(Methods.builder(null)
            .method("substring")
                .param(int.class, 5)
                .invoke()
            .method("substring")
                .param(int.class, 4)
                .invoke()
            .<String>toResult(), nullValue());
        
        assertThat(Methods.builder(value)
            .invoke("trim")
            .invoke("length")
            .<Integer>toResult(), is(10));
        
        assertThat(Methods.builder(null)
            .invoke("trim")
            .invoke("length")
            .<Integer>toResult(), nullValue());
    }
    
    @Test
    public void testPerfMethods()
    {
        new Benchmark<IProbe>(new Probe()
        {
            @Override
            public long run()
            {
                String value = "ooooobbbbd";
                Methods.builder(value)
                .method("substring")
                    .param(int.class, 5)
                    .invoke()
                .method("substring")
                    .param(int.class, 4)
                    .invoke()
                .<String>toResult();

                return 1;
            }
            
        }, 10000).print("========= invoke builder performance: ");
    }
}
