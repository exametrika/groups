/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.resource;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import com.exametrika.common.resource.IResourceProvider;
import com.exametrika.common.resource.config.ChildResourceAllocatorConfigurationBuilder;
import com.exametrika.common.resource.config.FixedAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.PercentageAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.resource.config.SharedResourceAllocatorConfigurationBuilder;
import com.exametrika.common.resource.config.UniformAllocationPolicyConfiguration;
import com.exametrika.common.resource.impl.ChildResourceAllocator;
import com.exametrika.common.resource.impl.RootResourceAllocator;
import com.exametrika.common.resource.impl.SharedResourceAllocator;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.tests.Tests;
import com.exametrika.tests.common.resource.ResourceAllocationPoliciesTests.TestResourceConsumer;
import com.exametrika.tests.common.time.TimeServiceMock;


/**
 * The {@link ResourceAllocatorTests} are tests for resource allocators.
 * 
 * @author Medvedev-A
 */
public class ResourceAllocatorTests
{
    @Test
    public void testResourceAllocator() throws Throwable
    {
        TimeServiceMock timeService = new TimeServiceMock();
        timeService.useSystemTime = false;
        ChildResourceAllocator allocator = new ChildResourceAllocatorConfigurationBuilder()
            .setName("test")
            .setInitializePeriod(0)
            .setQuotaIncreaseDelay(100)
            .setDefaultPolicy(new UniformAllocationPolicyConfiguration())
            .addPolicy("*.*.l3-1", new PercentageAllocationPolicyConfigurationBuilder().
                addQuota("l4-1", 50).addQuota("l4-2", 20).setOtherPolicy(
                    new UniformAllocationPolicyConfiguration()).toConfiguration())
            .addPolicy("*.*.l3-2", new PercentageAllocationPolicyConfigurationBuilder().
                addQuota("l4-1", 50).addQuota("l4-2", 20).setOtherPolicy(
                    new UniformAllocationPolicyConfiguration()).toConfiguration())
            .addPolicy("*.l2", new FixedAllocationPolicyConfigurationBuilder()
                .addQuota("l3-1", 1000).addQuota("l3-2", 2000).setOtherPolicy(
                    new UniformAllocationPolicyConfiguration()).toConfiguration())
            .toConfiguration().createAllocator();
        Tests.set(allocator, "timeService", timeService);
        
        assertThat(allocator.getName(), is("test"));
        
        TestResourceConsumer consumer1 = new TestResourceConsumer(100);
        TestResourceConsumer consumer2 = new TestResourceConsumer(200);
        TestResourceConsumer consumer3 = new TestResourceConsumer(300);
        TestResourceConsumer consumer4 = new TestResourceConsumer(400);
        TestResourceConsumer consumer5 = new TestResourceConsumer(500);
        TestResourceConsumer consumer6 = new TestResourceConsumer(600);
        TestResourceConsumer consumer7 = new TestResourceConsumer(700);
        TestResourceConsumer consumer8 = new TestResourceConsumer(800);
        TestResourceConsumer consumer9 = new TestResourceConsumer(900);
        TestResourceConsumer consumer10 = new TestResourceConsumer(1000);
        TestResourceConsumer consumer11 = new TestResourceConsumer(1100);
        TestResourceConsumer consumer12 = new TestResourceConsumer(1200);
        
        allocator.register("l1.l2.l3-1.l4-1", consumer1);
        allocator.setQuota(10000);
        assertThat(allocator.getQuota(), is(10000l));
        assertThat(allocator.getAmount(), is(100l));
        assertThat(consumer1.getQuota(), is(0l));
        timeService.time = 100;
        allocator.onTimer();
        assertThat(consumer1.getQuota(), is(500l));
        
        allocator.register("l1.l2.l3-1.l4-2", consumer2);
        allocator.setQuota(10000);
        assertThat(allocator.getQuota(), is(10000l));
        assertThat(allocator.getAmount(), is(300l));
        assertThat(consumer1.getQuota(), is(500l));
        timeService.time = 200;
        allocator.onTimer();
        assertThat(consumer2.getQuota(), is(200l));
        
        allocator.register("l1.l2.l3-1.l4-3", consumer5);
        allocator.setQuota(10000);
        timeService.time = 300;
        allocator.onTimer();
        assertThat(allocator.getQuota(), is(10000l));
        assertThat(allocator.getAmount(), is(800l));
        assertThat(consumer1.getQuota(), is(500l));
        assertThat(consumer2.getQuota(), is(200l));
        assertThat(consumer5.getQuota(), is(300l));
        
        allocator.register("l1.l2.l3-1.l4-4", consumer6);
        allocator.setQuota(10000);
        timeService.time = 400;
        allocator.onTimer();
        assertThat(allocator.getQuota(), is(10000l));
        assertThat(allocator.getAmount(), is(1400l));
        assertThat(consumer1.getQuota(), is(500l));
        assertThat(consumer2.getQuota(), is(200l));
        assertThat(consumer5.getQuota(), is(150l));
        assertThat(consumer6.getQuota(), is(150l));
        
        allocator.register("l1.l2.l3-2.l4-1", consumer3);
        allocator.setQuota(10000);
        timeService.time = 500;
        allocator.onTimer();
        assertThat(allocator.getQuota(), is(10000l));
        assertThat(allocator.getAmount(), is(1700l));
        assertThat(consumer1.getQuota(), is(500l));
        assertThat(consumer2.getQuota(), is(200l));
        assertThat(consumer5.getQuota(), is(150l));
        assertThat(consumer6.getQuota(), is(150l));
        assertThat(consumer3.getQuota(), is(1000l));
        
        allocator.register("l1.l2.l3-2.l4-2", consumer4);
        allocator.setQuota(10000);
        timeService.time = 600;
        allocator.onTimer();
        assertThat(allocator.getQuota(), is(10000l));
        assertThat(allocator.getAmount(), is(2100l));
        assertThat(consumer1.getQuota(), is(500l));
        assertThat(consumer2.getQuota(), is(200l));
        assertThat(consumer5.getQuota(), is(150l));
        assertThat(consumer6.getQuota(), is(150l));
        assertThat(consumer3.getQuota(), is(1000l));
        assertThat(consumer4.getQuota(), is(400l));
        
        allocator.register("l1.l2.l3-2.l4-3", consumer7);
        allocator.setQuota(10000);
        timeService.time = 700;
        allocator.onTimer();
        assertThat(allocator.getQuota(), is(10000l));
        assertThat(allocator.getAmount(), is(2800l));
        assertThat(consumer1.getQuota(), is(500l));
        assertThat(consumer2.getQuota(), is(200l));
        assertThat(consumer5.getQuota(), is(150l));
        assertThat(consumer6.getQuota(), is(150l));
        assertThat(consumer3.getQuota(), is(1000l));
        assertThat(consumer4.getQuota(), is(400l));
        assertThat(consumer7.getQuota(), is(600l));
        
        allocator.register("l1.l2.l3-2.l4-4", consumer8);
        allocator.setQuota(10000);
        timeService.time = 800;
        allocator.onTimer();
        assertThat(allocator.getQuota(), is(10000l));
        assertThat(allocator.getAmount(), is(3600l));
        assertThat(consumer1.getQuota(), is(500l));
        assertThat(consumer2.getQuota(), is(200l));
        assertThat(consumer5.getQuota(), is(150l));
        assertThat(consumer6.getQuota(), is(150l));
        assertThat(consumer3.getQuota(), is(1000l));
        assertThat(consumer4.getQuota(), is(400l));
        assertThat(consumer7.getQuota(), is(300l));
        assertThat(consumer8.getQuota(), is(300l));
        
        allocator.register("l1.l2.l3-3", consumer9);
        allocator.register("l1.l2.l3-4", consumer10);
        allocator.setQuota(10000);
        timeService.time = 900;
        allocator.onTimer();
        assertThat(allocator.getQuota(), is(10000l));
        assertThat(allocator.getAmount(), is(5500l));
        assertThat(consumer1.getQuota(), is(500l));
        assertThat(consumer2.getQuota(), is(200l));
        assertThat(consumer5.getQuota(), is(150l));
        assertThat(consumer6.getQuota(), is(150l));
        assertThat(consumer3.getQuota(), is(1000l));
        assertThat(consumer4.getQuota(), is(400l));
        assertThat(consumer7.getQuota(), is(300l));
        assertThat(consumer8.getQuota(), is(300l));
        assertThat(consumer9.getQuota(), is(3500l));
        assertThat(consumer10.getQuota(), is(3500l));
        
        allocator.register("l1.l2-1", consumer11);
        allocator.setQuota(10000);
        timeService.time = 1000;
        allocator.onTimer();
        assertThat(allocator.getQuota(), is(10000l));
        assertThat(allocator.getAmount(), is(6600l));
        assertThat(consumer1.getQuota(), is(500l));
        assertThat(consumer2.getQuota(), is(200l));
        assertThat(consumer5.getQuota(), is(150l));
        assertThat(consumer6.getQuota(), is(150l));
        assertThat(consumer3.getQuota(), is(1000l));
        assertThat(consumer4.getQuota(), is(400l));
        assertThat(consumer7.getQuota(), is(300l));
        assertThat(consumer8.getQuota(), is(300l));
        assertThat(consumer9.getQuota(), is(1000l));
        assertThat(consumer10.getQuota(), is(1000l));
        assertThat(consumer11.getQuota(), is(5000l));
        
        allocator.register("l1-1", consumer12);
        allocator.setQuota(10000);
        
        assertThat(allocator.getQuota(), is(10000l));
        assertThat(allocator.getAmount(), is(7800l));
        assertThat(consumer1.getQuota(), is(500l));
        assertThat(consumer2.getQuota(), is(200l));
        assertThat(consumer5.getQuota(), is(150l));
        assertThat(consumer6.getQuota(), is(150l));
        assertThat(consumer3.getQuota(), is(750l));
        assertThat(consumer4.getQuota(), is(300l));
        assertThat(consumer7.getQuota(), is(225l));
        assertThat(consumer8.getQuota(), is(225l));
        assertThat(consumer9.getQuota(), is(0l));
        assertThat(consumer10.getQuota(), is(0l));
        assertThat(consumer11.getQuota(), is(2500l));
        
        timeService.time = 1100;
        allocator.onTimer();
        assertThat(consumer12.getQuota(), is(5000l));
        
        allocator.unregister("l1-1");
        allocator.unregister("l1.l2-1");
        allocator.unregister("l1.l2.l3-3");
        allocator.unregister("l1.l2.l3-4");
        
        allocator.setQuota(10000);
        timeService.time = 1200;
        allocator.onTimer();
        assertThat(allocator.getQuota(), is(10000l));
        assertThat(allocator.getAmount(), is(3600l));
        assertThat(consumer1.getQuota(), is(500l));
        assertThat(consumer2.getQuota(), is(200l));
        assertThat(consumer5.getQuota(), is(150l));
        assertThat(consumer6.getQuota(), is(150l));
        assertThat(consumer3.getQuota(), is(1000l));
        assertThat(consumer4.getQuota(), is(400l));
        assertThat(consumer7.getQuota(), is(300l));
        assertThat(consumer8.getQuota(), is(300l));
        
        allocator.unregister("l1.l2.l3-1.l4-1");
        allocator.unregister("l1.l2.l3-1.l4-2");
        allocator.unregister("l1.l2.l3-1.l4-3");
        allocator.unregister("l1.l2.l3-1.l4-4");
        allocator.unregister("l1.l2.l3-2.l4-1");
        allocator.unregister("l1.l2.l3-2.l4-2");
        allocator.unregister("l1.l2.l3-2.l4-3");
        allocator.unregister("l1.l2.l3-2.l4-4");
        
        assertTrue(((Map)Tests.get(Tests.get(allocator, "root"), "children")).isEmpty());
    }
    
