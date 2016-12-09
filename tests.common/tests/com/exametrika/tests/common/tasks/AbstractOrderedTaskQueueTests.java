/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.tasks;



import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tasks.ITaskFilter;
import com.exametrika.common.tasks.ITaskHandler;
import com.exametrika.common.tasks.impl.AbstractOrderedTaskQueue;
import com.exametrika.common.tasks.impl.TaskExecutor;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.Objects;


/**
 * The {@link AbstractOrderedTaskQueueTests} are tests for {@link AbstractOrderedTaskQueue} class.
 * 
 * @see AbstractOrderedTaskQueue
 * @author Medvedev_A
 */
public class AbstractOrderedTaskQueueTests
{
    @Test
    public void testQueue() throws Throwable
    {
        TestOrderedMessageQueue queue = new TestOrderedMessageQueue(new FlowControllerMock(), null, 5, 10);
        TaskHandler handler = new TaskHandler();
        TaskExecutor<TestTask> taskExecutor = new TaskExecutor<TestTask>(10, queue, handler, null);
        taskExecutor.addTaskListener(queue);
        taskExecutor.start();
        
        for (int i = 0; i < 1000; i++)
        {
            TestTask task = new TestTask(0, i);
            queue.offer(task);
        }
        
        Thread.sleep(3000);
        
        taskExecutor.stop();
        
        int count = 0;
        for (Map.Entry<TestFlow, List<TestTask>> entry : handler.messageMap.entrySet())
        {
            int lastOrder = Integer.MIN_VALUE;
            
            for (TestTask task : entry.getValue())
            {
                assertThat(task.value > lastOrder, is(true));
                lastOrder = task.value;
                count++;
            }
        }
        
        assertThat(count, is(1000));
    }
    
    @Test
    public void testFlowController() throws Throwable
    {
        TestOrderedMessageQueue queue = new TestOrderedMessageQueue(new FlowControllerMock(), null, 5, 10);
        TaskHandler handler = new TaskHandler();
        TaskExecutor<TestTask> taskExecutor = new TaskExecutor<TestTask>(10, queue, handler, null);
        taskExecutor.addTaskListener(queue);
        taskExecutor.start();
        
        TestTask startTask = new TestTask(0, 0);
        queue.offer(startTask);
        
        Thread.sleep(500);
        
        queue.lockFlow(new TestFlow(0));
        
        for (int i = 1; i < 1000; i++)
        {
            TestTask task = new TestTask(0, i);
            queue.offer(task);
        }
        
        Thread.sleep(2000);
        
        assertThat(handler.messageMap.size(), is(1));
        
        queue.unlockFlow(new TestFlow(0));
        
        Thread.sleep(2000);
        
        taskExecutor.stop();
        
        int count = 0;
        for (Map.Entry<TestFlow, List<TestTask>> entry : handler.messageMap.entrySet())
        {
            int lastOrder = Integer.MIN_VALUE;
            
            for (TestTask task : entry.getValue())
            {
                assertThat(task.value > lastOrder, is(true));
                lastOrder = task.value;
                count++;
            }
        }
        
        assertThat(count, is(1000));
    }
    
    @Test
    public void testParentFlowController() throws Throwable
    {
        FlowControllerMock flowController = new FlowControllerMock();
        TestOrderedMessageQueue queue = new TestOrderedMessageQueue(flowController, null, 2, 5);
        TaskHandler handler = new TaskHandler();
        TaskExecutor<TestTask> taskExecutor = new TaskExecutor<TestTask>(10, queue, handler, null);
        taskExecutor.addTaskListener(queue);
        taskExecutor.start();
        
        TestTask startTask = new TestTask(0, 0);
        queue.offer(startTask);
        
        Thread.sleep(500);
        
        queue.lockFlow(new TestFlow(0));
        
        for (int i = 1; i < 1000; i++)
        {
            TestTask task = new TestTask(0, i);
            queue.offer(task);
        }
        
        Thread.sleep(2000);
        
        assertThat(flowController.get(new TestFlow(0)), is(1));
        
        queue.unlockFlow(new TestFlow(0));
        
        Thread.sleep(2000);
        
        taskExecutor.stop();
        
        assertThat(flowController.get(new TestFlow(0)), is(0));
        
        int count = 0;
        for (Map.Entry<TestFlow, List<TestTask>> entry : handler.messageMap.entrySet())
        {
            int lastOrder = Integer.MIN_VALUE;
            
            for (TestTask task : entry.getValue())
            {
                assertThat(task.value > lastOrder, is(true));
                lastOrder = task.value;
                count++;
            }
        }
        
        assertThat(count, is(1000));
    }
    
