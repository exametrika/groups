/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.tasks;


import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.tasks.impl.Timer;


/**
 * The {@link TimerTests} are tests for {@link Timer} class.
 * 
 * @see Timer
 * @author Medvedev_A
 */
public class TimerTests
{
    @Test
    public void testTimer() throws Throwable
    {
        TestTimerListener listener = new TestTimerListener();
        Timer timer = new Timer(10, listener, false, "testTimer", null);
        timer.start();
        
        Thread.sleep(100);
        
        timer.stop();
        
        int counter = listener.counter;
        assertThat(counter > 0 && counter < 15, is(true));
        
        Thread.sleep(100);
        assertThat(listener.counter, is(counter));
    }
    
    @Test
    public void testSuspendResume() throws Throwable
    {
        TestTimerListener listener = new TestTimerListener();
        Timer timer = new Timer(10, listener, true, "testTimer", null);
        timer.start();
        
        Thread.sleep(100);
        
        int counter = listener.counter;
        assertThat(counter, is(0));
        
        timer.resume();
        
        Thread.sleep(100);
        
        counter = listener.counter;
        assertThat(counter > 0 && counter < 11, is(true));

        timer.suspend();
        
        Thread.sleep(100);
        
        timer.stop();
    }
    
    @Test
    public void testSignal() throws Throwable
    {
        TestTimerListener listener = new TestTimerListener();
        Timer timer = new Timer(100000000, listener, false, "testTimer", null);
        timer.start();
        
        Thread.sleep(100);
        
        int counter = listener.counter;
        assertThat(counter, is(0));
        
        timer.signal();
        
        Thread.sleep(100);
        
        counter = listener.counter;
        assertThat(counter == 1, is(true));
        
        timer.stop();
    }
    
    private static class TestTimerListener implements ITimerListener
    {
        public volatile int counter;
        
        @Override
        public void onTimer()
        {
            counter++;
        }
    }
}