    @Test
    public void testRootResourceAllocator() throws Throwable
    {
        ChildResourceAllocatorConfigurationBuilder childBuilder = new ChildResourceAllocatorConfigurationBuilder()
            .setName("test")
            .setInitializePeriod(0)
            .setQuotaIncreaseDelay(100)
            .setDefaultPolicy(new UniformAllocationPolicyConfiguration());
        
        ChildResourceAllocator childAllocator1 = childBuilder.setName("l1.l2-1").toConfiguration().createAllocator();
        ChildResourceAllocator childAllocator2 = childBuilder.setName("l1.l2-2").toConfiguration().createAllocator();
        
        RootResourceAllocator rootAllocator = new RootResourceAllocatorConfigurationBuilder()
            .setName("root")
            .setInitializePeriod(0)
            .setTimerPeriod(10)
            .setAllocationPeriod(200)
            .setQuotaIncreaseDelay(20)
            .setDefaultPolicy(new UniformAllocationPolicyConfiguration())
            .toConfiguration().createAllocator();
        rootAllocator.start();
        
        TestResourceProvider provider = new TestResourceProvider();
        Tests.set(rootAllocator, "resourceProvider", provider);

        TestResourceConsumer consumer1 = new TestResourceConsumer(100);
        TestResourceConsumer consumer2 = new TestResourceConsumer(200);
        
        rootAllocator.register(childAllocator1.getName(), childAllocator1);
        childAllocator1.register("l3-1.l4-1", consumer1);
        childAllocator1.register("l3-1.l4-2", consumer2);
        
        provider.setAmount(10000);
        Thread.sleep(500);
        
        assertThat(rootAllocator.getQuota(), is(10000l));
        assertThat(rootAllocator.getAmount(), is(300l));
        assertThat(consumer1.getQuota(), is(5000l));
        assertThat(consumer2.getQuota(), is(5000l));
        
        rootAllocator.register(childAllocator2.getName(), childAllocator2);
        
        Thread.sleep(500);
        assertThat(rootAllocator.getQuota(), is(10000l));
        assertThat(rootAllocator.getAmount(), is(300l));
        assertThat(consumer1.getQuota(), is(2500l));
        assertThat(consumer2.getQuota(), is(2500l));
        
        rootAllocator.unregister(childAllocator2.getName());
        
        Thread.sleep(500);
        assertThat(rootAllocator.getQuota(), is(10000l));
        assertThat(rootAllocator.getAmount(), is(300l));
        assertThat(consumer1.getQuota(), is(5000l));
        assertThat(consumer2.getQuota(), is(5000l));
        
        rootAllocator.stop();
    }
    
