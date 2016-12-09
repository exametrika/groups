/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.tasks;


import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.tasks.impl.Daemon;
import com.exametrika.common.tests.Sequencer;


/**
 * The {@link DaemonTests} are tests for {@link Daemon} class.
 * 
 * @see Daemon
 * @author Medvedev_A
 */
public class DaemonTests
{
    private Sequencer sequencer = new Sequencer();
    
    @Test
    public void testDaemon() throws Throwable
    {
        sequencer.createBarrier(2);
        
        Handler handler = new Handler();
        Daemon daemon = new Daemon(handler);
        daemon.start();
        
        sequencer.waitBarrier();
        
        assertThat(handler.counter, is(1));
        daemon.stop();
    }
    
    private class Handler implements Runnable
    {
        public volatile int counter;
        
        @Override
        public void run()
        {
            counter++;
            
            sequencer.waitBarrier();
            
            try
            {
                Thread.sleep(Integer.MAX_VALUE);
            }
            catch (InterruptedException e)
            {
                throw new ThreadInterruptedException(e);
            }
        }
    }
}
