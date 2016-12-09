/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.tasks;


import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.common.tasks.IActivationCondition;
import com.exametrika.common.tasks.ITaskContext;
import com.exametrika.common.tasks.impl.RunnableTaskHandler;
import com.exametrika.common.tasks.impl.TaskScheduler;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Times;


/**
 * The {@link TaskSchedulerTests} are tests for {@link TaskScheduler} class.
 * 
 * @see TaskScheduler
 * @author Medvedev_A
 */
public class TaskSchedulerTests
{
    private TaskScheduler<Runnable> taskScheduler;

    @Before
    public void setUp()
    {
        TaskMock.active.clear();
        taskScheduler = new TaskScheduler(10, new RunnableTaskHandler(), new TimeServiceMock(), 50, "testScheduler", "testExecutor", null);
        taskScheduler.start();
    }
    
    @After
    public void tearDown()
    {
        taskScheduler.stop();
    }
    
    @Test
    public void testAsyncTask() throws Throwable
    {
        TaskMock task = new TaskMock("group", 100);
        taskScheduler.offer(task);
        
        task.await(500);
        
        assertThat(task.executionCount == 1, is(true));
    }
    
    @Test
    public void testAddRecurrentTask() throws Throwable
    {
        TaskMock task = new TaskMock("group", 100);
        taskScheduler.addTask("task", task, new ActivationConditionMock(task, null), true, false, null);
        assertThat(taskScheduler.findTask("task") == task, is(true));
        
        Thread.sleep(1000);
        
        assertThat(task.executionCount == 3, is(true));
    }
    
    @Test
    public void testAddOneTimeTask() throws Throwable
    {
        TaskMock task = new TaskMock("group", 100);
        taskScheduler.addTask("task", task, new ActivationConditionMock(task, null), false, false, null);
        
        task.await(500);

        Thread.sleep(500);
        
        assertThat(task.executionCount == 1, is(true));
    }
    
    @Test
    public void testTaskContext() throws Throwable
    {
        TaskMock task1 = new TaskMock("group1", 500);
        TaskMock task2 = new TaskMock("group1", 500);
        TaskMock task3 = new TaskMock("group2", 500);
        taskScheduler.addTask("task1", task1, new ActivationConditionMock(task1, "group1"), true, false, null);
        taskScheduler.addTask("task2", task2, new ActivationConditionMock(task2, "group1"), true, false, null);
        taskScheduler.addTask("task3", task3, new ActivationConditionMock(task3, "group2"), true, false, null);
        
        Thread.sleep(6000);
        
        assertThat(task1.executionCount == 3, is(true));
        assertThat(task2.executionCount == 3, is(true));
        assertThat(task3.executionCount == 3, is(true));
    }
    
    @Test
    public void testInterruptTask() throws Throwable
    {
        TaskMock task1 = new TaskMock("group1", 100000);
        ActivationConditionMock condition = new ActivationConditionMock(task1, "group1");
        condition.maxExecutionTime = 200;
        condition.count = Integer.MAX_VALUE;
        taskScheduler.addTask("task1", task1, condition, true, false, null);
        
        Thread.sleep(1000);
        
        assertThat(task1.executionCount > 3, is(true));
    }
    
    @Test
    public void testRemoveTask() throws Throwable
    {
        TaskMock task = new TaskMock("group", 100);
        ActivationConditionMock activationCondition = new ActivationConditionMock(task, null);
        taskScheduler.addTask("task", task, activationCondition, true, false, null);
        taskScheduler.removeTask("task");
        assertThat(taskScheduler.findTask("task"), nullValue());
        
        Thread.sleep(500);
        
        task.executionCount = 0;
        
        Thread.sleep(500);
        
        assertThat(task.executionCount == 0, is(true));
    }
    
    @Test
    public void testRemoveAllTasks() throws Throwable
    {
        TaskMock task = new TaskMock("group", 100);
        ActivationConditionMock activationCondition = new ActivationConditionMock(task, null);
        taskScheduler.addTask("task", task, activationCondition, true, false, null);
        taskScheduler.removeAllTasks();
        assertThat(taskScheduler.findTask("task"), nullValue());
        
        Thread.sleep(500);
        
        task.executionCount = 0;
        
        Thread.sleep(500);
        
        assertThat(task.executionCount == 0, is(true));
    }
    
    private static class TaskMock implements Runnable
    {
        private ReentrantLock lock = new ReentrantLock();
        private Condition condVar = lock.newCondition();
        private volatile long executionTime;
        volatile int executionCount = 0;
        private boolean signaled = false;
        private volatile boolean executing = false;
        private static final Map<String, TaskMock> active = new ConcurrentHashMap<String, TaskMock>();
        private final String group;
        private final long count;
        private volatile boolean canceled;
        
        public TaskMock(String group, int count)
        {
            this.group = group;
            this.count = count;
        }
        
        @Override
        public void run()
        {
            assertThat(executing, is(false));
            assertThat(active.containsKey(group), is(false));
            active.put(group, this);
            
            executing = true;
            executionTime = Times.getCurrentTime();
            executionCount++;
            
            try
            {
                for (int i = 0; i < count; i++)
                {
                    if (canceled)
                        break;
                    Thread.sleep(1);
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            finally
            {
                executing = false;
                active.remove(group);
            }
            
            signal();
        }
        
        public void await(long time) throws Throwable
        {
            lock.lock();
            
            try
            {
                while (!signaled)
                {
                    try
                    {
                        if (!condVar.await(time, TimeUnit.MILLISECONDS) && !signaled)
                        {
                            // Timeout occurs only when pending lock is not signaled (i.e. acquired)
                            throw new TimeoutException();
                        }
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
            finally
            {
                signaled = false;
                lock.unlock();
            }
        }
        
        private void signal()
        {
            lock.lock();
            signaled = true;
            condVar.signal();
            lock.unlock();
        }
    }

    private static class ActivationConditionMock implements IActivationCondition<Runnable>
    {
        private final TaskMock task;
        private int count = 3;
        private final String group;
        private long maxExecutionTime;

        public ActivationConditionMock(TaskMock task, String group)
        {
            this.task = task;
            this.group = group;
        }
        
        @Override
        public boolean evaluate(Long value)
        {
            // #canActivate supersedes this
            return false;
        }

        @Override
        public boolean canActivate(long currentTime, ITaskContext context)
        {
            if (count <= 0)
                return false;
            
            if (context.getParameters().containsKey(group))
                return false;
            
            if (currentTime - task.executionTime > 200)
            {
                // Execute every 0.2 seconds
                count--;
                
                if (group != null)
                    assertThat(context.getParameters().put(group, this), nullValue());
                return true;
            }
            
            return false;
        }

        @Override
        public void tryInterrupt(long currentTime)
        {
            if (maxExecutionTime != 0 && (currentTime - task.executionTime > maxExecutionTime))
                task.canceled = true;
        }

        @Override
        public void onCompleted(ITaskContext context)
        {
            if (group != null)
                assertThat(context.getParameters().remove(group) == this, is(true));
        }
    }
    
    private static class TimeServiceMock implements ITimeService
    {
        @Override
        public long getCurrentTime()
        {
            return Times.getCurrentTime();
        }
    }
}