    @Test
    public void testSharedResourceAllocator() throws Throwable
    {
        File tmpFile = File.createTempFile("test", ".tmp");
        tmpFile.deleteOnExit();
        
        SharedResourceAllocatorConfigurationBuilder builder = new SharedResourceAllocatorConfigurationBuilder()
            .setName("l1.l2-1")
            .setInitializePeriod(0)
            .setTimerPeriod(10)
            .setAllocationPeriod(500)
            .setQuotaIncreaseDelay(20)
            .setDefaultPolicy(new UniformAllocationPolicyConfiguration())
            .setDataExchangeFileName(tmpFile.getPath())
            .setDataExchangePeriod(100)
            .setStaleAllocatorPeriod(300000)
            .setResourceProvider(new FixedResourceProviderConfiguration(10000))
            .setInitialQuota(1000);
        
        SharedResourceAllocator allocator1 = builder.setName("l1.l2-1").toConfiguration().createAllocator();
        SharedResourceAllocator allocator2 = builder.setName("l1.l2-2").toConfiguration().createAllocator();
        
        allocator1.start();
        
        TestResourceConsumer consumer1 = new TestResourceConsumer(100);
        TestResourceConsumer consumer2 = new TestResourceConsumer(200);
        
        allocator1.register("l3", consumer1);
        allocator2.register("l3", consumer2);

        Thread.sleep(500);
        
        assertThat(allocator1.getQuota(), is(10000l));
        assertThat(allocator1.getAmount(), is(100l));
        assertThat(consumer1.getQuota(), is(10000l));
        
        allocator2.start();
        Thread.sleep(500);
        
        assertThat(allocator1.getQuota(), is(10000l));
        assertThat(allocator1.getAmount(), is(300l));
        assertThat(consumer1.getQuota(), is(5000l));
        
        assertThat(allocator2.getQuota(), is(10000l));
        assertThat(allocator2.getAmount(), is(300l));
        assertThat(consumer2.getQuota(), is(5000l));
        
        allocator2.stop();
        Thread.sleep(1000);
        
        assertThat(allocator1.getQuota(), is(10000l));
        assertThat(allocator1.getAmount(), is(100l));
        assertThat(consumer1.getQuota(), is(10000l));
        
        allocator1.stop();
    }
    
