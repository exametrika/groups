/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.compartment;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentDispatcher;
import com.exametrika.common.compartment.ICompartmentFactory.Parameters;
import com.exametrika.common.compartment.ICompartmentGroupFactory;
import com.exametrika.common.compartment.ICompartmentGroupProcessor;
import com.exametrika.common.compartment.ICompartmentProcessor;
import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.compartment.impl.Compartment;
import com.exametrika.common.compartment.impl.CompartmentFactory;
import com.exametrika.common.compartment.impl.CompartmentGroup;
import com.exametrika.common.compartment.impl.CompartmentGroupFactory;
import com.exametrika.common.compartment.impl.SimpleCompartmentQueue;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.tests.Tests;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;


/**
 * The {@link CompartmentTests} are tests for {@link Compartment}.
 * 
 * @see Compartment
 * @author Medvedev-A
 */
public class CompartmentTests
{
    @Test
    public void testStartStop() throws Throwable
    {
        TestCompartmentDispatcher dispatcher = new TestCompartmentDispatcher();
        Parameters parameters = new Parameters();
        parameters.dispatcher = dispatcher;
        Compartment compartment = new CompartmentFactory().createCompartment(parameters);

        compartment.start();
        Thread.sleep(300);
        compartment.stop();
        
        assertThat(dispatcher.compartment == compartment, is(true));
        assertThat(dispatcher.closed, is(true));
        assertThat(dispatcher.beforeClose, is(true));
        assertThat(dispatcher.block, is(true));
        
        dispatcher.close();
        dispatcher = new TestCompartmentDispatcher();
        dispatcher.blocked = true;
        parameters = new Parameters();
        parameters.dispatcher = dispatcher;
        compartment = new CompartmentFactory().createCompartment(parameters);

        compartment.start();
        Thread.sleep(300);
        compartment.stop();
        
        assertThat(dispatcher.compartment == compartment, is(true));
        assertThat(dispatcher.closed, is(true));
        assertThat(dispatcher.beforeClose, is(true));
        assertThat(dispatcher.block, is(true));
        
        dispatcher.close();
        dispatcher = new TestCompartmentDispatcher();
        dispatcher.delay = true;
        parameters = new Parameters();
        parameters.dispatcher = dispatcher;
        compartment = new CompartmentFactory().createCompartment(parameters);

        long start = Times.getCurrentTime();
        compartment.start();
        compartment.stop();
        long end = Times.getCurrentTime();
        
        assertThat(end - start > 1000, is(true));
        
        assertThat(dispatcher.compartment == compartment, is(true));
        assertThat(dispatcher.closed, is(true));
        assertThat(dispatcher.beforeClose, is(true));
        assertThat(dispatcher.block, is(true));
        dispatcher.close();
    }
    