    @Test
    public void testRemoveQueue() throws Throwable
    {
        FlowControllerMock flowController = new FlowControllerMock();
        TestOrderedMessageQueue queue = new TestOrderedMessageQueue(flowController, null, 2, 5);
        TaskHandler handler = new TaskHandler();
        TaskExecutor<TestTask> taskExecutor = new TaskExecutor<TestTask>(10, queue, handler, null);
        taskExecutor.addTaskListener(queue);
        taskExecutor.start();
        
        TestTask startTask = new TestTask(0, 0);
        queue.offer(startTask);
        
        Thread.sleep(500);
        
        queue.lockFlow(new TestFlow(0));
        
        for (int i = 1; i < 1000; i++)
        {
            TestTask task = new TestTask(0, i);
            queue.offer(task);
        }
        
        Thread.sleep(2000);
        
        assertThat(flowController.get(new TestFlow(0)), is(1));
        
        queue.removeQueue(new TestFlow(0));
        
        queue.unlockFlow(new TestFlow(0));
        
        Thread.sleep(2000);
        
        taskExecutor.stop();
        
        assertThat(flowController.get(new TestFlow(0)), is(0));
        
        assertThat(handler.messageMap.get(new TestFlow(0)).size(), is(1));
    }
    
    public void testRemoveQueues() throws Throwable
    {
        FlowControllerMock flowController = new FlowControllerMock();
        TestOrderedMessageQueue queue = new TestOrderedMessageQueue(flowController, null, 2, 5);
        TaskHandler handler = new TaskHandler();
        TaskExecutor<TestTask> taskExecutor = new TaskExecutor<TestTask>(10, queue, handler, null);
        taskExecutor.addTaskListener(queue);
        taskExecutor.start();
        
        TestTask startTask = new TestTask(0, 0);
        queue.offer(startTask);
        
        Thread.sleep(500);
        
        queue.lockFlow(new TestFlow(0));
        
        for (int i = 1; i < 1000; i++)
        {
            TestTask task = new TestTask(0, i);
            queue.offer(task);
        }
        
        Thread.sleep(2000);
        
        assertThat(flowController.get(new TestFlow(0)), is(1));
        
        queue.removeQueues(new ICondition<TestFlow>()
        {
            @Override
            public boolean evaluate(TestFlow value)
            {
                return value.threadId == 0;
            }
        });
        
        queue.unlockFlow(new TestFlow(0));
        
        Thread.sleep(2000);
        
        taskExecutor.stop();
        
        assertThat(flowController.get(new TestFlow(0)), is(0));
        
        assertThat(handler.messageMap.get(new TestFlow(0)).size(), is(1));
    }
    
    public void testRemoveAllQueues() throws Throwable
    {
        FlowControllerMock flowController = new FlowControllerMock();
        TestOrderedMessageQueue queue = new TestOrderedMessageQueue(flowController, null, 2, 5);
        TaskHandler handler = new TaskHandler();
        TaskExecutor<TestTask> taskExecutor = new TaskExecutor<TestTask>(10, queue, handler, null);
        taskExecutor.addTaskListener(queue);
        taskExecutor.start();
        
        TestTask startTask = new TestTask(0, 0);
        queue.offer(startTask);
        
        Thread.sleep(500);
        
        queue.lockFlow(new TestFlow(0));
        
        for (int i = 1; i < 1000; i++)
        {
            TestTask task = new TestTask(0, i);
            queue.offer(task);
        }
        
        Thread.sleep(2000);
        
        assertThat(flowController.get(new TestFlow(0)), is(1));
        
        queue.removeAllQueues();
        
        queue.unlockFlow(new TestFlow(0));
        
        Thread.sleep(2000);
        
        taskExecutor.stop();
        
        assertThat(flowController.get(new TestFlow(0)), is(0));
        
        assertThat(handler.messageMap.get(new TestFlow(0)).size(), is(1));
    }
    
