/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.common.compartment;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.exametrika.common.compartment.ICompartmentFactory.Parameters;
import com.exametrika.common.compartment.ICompartmentGroupFactory;
import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.compartment.impl.Compartment;
import com.exametrika.common.compartment.impl.CompartmentFactory;
import com.exametrika.common.compartment.impl.CompartmentGroupFactory;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;


/**
 * The {@link CompartmentPerfTests} are performance tests for {@link Compartment}.
 * 
 * @see Compartment
 * @author Medvedev-A
 */
public class CompartmentPerfTests
{
    private static final int SINGLE_COUNT = 10000000;
    private static final int BATCH_COUNT = 250000;
    private static final int BATCH_SIZE = 40;
    
    @Test
    public void testExecution() throws Throwable
    {
        System.out.println("Single count: " + SINGLE_COUNT);
        ICompartmentGroupFactory.Parameters groupParameters = new ICompartmentGroupFactory.Parameters();
        groupParameters.threadCount = 1;
        Parameters parameters = new Parameters();
        parameters.group = new CompartmentGroupFactory().createCompartmentGroup(groupParameters);
        final Compartment compartment = new CompartmentFactory().createCompartment(parameters);
        final Compartment compartment2 = new CompartmentFactory().createCompartment(new Parameters());

        compartment.start();
        compartment2.start();
        
        Thread.sleep(200);
        
        final TestCompartmentTask task1 = new TestCompartmentTask(compartment2, compartment, "hello1", SINGLE_COUNT);
        
        long l = Times.getCurrentTime();
        
        compartment2.offer(new Runnable()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < SINGLE_COUNT; i++)
                    compartment.offer(task1);
            }
        });
        
        task1.await();
        
        System.out.println(Times.getCurrentTime() - l);
        
        final TestCompartmentRunnableTask task2 = new TestCompartmentRunnableTask(compartment, SINGLE_COUNT);
        l = Times.getCurrentTime();
        
        for (int i = 0; i < SINGLE_COUNT; i++)
            compartment.offer(task2);
        
        task2.await();
        
        System.out.println(Times.getCurrentTime() - l);
        
        System.out.println("Batch count: " + BATCH_COUNT + ", batch size: " + BATCH_SIZE + ", total: " + BATCH_COUNT * BATCH_SIZE);
        
        final TestCompartmentTask task3 = new TestCompartmentTask(compartment2, compartment, "hello1", BATCH_COUNT * BATCH_SIZE);
        final List<TestCompartmentTask> tasks3 = new ArrayList<TestCompartmentTask>(BATCH_SIZE);
        for (int i = 0; i < BATCH_SIZE; i++)
            tasks3.add(task3);
        
        l = Times.getCurrentTime();
        
        compartment2.offer(new Runnable()
        {
            @Override
            public void run()
            {
                int count = BATCH_COUNT;
                for (int i = 0; i < count; i++)
                    compartment.offer(tasks3);
            }
        });
        
        task3.await();
        
        System.out.println(Times.getCurrentTime() - l);
        
        final TestCompartmentRunnableTask task4 = new TestCompartmentRunnableTask(compartment, BATCH_COUNT * BATCH_SIZE);
        final List<TestCompartmentRunnableTask> tasks4 = new ArrayList<TestCompartmentRunnableTask>(BATCH_SIZE);
        for (int i = 0; i < BATCH_SIZE; i++)
            tasks4.add(task4);
        
        l = Times.getCurrentTime();
        
        int count = BATCH_COUNT;
        for (int i = 0; i < count; i++)
            compartment.offer(tasks4);
        
        task4.await();
        System.out.println(Times.getCurrentTime() - l);
    }
    
    private static class TestCompartmentTask implements ICompartmentTask
    {
        private final Compartment source;
        private final Compartment destination;
        private final String param;
        private volatile int count;
        private final int waitCount;
        
        public TestCompartmentTask(Compartment source, Compartment destination, String param, int waitCount)
        {
            this.source = source;
            this.destination = destination;
            this.param = param;
            this.waitCount = waitCount;
        }
        
        public synchronized void await()
        {
            while (count < waitCount)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        @Override
        public Object execute()
        {
            Assert.checkState(destination.isMainThread());
            return param;
        }

        @Override
        public void onSucceeded(Object result)
        {
            Assert.checkState(source.isMainThread());
            Assert.isTrue(param == result);
            count++;
            
            if (count == waitCount)
            {
                synchronized (this)
                {
                    notify();
                }
            }
        }

        @Override
        public void onFailed(Throwable error)
        {
            Assert.error();
        }
    }
    
    private static class TestCompartmentRunnableTask implements Runnable
    {
        private final Compartment compartment;
        private volatile int count;
        private final int waitCount;
        
        public TestCompartmentRunnableTask(Compartment compartment, int waitCount)
        {
            this.compartment = compartment;
            this.waitCount = waitCount;
        }
        
        @Override
        public void run()
        {
            Assert.checkState(compartment.isMainThread());
            count++;
            
            if (count == waitCount)
            {
                synchronized (this)
                {
                    notify();
                }
            }
        }
        
        public synchronized void await()
        {
            while (count < waitCount)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                }
            }
        }
    }
}