    @Test
    public void testExecution() throws Throwable
    {
        TestCompartmentDispatcher dispatcher = new TestCompartmentDispatcher();
        Parameters parameters = new Parameters();
        parameters.dispatcher = dispatcher;
        parameters.dispatchPeriod = Integer.MAX_VALUE;
        final Compartment compartment = new CompartmentFactory().createCompartment(parameters);
        final Compartment compartment2 = new CompartmentFactory().createCompartment(new Parameters());

        compartment.start();
        compartment2.start();
        
        Thread.sleep(200);
        
        TestCompartmentTask task1 = new TestCompartmentTask(compartment, null, "hello2");
        TestCompartmentTask task2 = new TestCompartmentTask(compartment, new RuntimeException("test"), null);
        TestCompartmentRunnableTask task3 = new TestCompartmentRunnableTask(compartment);
        TestCompartmentTask task01 = new TestCompartmentTask(compartment, null, "hello1");
        TestCompartmentTask task21 = new TestCompartmentTask(compartment, new RuntimeException("test"), null);
        TestCompartmentRunnableTask task31 = new TestCompartmentRunnableTask(compartment);
        
        compartment.offer((Runnable)task1);
        compartment.offer((Runnable)task2);
        compartment.offer(task3);
        compartment.offer(Arrays.asList(task01, task21, task31));
        
        final TestCompartmentTask2 task4 = new TestCompartmentTask2(compartment, compartment, null, "hello1");
        final TestCompartmentTask2 task5 = new TestCompartmentTask2(compartment, compartment, new RuntimeException("test"), null);
        final TestCompartmentRunnableTask2 task6 = new TestCompartmentRunnableTask2(compartment, null);
        final TestCompartmentRunnableTask2 task7 = new TestCompartmentRunnableTask2(compartment, new RuntimeException("test"));
        final TestCompartmentTask2 task41 = new TestCompartmentTask2(compartment, compartment, null, "hello1");
        final TestCompartmentTask2 task51 = new TestCompartmentTask2(compartment, compartment, new RuntimeException("test"), null);
        final TestCompartmentRunnableTask2 task61 = new TestCompartmentRunnableTask2(compartment, null);
        final TestCompartmentRunnableTask2 task71 = new TestCompartmentRunnableTask2(compartment, new RuntimeException("test"));
        
        compartment.offer(new Runnable()
        {
            @Override
            public void run()
            {
                compartment.offer(task4);
                compartment.offer(task5);
                compartment.offer(task6);
                compartment.offer(task7);
                compartment.offer(Arrays.asList(task41, task51));
                compartment.offer(Arrays.asList(task61, task71));
            }
        });
        
        final TestCompartmentTask2 task8 = new TestCompartmentTask2(compartment2, compartment, null, "hello1");
        final TestCompartmentTask2 task9 = new TestCompartmentTask2(compartment2, compartment, new RuntimeException("test"), null);
        final TestCompartmentRunnableTask2 task10 = new TestCompartmentRunnableTask2(compartment, null);
        final TestCompartmentRunnableTask2 task11 = new TestCompartmentRunnableTask2(compartment, new RuntimeException("test"));
        final TestCompartmentRunnableTask2 task12 = new TestCompartmentRunnableTask2(compartment, null);
        final TestCompartmentRunnableTask2 task13 = new TestCompartmentRunnableTask2(compartment, new RuntimeException("test"));
        
        final TestCompartmentTask2 task81 = new TestCompartmentTask2(compartment2, compartment, null, "hello1");
        final TestCompartmentTask2 task91 = new TestCompartmentTask2(compartment2, compartment, new RuntimeException("test"), null);
        final TestCompartmentRunnableTask2 task101 = new TestCompartmentRunnableTask2(compartment, null);
        final TestCompartmentRunnableTask2 task111 = new TestCompartmentRunnableTask2(compartment, new RuntimeException("test"));
        final TestCompartmentRunnableTask2 task121 = new TestCompartmentRunnableTask2(compartment, null);
        final TestCompartmentRunnableTask2 task131 = new TestCompartmentRunnableTask2(compartment, new RuntimeException("test"));
        
        compartment2.offer(new Runnable()
        {
            @Override
            public void run()
            {
                compartment.offer(task8);
                compartment.offer(task9);
                compartment.offer(task10);
                compartment.offer(task11);
                compartment.offer(Arrays.asList(task81, task91));
                compartment.offer(Arrays.asList(task101, task111));
            }
        });
        
        compartment.offer(task12);
        compartment.offer(task13);
        compartment.offer(Arrays.asList(task121, task131));
        
        Thread.sleep(500);
        
        assertThat(task1.completed, is(true));
        assertThat(task2.completed, is(true));
        assertThat(task3.completed, is(true));
        assertThat(task4.completed, is(true));
        assertThat(task5.completed, is(true));
        assertThat(task6.completed, is(true));
        assertThat(task7.completed, is(true));
        assertThat(task8.completed, is(true));
        assertThat(task9.completed, is(true));
        assertThat(task10.completed, is(true));
        assertThat(task11.completed, is(true));
        assertThat(task12.completed, is(true));
        assertThat(task13.completed, is(true));
        
        assertThat(task01.completed, is(true));
        assertThat(task21.completed, is(true));
        assertThat(task31.completed, is(true));
        assertThat(task41.completed, is(true));
        assertThat(task51.completed, is(true));
        assertThat(task61.completed, is(true));
        assertThat(task71.completed, is(true));
        assertThat(task81.completed, is(true));
        assertThat(task91.completed, is(true));
        assertThat(task101.completed, is(true));
        assertThat(task111.completed, is(true));
        assertThat(task121.completed, is(true));
        assertThat(task131.completed, is(true));
        
        compartment.stop();
        
        dispatcher.close();
        dispatcher = new TestCompartmentDispatcher();
        parameters = new Parameters();
        parameters.dispatcher = dispatcher;
        parameters.dispatchPeriod = 100;
        TestCompartmentProcessor processor = new TestCompartmentProcessor();
        parameters.processors.add(processor);
        Compartment compartment3 = new CompartmentFactory().createCompartment(parameters);
        processor.compartment = compartment3;

        compartment3.start();
        
        long t = compartment3.getCurrentTime();
        
        Thread.sleep(500);
 
        assertThat(compartment3.getCurrentTime() - t >= 100, is(true));

        assertThat(processor.count >= 1, is(true));
        
        TestCompartmentGroupProcessor groupProcessor = new TestCompartmentGroupProcessor();
        ICompartmentGroupFactory.Parameters groupParams = new ICompartmentGroupFactory.Parameters();
        groupParams.processors.add(groupProcessor);
        
        CompartmentGroup group = new CompartmentGroupFactory().createCompartmentGroup(groupParams);
        
        group.start();

        groupProcessor.id = ((Thread)Tests.get(((Timer)Tests.get(group, "timer")), "timerThread")).getId();
        
        t = group.getCurrentTime();
        
        Thread.sleep(200);
 
        assertThat(group.getCurrentTime() - t >= 100, is(true));

        assertThat(groupProcessor.count >= 1, is(true));
        
        compartment2.stop();
        compartment3.stop();
        group.stop();
        dispatcher.close();
    }
    