    @Test
    public void testSharedResourceAllocatorStales() throws Throwable
    {
        File tmpFile = File.createTempFile("test", ".tmp");
        tmpFile.deleteOnExit();
        
        SharedResourceAllocatorConfigurationBuilder builder = new SharedResourceAllocatorConfigurationBuilder()
            .setName("l1.l2-1")
            .setInitializePeriod(0)
            .setTimerPeriod(10)
            .setAllocationPeriod(500)
            .setQuotaIncreaseDelay(20)
            .setDefaultPolicy(new UniformAllocationPolicyConfiguration())
            .setDataExchangeFileName(tmpFile.getPath())
            .setDataExchangePeriod(100)
            .setStaleAllocatorPeriod(1000)
            .setResourceProvider(new FixedResourceProviderConfiguration(10000))
            .setInitialQuota(1000);
        
        SharedResourceAllocator allocator1 = builder.setName("l1.l2-1").toConfiguration().createAllocator();
        SharedResourceAllocator allocator2 = builder.setName("l1.l2-2").toConfiguration().createAllocator();
        
        allocator1.start();
        allocator2.start();
        
        TestResourceConsumer consumer1 = new TestResourceConsumer(100);
        TestResourceConsumer consumer2 = new TestResourceConsumer(200);
        
        allocator1.register("l3", consumer1);
        allocator2.register("l3", consumer2);

        Thread.sleep(500);
        
        assertThat(allocator1.getQuota(), is(10000l));
        assertThat(allocator1.getAmount(), is(300l));
        assertThat(consumer1.getQuota(), is(5000l));
        
        ((Timer)Tests.get(allocator2, "timer")).stop();
        
        Thread.sleep(1500);
        
        assertThat(allocator1.getQuota(), is(10000l));
        assertThat(allocator1.getAmount(), is(100l));
        assertThat(consumer1.getQuota(), is(10000l));
    }
    