    public void testFliter() throws Throwable
    {
        FlowControllerMock flowController = new FlowControllerMock();
        TestOrderedMessageQueue queue = new TestOrderedMessageQueue(flowController, new ITaskFilter<TestTask>()
        {
            @Override
            public boolean accept(TestTask task)
            {
                return task.value >= 500;
            }
        }, 2, 5);
        TaskHandler handler = new TaskHandler();
        TaskExecutor<TestTask> taskExecutor = new TaskExecutor<TestTask>(10, queue, handler, null);
        taskExecutor.addTaskListener(queue);
        taskExecutor.start();
        
        for (int i = 0; i < 1000; i++)
        {
            TestTask task = new TestTask(0, i);
            queue.offer(task);
        }
        
        Thread.sleep(2000);
        
        taskExecutor.stop();
        
        int count = 0;
        for (Map.Entry<TestFlow, List<TestTask>> entry : handler.messageMap.entrySet())
        {
            int lastOrder = Integer.MIN_VALUE;
            
            for (TestTask task : entry.getValue())
            {
                assertThat(task.value > lastOrder, is(true));
                assertThat(task.value >= 500, is(true));
                lastOrder = task.value;
                count++;
            }
        }
        
        assertThat(count, is(500));
    }
    
    private static class TaskHandler implements ITaskHandler<TestTask>
    {
        private Map<TestFlow, List<TestTask>> messageMap = new HashMap<TestFlow, List<TestTask>>();
        
        @Override
        public void handle(TestTask task)
        {
            synchronized (this)
            {
                List<TestTask> messages = messageMap.get(task.flow);
                if (messages == null)
                {
                    messages = new ArrayList<TestTask>();
                    messageMap.put(task.flow, messages);
                }
                
                messages.add(task);
                
                try
                {
                    Thread.sleep(1);
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    
    private static class FlowControllerMock implements IFlowController<TestFlow>
    {
        Map<TestFlow, Integer> counters = new HashMap<TestFlow, Integer>();
        
        public synchronized Integer get(TestFlow source)
        {
            return counters.get(source);
        }
        
        @Override
        public synchronized void lockFlow(TestFlow source)
        {
            Integer value = counters.get(source);
            if (value == null)
                value = 0;
            
            value++;
            
            counters.put(source, value);
        }

        @Override
        public synchronized void unlockFlow(TestFlow source)
        {
            Integer value = counters.get(source);
            if (value == null)
                value = 0;
            
            value--;
            
            counters.put(source, value);
        }
    }
    
    private static class TestFlow
    {
        private final long threadId;

        public TestFlow(long threadId)
        {
            this.threadId = threadId;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            
            if (!(o instanceof TestFlow))
                return false;
            
            TestFlow flow = (TestFlow)o;
            
            if (threadId != flow.threadId)
                return false;
            
            return true;
        }
        
        @Override
        public int hashCode()
        {
            return (int)(threadId ^ (threadId >>> 32)); 
        }
    }

    private static class TestTask
    {
        private final TestFlow flow;
        private final int value;

        public TestTask(long threadId, int value)
        {
            this.flow = new TestFlow(threadId);
            this.value = value;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this)
                return true;
            
            if (!(o instanceof TestTask))
                return false;
            
            TestTask task = (TestTask)o;
            
            if (flow != task.flow || value != task.value)
                return false;
            
            return true;
        }
        
        @Override
        public int hashCode()
        {
            return Objects.hashCode(flow, value); 
        }
    }

    private static class TestOrderedMessageQueue extends AbstractOrderedTaskQueue<TestFlow, TestTask>
    {
        public TestOrderedMessageQueue(IFlowController<TestFlow> flowController, ITaskFilter<TestTask> filter, 
            int maxUnlockQueueCapacity, int minLockQueueCapacity)
        {
            super(flowController, filter, maxUnlockQueueCapacity, minLockQueueCapacity);
        }

        @Override
        protected TestFlow getTaskFlow(TestTask task)
        {
            return task.flow;
        }
    }
}