    @Test
    public void testFlowControl() throws Throwable
    {
        TestCompartmentDispatcher dispatcher = new TestCompartmentDispatcher();
        Parameters parameters = new Parameters();
        parameters.name = "compartment1"; 
        parameters.dispatcher = dispatcher;
        parameters.dispatchPeriod = 1;
        TestFlowController flowController = new TestFlowController();
        parameters.flowController = flowController;
        parameters.minLockQueueCapacity = 3;
        parameters.maxUnlockQueueCapacity = 1;
        TestCompartmentBlocker blocker = new TestCompartmentBlocker();
        parameters.processors.add(blocker);
        final Compartment compartment = new CompartmentFactory().createCompartment(parameters);
        SimpleCompartmentQueue queue1 = (SimpleCompartmentQueue)parameters.queue;
        parameters = new Parameters();
        parameters.name = "compartment2";
        SimpleCompartmentQueue queue2 = (SimpleCompartmentQueue)parameters.queue;
        final Compartment compartment2 = new CompartmentFactory().createCompartment(parameters);

        compartment.start();
        compartment2.start();
        
        blocker.block(true);
        
        Thread.sleep(100);
        
        final TestCompartmentTask2 task1 = new TestCompartmentTask2(compartment2, compartment, null, "hello1");
        final TestCompartmentRunnableTask2 task2 = new TestCompartmentRunnableTask2(compartment, null);
        final TestCompartmentRunnableTask2 task3 = new TestCompartmentRunnableTask2(compartment, null);
        final TestCompartmentRunnableTask2 task4 = new TestCompartmentRunnableTask2(compartment, null);
        
        final TestCompartmentTask2 task11 = new TestCompartmentTask2(compartment2, compartment, null, "hello1");
        final TestCompartmentRunnableTask2 task21 = new TestCompartmentRunnableTask2(compartment, null);
        final TestCompartmentRunnableTask2 task31 = new TestCompartmentRunnableTask2(compartment, null);
        final TestCompartmentRunnableTask2 task41 = new TestCompartmentRunnableTask2(compartment, null);
        
        compartment2.offer(new Runnable()
        {
            @Override
            public void run()
            {
                compartment.offer(task1);
                compartment.offer(task2);
                compartment.offer(task3);
                compartment.offer(Arrays.asList(task11));
                compartment.offer(Arrays.asList(task21, task31));
                
                compartment2.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        compartment.offer(task4);
                        compartment.offer(Arrays.asList(task41));
                    }
                });
            }
        });
        
        Thread.sleep(200);
        
        assertThat(task1.completed, is(false));
        assertThat(task2.completed, is(false));
        assertThat(task3.completed, is(false));
        assertThat(task4.completed, is(false));
        assertThat(task11.completed, is(false));
        assertThat(task21.completed, is(false));
        assertThat(task31.completed, is(false));
        assertThat(task41.completed, is(false));
        
        blocker.block(false);
        
        Thread.sleep(200);
        
        assertThat(task1.completed, is(true));
        assertThat(task2.completed, is(true));
        assertThat(task3.completed, is(true));
        assertThat(task4.completed, is(true));
        assertThat(task11.completed, is(true));
        assertThat(task21.completed, is(true));
        assertThat(task31.completed, is(true));
        assertThat(task41.completed, is(true));
        
        assertThat(flowController.lockCount, is(1));
        assertThat(flowController.unlockCount, is(1));
     
        assertThat(queue1.getCapacity(), is(0));
        assertThat(queue2.getCapacity(), is(0));
        
        compartment.stop();
        compartment2.stop();
        dispatcher.close();
    }
    
    private static class TestFlowController implements IFlowController
    {
        private int lockCount;
        private int unlockCount;
        
        @Override
        public void lockFlow(Object flow)
        {
            lockCount++;
        }

        @Override
        public void unlockFlow(Object flow)
        {
            unlockCount++;
        }
    }
    
    private static class TestCompartmentDispatcher implements ICompartmentDispatcher
    {
        private Compartment compartment;
        private boolean block;
        private boolean beforeClose;
        private boolean blocked;
        private boolean closed;
        private final Object sync = new Object();
        private boolean delay;
        private long startTime = Times.getCurrentTime();

        @Override
        public void setCompartment(ICompartment compartment)
        {
            this.compartment = (Compartment)compartment;
        }

        @Override
        public void block(long period)
        {
            block = true;
            Assert.checkState(compartment.isMainThread());
            
            try
            {
                synchronized (sync)
                {
                    while (blocked)
                        sync.wait();
                }
                
                if (period > 0)
                {
                    synchronized (this)
                    {
                        wait(period);
                    }
                }
            }
            catch (InterruptedException e)
            {
                throw new ThreadInterruptedException(e);
            }
        }

        @Override
        public synchronized void wakeup()
        {
            notify();
        }

        @Override
        public boolean canFinish(boolean stopRequested)
        {
            Assert.checkState(compartment.isMainThread());
            
            if (!stopRequested)
                return false;
            
            if (delay && Times.getCurrentTime() - startTime < 1000)
                return false;
            else
                return true;
        }

        @Override
        public void beforeClose()
        {
            beforeClose = true;
            synchronized (sync)
            {
                blocked = false;
                sync.notify();
            }
        }

        @Override
        public void close()
        {
            closed = true;
        }
    }
    
    private static class TestCompartmentProcessor implements ICompartmentProcessor
    {
        private Compartment compartment;
        private volatile int count;
        
        @Override
        public void onTimer(long currentTime)
        {
            Assert.checkState(compartment.isMainThread());
            count++;
        }
    }
    
    private static class TestCompartmentGroupProcessor implements ICompartmentGroupProcessor
    {
        private long id;
        private volatile int count;
        
        @Override
        public void onTimer(long currentTime)
        {
            if (id == 0)
                return;
            
            Assert.checkState(Thread.currentThread().getId() == id);
            count++;
        }
    }
    
    private static class TestCompartmentTask implements ICompartmentTask, Runnable
    {
        private final Compartment compartment;
        private final RuntimeException exception;
        private final String param;
        private volatile boolean completed;
        
        public TestCompartmentTask(Compartment compartment, RuntimeException exception, String param)
        {
            this.compartment = compartment;
            this.exception = exception;
            this.param = param;
        }
        
        @Override
        public void run()
        {
            Assert.checkState(compartment.isMainThread());
            compartment.execute((ICompartmentTask)this);
        }
        
        @Override
        public Object execute()
        {
            Assert.checkState(!compartment.isMainThread());
            if (exception != null)
                throw exception;
            else
                return param;
        }

        @Override
        public void onSucceeded(Object result)
        {
            Assert.checkState(compartment.isMainThread());
            Assert.isTrue(param == result);
            completed = true;
        }

        @Override
        public void onFailed(Throwable error)
        {
            Assert.checkState(compartment.isMainThread());
            Assert.isTrue(error == exception);
            completed = true;
        }
    }
    
    private static class TestCompartmentRunnableTask implements Runnable
    {
        private final Compartment compartment;
        private volatile boolean completed;
        
        public TestCompartmentRunnableTask(Compartment compartment)
        {
            this.compartment = compartment;
        }
        
        @Override
        public void run()
        {
            Assert.checkState(compartment.isMainThread());
            compartment.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    Assert.checkState(!compartment.isMainThread());
                    completed = true;
                }
            });
        }
    }
    
    private static class TestCompartmentTask2 implements ICompartmentTask
    {
        private final Compartment source;
        private final Compartment destination;
        private final RuntimeException exception;
        private final String param;
        private volatile boolean completed;
        
        public TestCompartmentTask2(Compartment source, Compartment destination, RuntimeException exception, String param)
        {
            this.source = source;
            this.destination = destination;
            this.exception = exception;
            this.param = param;
        }
        
        @Override
        public Object execute()
        {
            Assert.checkState(destination.isMainThread());
            if (exception != null)
                throw exception;
            else
                return param;
        }

        @Override
        public void onSucceeded(Object result)
        {
            Assert.checkState(source.isMainThread());
            Assert.isTrue(param == result);
            Assert.isNull(exception);
            completed = true;
        }

        @Override
        public void onFailed(Throwable error)
        {
            Assert.checkState(source.isMainThread());
            Assert.isTrue(error == exception);
            Assert.isNull(param);
            completed = true;
        }
    }
    
    private static class TestCompartmentRunnableTask2 implements Runnable
    {
        private final Compartment compartment;
        private final RuntimeException exception;
        private volatile boolean completed;
        
        public TestCompartmentRunnableTask2(Compartment compartment, RuntimeException exception)
        {
            this.compartment = compartment;
            this.exception = exception;
        }
        
        @Override
        public void run()
        {
            Assert.checkState(compartment.isMainThread());
            completed = true;
            
            if (exception != null)
                throw exception;
        }
    }
    
    private static class TestCompartmentBlocker implements ICompartmentProcessor
    {
        private boolean blocked;
        
        public synchronized void block(boolean value)
        {
            blocked = value;
            notify();
        }
        
        @Override
        public synchronized void onTimer(long currentTime)
        {
            while (blocked)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    break;
                }
            }
        }
    }
}