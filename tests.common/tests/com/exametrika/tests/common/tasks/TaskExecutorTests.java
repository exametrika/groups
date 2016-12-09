/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.tasks;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.exametrika.common.tasks.ITaskListener;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.tasks.impl.RunnableTaskHandler;
import com.exametrika.common.tasks.impl.TaskExecutor;
import com.exametrika.common.tasks.impl.TaskQueue;
import com.exametrika.common.tests.Sequencer;
import com.exametrika.common.utils.Threads;
import com.exametrika.common.utils.Times;


/**
 * The {@link TaskExecutorTests} are tests for {@link TaskExecutor} class.
 * 
 * @see TaskExecutor
 * @author Medvedev_A
 */
public class TaskExecutorTests
{
    private Sequencer sequencer = new Sequencer();
    
    @Test
    public void testExecute() throws Throwable
    {
        // Check task order
        TaskQueue queue = new TaskQueue();
        TaskExecutor<Runnable> taskExecutor = new TaskExecutor(1, queue, new RunnableTaskHandler(), null);
        taskExecutor.start();
        
        final List<Integer> commandOrder = new ArrayList<Integer>();
        
        for (int i = 0; i < 200; i++)
        {
            final int k = i;
            queue.offer(new Runnable()
            {
                @Override
                public void run()
                {
                    commandOrder.add(k);
                }
            });
        }
        
        taskExecutor.stop();
        
        Integer lastPriority = -1;
        for (Integer priority : commandOrder)
        {
            assertThat(lastPriority < priority, is(true));
            lastPriority = priority;
        }
        
        // Check threads count
        queue = new TaskQueue();
        taskExecutor = new TaskExecutor(10, queue, new RunnableTaskHandler(), null);
        taskExecutor.start();
        
        for (int i = 0; i < 1000; i++)
        {
            queue.offer(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Thread.sleep(20);
                    }
                    catch (InterruptedException e)
                    {
                        throw new ThreadInterruptedException(e);
                    }
                    sequencer.allowSingle();
                }
            });
        }
        
        sequencer.waitAll(1000);
        
        taskExecutor.setThreadCount(20);

        for (int i = 0; i < 1000; i++)
        {
            queue.offer(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Thread.sleep(10);
                    }
                    catch (InterruptedException e)
                    {
                        throw new ThreadInterruptedException(e);
                    }
                    sequencer.allowSingle();
                }
            });
        }
        
        sequencer.waitAll(1000);
        
        taskExecutor.setThreadCount(5);
        Threads.sleep(100);

        for (int i = 0; i < 1000; i++)
        {
            queue.offer(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Thread.sleep(10);
                    }
                    catch (InterruptedException e)
                    {
                        throw new ThreadInterruptedException(e);
                    }
                    sequencer.allowSingle();
                }
            });
        }
        
        sequencer.waitAll(1000);
        
        taskExecutor.stop();
    }
    
    @Test
    public void testListeners() throws Throwable
    {
        // Check invocation of task listeners
        TaskQueue queue = new TaskQueue();
        TaskExecutor taskExecutor = new TaskExecutor(10, queue, new RunnableTaskHandler(), null);
        taskExecutor.start();
        
        TaskListenerMock listener1 = new TaskListenerMock();
        TaskListenerMock listener2 = new TaskListenerMock();
        
        taskExecutor.addTaskListener(listener1);
        taskExecutor.addTaskListener(listener2);
        
        for (int i = 0; i < 100; i++)
        {
            queue.offer(new Task(i));
        }
        
        Thread.sleep(100);
        
        taskExecutor.stop();
        
        checkListener(listener1);
        checkListener(listener2);
    }
    
    private void checkListener(TaskListenerMock listener)
    {
        assertThat(listener.tasks.size(), is(100));
        
        for (Map.Entry<Integer, TaskInfo> entry : listener.tasks.entrySet())
        {
            TaskInfo info = entry.getValue();
            
            assertThat(info.startTime <= info.task.executionTime, is(true));
            assertThat(info.completeTime >= info.task.executionTime, is(true));
            assertThat(info.task.threadId, is(info.threadId));
            
            if ((info.task.id % 10) == 0)
            {
                assertThat(info.error, instanceOf(RuntimeException.class));
                assertThat(info.error.getMessage(), is("Test error"));
            }
            else
                assertThat(info.error, nullValue());
        }
    }
    
    private static class Task implements Runnable
    {
        Integer id;
        long executionTime;
        long threadId;
        
        public Task(Integer id)
        {
            this.id = id;
        }
        
        @Override
        public void run()
        {
            executionTime = Times.getCurrentTime();
            threadId = Thread.currentThread().getId();
            
            if ((id % 10) == 0)
                throw new RuntimeException("Test error");
        }
    }
    
    private static class TaskInfo
    {
        long startTime;
        long completeTime;
        Task task;
        Throwable error;
        long threadId;
    }
    
    private static class TaskListenerMock implements ITaskListener<Runnable>
    {
        final Map<Integer, TaskInfo> tasks = new HashMap<Integer, TaskInfo>();
        
        @Override
        public void onTaskStarted(Runnable runnable)
        {
            synchronized (tasks)
            {
                Task task =  (Task)runnable;
                assertThat(tasks.containsKey(task.id), is(false));
                
                TaskInfo info = new TaskInfo();
                info.task = task;
                info.startTime = Times.getCurrentTime();
                info.threadId = Thread.currentThread().getId();
                
                tasks.put(task.id, info);
            }
        }
        
        @Override
        public void onTaskCompleted(Runnable runnable)
        {
            synchronized (tasks)
            {
                Task task =  (Task)runnable;
                TaskInfo info = tasks.get(task.id);
                
                info.completeTime = Times.getCurrentTime();
                
                assertThat(info.threadId, is(Thread.currentThread().getId()));
            }
        }

        @Override
        public void onTaskFailed(Runnable runnable, Throwable error)
        {
            synchronized (tasks)
            {
                Task task =  (Task)runnable;
                TaskInfo info = tasks.get(task.id);
                
                info.completeTime = Times.getCurrentTime();
                info.error = error;
                
                assertThat(info.threadId, is(Thread.currentThread().getId()));
            }
        }
    }
}
