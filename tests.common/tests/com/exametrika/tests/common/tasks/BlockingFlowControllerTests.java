/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.tasks;


import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.tasks.impl.BlockingFlowController;
import com.exametrika.common.tasks.impl.RunnableTaskHandler;
import com.exametrika.common.tasks.impl.TaskExecutor;
import com.exametrika.common.tasks.impl.TaskQueue;
import com.exametrika.common.utils.ICondition;


/**
 * The {@link BlockingFlowControllerTests} are tests for {@link BlockingFlowController} class.
 * 
 * @see BlockingFlowController
 * @author Medvedev_A
 */
public class BlockingFlowControllerTests
{
    private BlockingFlowController<TestTask> controller;
    private TaskQueue<TestTask> queue;
    private TaskExecutor<TestTask> executor;
    
    @Before
    public void setUp()
    {
        queue = new TaskQueue<TestTask>();
        executor = new TaskExecutor<TestTask>(10, queue, new RunnableTaskHandler<TestTask>(), null);
        executor.start();

        controller = new BlockingFlowController<TestTask>();
    }
    
    @Test
    public void testLockUnlock() throws Throwable
    {
        TestTask[] tasks = new TestTask[10];
        for (int i = 0; i < tasks.length; i++)
            tasks[i] = new TestTask(i);
        
        controller.lockFlow(tasks[0]);
        controller.lockFlow(tasks[0]);
        controller.lockFlow(tasks[1]);
        controller.lockFlow(tasks[1]);
        controller.lockFlow(tasks[2]);
        
        for (int i = 0; i < tasks.length; i++)
            queue.offer(tasks[i]);
        
        Thread.sleep(200);
        
        for (int i = 0; i < tasks.length; i++)
        {
            if (i <= 2)
                assertThat(tasks[i].done, is(false));
            else
                assertThat(tasks[i].done, is(true));
        }
        
        controller.unlockFlow(tasks[0]);
        controller.unlockFlow(tasks[1]);
        controller.unlockFlow(tasks[2]);
        
        Thread.sleep(200);
        
        assertThat(tasks[0].done, is(false));
        assertThat(tasks[1].done, is(false));
        assertThat(tasks[2].done, is(true));
        
        controller.unlockFlow(tasks[0]);
        controller.unlockFlow(tasks[1]);
        
        Thread.sleep(200);
        
        assertThat(tasks[0].done, is(true));
        assertThat(tasks[1].done, is(true));
    }

    @Test
    public void testRemove() throws Throwable
    {
        TestTask[] tasks = new TestTask[10];
        for (int i = 0; i < tasks.length; i++)
        {
            tasks[i] = new TestTask(i);
            controller.lockFlow(tasks[i]);
            controller.lockFlow(tasks[i]);
        }
        
        for (int i = 0; i < tasks.length; i++)
            queue.offer(tasks[i]);
        
        Thread.sleep(200);
        
        for (int i = 0; i < tasks.length; i++)
            assertThat(tasks[i].done, is(false));

        controller.removeFlow(tasks[0]);
        
        Thread.sleep(200);
        
        for (int i = 0; i < tasks.length; i++)
        {
            if (i != 0)
                assertThat(tasks[i].done, is(false));
            else
                assertThat(tasks[i].done, is(true));
        }
        
        controller.removeFlows(new ICondition<TestTask>()
        {
            @Override
            public boolean evaluate(TestTask value)
            {
                return value.i <= 5;
            }
        });
        
        Thread.sleep(200);
        
        for (int i = 0; i < tasks.length; i++)
        {
            if (i > 5)
                assertThat(tasks[i].done, is(false));
            else
                assertThat(tasks[i].done, is(true));
        }
        
        controller.removeAllFlows();
        
        Thread.sleep(200);
        
        for (int i = 0; i < tasks.length; i++)
            assertThat(tasks[i].done, is(true));
    }

    private class TestTask implements Runnable
    {
        boolean done;
        private final int i;
        
        public TestTask(int i)
        {
            this.i = i;
        }
        
        @Override
        public void run()
        {
            controller.await(this);
            done = true;
        }
    }
}    

