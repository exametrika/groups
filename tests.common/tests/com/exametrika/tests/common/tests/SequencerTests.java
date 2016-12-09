/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.tests;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.tests.Expected;
import com.exametrika.common.tests.ITestable;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.tests.TestableRunnable;
import com.exametrika.common.utils.ICondition;


/**
 * The {@link SequencerTests} are tests for {@link Sequencer}.
 * 
 * @see Sequencer
 * @author medvedev
 */
public class SequencerTests
{
    private Sequencer sequencer = new Sequencer();
    
    @Test
    public void testBarriers() throws Throwable
    {
        sequencer.createBarrier(11);
        
        Thread[] threads = new Thread[10];
        final int[] values = new int[10]; 
        
        for (int i = 0; i < threads.length; i++)
        {
            final int n = i;
            Thread thread = new Thread(new TestableRunnable(new ITestable()
            {
                @Override
                public void test() throws Throwable
                {
                    for (int k = 0; k < 10; k++)
                    {
                        sequencer.waitBarrier(Long.MAX_VALUE, Integer.toString(k));
                        
                        for (int value : values)
                            assertThat(value == k || value == k + 1, is(true));
                        
                        values[n]++;
                    }
                }
            }));
            thread.start();
            threads[i] = thread;
        }
        
        for (int k = 0; k < 10; k++)
            sequencer.waitBarrier(Long.MAX_VALUE, Integer.toString(k));
        
        for (Thread thread : threads)
            thread.join();
    }
    
    @Test
    public void testWaitAllAllow() throws Throwable
    {
        Thread[] threads = new Thread[10];
        final int[] values = new int[10];
        
        for (int i = 0; i < threads.length; i++)
        {
            final int n = i;
            Thread thread = new Thread(new TestableRunnable(new ITestable()
            {
                @Override
                public void test() throws Throwable
                {
                    Thread.sleep(200);
                    values[n] = n;
                    sequencer.allowSingle(Integer.toString(n));
                }
            }));
            thread.start();
            threads[i] = thread;
        }
        
        sequencer.waitAll(10);
        
        for (int i = 0; i < values.length; i++)
            assertThat(values[i], is(i));
        
        for (Thread thread : threads)
            thread.join();
    }
    
    @Test
    public void testWaitAllDeny() throws Throwable
    {
        Thread[] threads = new Thread[10];
        final int[] values = new int[10];
        
        for (int i = 0; i < threads.length; i++)
        {
            final int n = i;
            Thread thread = new Thread(new TestableRunnable(new ITestable()
            {
                @Override
                public void test() throws Throwable
                {
                    Thread.sleep(200);
                    values[n] = n;
                    
                    if (n != 9)
                        sequencer.allowSingle(Integer.toString(n));
                    else
                        sequencer.denySingle();
                }
            }));
            thread.start();
            threads[i] = thread;
        }
        
        new Expected(AssertionError.class, new ITestable()
        {
            @Override
            public void test() throws Throwable
            {
                sequencer.waitAll(10);
            }
        });
        
        for (Thread thread : threads)
            thread.join();
    }
    
    @Test
    public void testWaitAllDenyException() throws Throwable
    {
        Thread[] threads = new Thread[10];
        final int[] values = new int[10];
        
        for (int i = 0; i < threads.length; i++)
        {
            final int n = i;
            Thread thread = new Thread(new TestableRunnable(new ITestable()
            {
                @Override
                public void test() throws Throwable
                {
                    Thread.sleep(200);
                    values[n] = n;
                    
                    if (n != 9)
                        sequencer.allowSingle(Integer.toString(n));
                    else
                        sequencer.denySingle(new IllegalArgumentException());
                }
            }));
            thread.start();
            threads[i] = thread;
        }
        
        new Expected(IllegalArgumentException.class, new ITestable()
        {
            @Override
            public void test() throws Throwable
            {
                sequencer.waitAll(10);
            }
        });
        
        for (Thread thread : threads)
            thread.join();
    }
    
    @Test
    public void testWaitAllDenyMessage() throws Throwable
    {
        Thread[] threads = new Thread[10];
        final int[] values = new int[10];
        
        for (int i = 0; i < threads.length; i++)
        {
            final int n = i;
            Thread thread = new Thread(new TestableRunnable(new ITestable()
            {
                @Override
                public void test() throws Throwable
                {
                    Thread.sleep(200);
                    values[n] = n;
                    
                    if (n != 9)
                        sequencer.allowSingle(Integer.toString(n));
                    else
                        sequencer.denySingle("test");
                }
            }));
            thread.start();
            threads[i] = thread;
        }
        
        new Expected(new ICondition<Throwable>()
        {
            @Override
            public boolean evaluate(Throwable value)
            {
                return value instanceof AssertionError && ((AssertionError)value).getMessage().equals("test");
            }
        }, AssertionError.class, new ITestable()
        {
            @Override
            public void test() throws Throwable
            {
                sequencer.waitAll(10);
            }
        });
        
        for (Thread thread : threads)
            thread.join();
    }
    
    @Test
    public void testWaitSingle() throws Throwable
    {
        Thread[] threads = new Thread[10];
        final int[] values = new int[1];
        
        for (int i = 0; i < threads.length; i++)
        {
            final int n = i;
            Thread thread = new Thread(new TestableRunnable(new ITestable()
            {
                @Override
                public void test() throws Throwable
                {
                    sequencer.waitSingle(Long.MAX_VALUE, Integer.toString(n));
                    assertThat(values[0], is(100));
                    sequencer.allowSingle();
                }
            }));
            thread.start();
            threads[i] = thread;
        }
        
        Thread.sleep(200);
        values[0] = 100;
        
        sequencer.allowAll(10, "main");
        sequencer.waitAll(10);
        
        for (Thread thread : threads)
            thread.join();
    }
}