    @Test
    public void testSharedResourceAllocatorStrategies() throws Throwable
    {
        File tmpFile = File.createTempFile("test", ".tmp");
        tmpFile.deleteOnExit();
        
        SharedResourceAllocatorConfigurationBuilder builder = new SharedResourceAllocatorConfigurationBuilder()
            .setName("l1.l2-1")
            .setInitializePeriod(0)
            .setTimerPeriod(10)
            .setAllocationPeriod(500)
            .setQuotaIncreaseDelay(20)
            .setDefaultPolicy(new UniformAllocationPolicyConfiguration())
            .addPolicy("l1.l2-1", new FixedAllocationPolicyConfigurationBuilder(
                ).addQuota("l3", 200).setOtherPolicy(new UniformAllocationPolicyConfiguration()).toConfiguration())
            .setDataExchangeFileName(tmpFile.getPath())
            .setDataExchangePeriod(100)
            .setStaleAllocatorPeriod(1000)
            .setResourceProvider(new FixedResourceProviderConfiguration(10000))
            .setInitialQuota(1000);
        
        SharedResourceAllocator allocator1 = builder.setName("l1.l2-1").toConfiguration().createAllocator();
        SharedResourceAllocator allocator2 = builder.setName("l1.l2-2").toConfiguration().createAllocator();
        
        allocator1.start();
        allocator2.start();
        
        TestResourceConsumer consumer1 = new TestResourceConsumer(100);
        TestResourceConsumer consumer2 = new TestResourceConsumer(200);
        
        allocator1.register("l3", consumer1);
        allocator2.register("l3", consumer2);

        Thread.sleep(500);
        
        assertThat(allocator1.getQuota(), is(10000l));
        assertThat(allocator1.getAmount(), is(300l));
        assertThat(consumer1.getQuota(), is(200l));
        
        assertThat(allocator2.getQuota(), is(10000l));
        assertThat(allocator2.getAmount(), is(300l));
        assertThat(consumer2.getQuota(), is(5000l));

        allocator1.stop();
        allocator2.stop();
    }
    
    @Test
    public void testSharedResourceAllocatorJustifiedAllocations() throws Throwable
    {
        File tmpFile = File.createTempFile("test", ".tmp");
        tmpFile.deleteOnExit();
        
        SharedResourceAllocatorConfigurationBuilder builder = new SharedResourceAllocatorConfigurationBuilder()
            .setTimerPeriod(10)
            .setInitializePeriod(0)
            .setAllocationPeriod(1000)
            .setQuotaIncreaseDelay(20)
            .setDefaultPolicy(new UniformAllocationPolicyConfiguration())
            .setDataExchangeFileName(tmpFile.getPath())
            .setDataExchangePeriod(100)
            .setStaleAllocatorPeriod(100000)
            .setResourceProvider(new FixedResourceProviderConfiguration(10000))
            .setInitialQuota(1000);
        
        SharedResourceAllocator allocator1 = builder.setName("l1.l2-1").toConfiguration().createAllocator();
        SharedResourceAllocator allocator2 = builder.setName("l1.l2-2").toConfiguration().createAllocator();

        TestResourceConsumer consumer1 = new TestResourceConsumer(100);
        TestResourceConsumer consumer2 = new TestResourceConsumer(200);

        allocator1.register("l3", consumer1);
        allocator2.register("l3", consumer2);

        allocator1.start();
        Thread.sleep(500);
        allocator2.start();
        
        Thread.sleep(2000);
        
        assertTrue(Math.abs(consumer1.time - consumer2.time) < 100);

        allocator1.stop();
        allocator2.stop();
    }
    
    private static class TestResourceProvider implements IResourceProvider
    {
        private long amount;

        @Override
        public synchronized long getAmount()
        {
            return amount;
        }
        
        public synchronized void setAmount(long value)
        {
            amount = value;
        }
    }
}