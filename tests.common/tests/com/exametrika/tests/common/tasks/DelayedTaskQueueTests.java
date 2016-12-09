/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.tasks;


import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.exametrika.common.tasks.impl.Daemon;
import com.exametrika.common.tasks.impl.DelayedTaskQueue;
import com.exametrika.common.tasks.impl.RunnableTaskHandler;
import com.exametrika.common.tasks.impl.TaskExecutor;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Times;


/**
 * The {@link DelayedTaskQueueTests} are tests for {@link DelayedTaskQueue} class.
 * 
 * @see Daemon
 * @author Medvedev_A
 */
public class DelayedTaskQueueTests
{
    @Test
    public void testQueue() throws Throwable
    {
        DelayedTaskQueue<Runnable> queue = new DelayedTaskQueue<Runnable>(new SystemTimeService());
        TaskExecutor<Runnable> executor = new TaskExecutor<Runnable>(1, queue, new RunnableTaskHandler<Runnable>(), "executor");
        executor.start();

        final List<Pair<Long, Integer>> tasks = new ArrayList<Pair<Long, Integer>>();
        final long time = Times.getCurrentTime();
        for (int i = 0; i < 10; i++)
        {
            final int n = i;
            queue.offer(new Runnable()
            {
                @Override
                public void run()
                {
                    tasks.add(new Pair(Times.getCurrentTime() - time, n));
                }
            }, 1000 - i * 50);
        }
        
        Thread.sleep(2000);
        
        assertThat(tasks.size(), is(10));
        for (int i = 0 ; i < 10; i++)
        {
            assertThat(tasks.get(i).getValue(), is(9 - i));
            assertThat(tasks.get(i).getKey() > 500 + i * 50, is(true));
        }
    }
}
