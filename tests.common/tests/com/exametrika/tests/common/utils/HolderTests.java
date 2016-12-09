/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.Closeable;
import java.io.IOException;

import org.junit.Test;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Holder;
import com.exametrika.common.utils.HolderManager;


/**
 * The {@link HolderTests} are tests for {@link Holder}.
 * 
 * @see Assert
 * @author Medvedev-A
 */
public class HolderTests
{
    @Test
    public void testHolder()
    {
        HolderManager<TestCloseable> manager = new HolderManager<TestCloseable>();
        
        TestCloseable instance1 = new TestCloseable();
        TestCloseable instance2 = new TestCloseable();
        TestCloseable instance3 = new TestCloseable();
        
        Holder<TestCloseable> holder1 = manager.createHolder(instance1);
        Holder<TestCloseable> holder2 = manager.createHolder(instance2);
        manager.createHolder(instance3);
        
        assertThat(holder1.get(), is(instance1));
        holder1.addRef();
        holder1.release();
        assertThat(instance1.closed, is(false));
        holder1.release();
        assertThat(instance1.closed, is(true));
        
        holder2.close();
        assertThat(instance2.closed, is(true));
        
        manager.close();
        assertThat(instance3.closed, is(true));
    }
    
    private static class TestCloseable implements Closeable
    {
        private boolean closed;

        @Override
        public void close() throws IOException
        {
            closed = true;
        }
    }
}
