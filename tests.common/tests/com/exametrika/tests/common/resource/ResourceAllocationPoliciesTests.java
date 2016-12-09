/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.resource;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import sun.misc.VM;

import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.resource.config.DynamicFixedAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.DynamicPercentageAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.DynamicUniformAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.FixedAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.FloatingAllocationPolicyConfiguration;
import com.exametrika.common.resource.config.LimitingAllocationPolicyConfiguration;
import com.exametrika.common.resource.config.MemoryResourceProviderConfiguration;
import com.exametrika.common.resource.config.PercentageAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.PercentageResourceProviderConfiguration;
import com.exametrika.common.resource.config.ThresholdAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.UniformAllocationPolicyConfiguration;
import com.exametrika.common.resource.impl.DynamicFixedAllocationPolicy;
import com.exametrika.common.resource.impl.DynamicPercentageAllocationPolicy;
import com.exametrika.common.resource.impl.DynamicUniformAllocationPolicy;
import com.exametrika.common.resource.impl.FixedAllocationPolicy;
import com.exametrika.common.resource.impl.FixedResourceProvider;
import com.exametrika.common.resource.impl.FloatingAllocationPolicy;
import com.exametrika.common.resource.impl.LimitingAllocationPolicy;
import com.exametrika.common.resource.impl.MemoryResourceProvider;
import com.exametrika.common.resource.impl.PercentageAllocationPolicy;
import com.exametrika.common.resource.impl.PercentageResourceProvider;
import com.exametrika.common.resource.impl.ThresholdAllocationPolicy;
import com.exametrika.common.resource.impl.UniformAllocationPolicy;
import com.exametrika.common.utils.Times;


/**
 * The {@link ResourceAllocationPoliciesTests} are tests for resource allocation policies.
 * 
 * @author Medvedev-A
 */
public class ResourceAllocationPoliciesTests
{
    @Test
    public void testFixedResourceProvider()
    {
        FixedResourceProviderConfiguration configuration = new FixedResourceProviderConfiguration(1000);
        FixedResourceProvider provider = configuration.createProvider();
        
        assertThat(provider.getAmount(), is(1000l));
    }
    
    @Test
    public void testPercentageResourceProvider()
    {
        PercentageResourceProviderConfiguration configuration = new PercentageResourceProviderConfiguration(new FixedResourceProviderConfiguration(1000), 50);
        PercentageResourceProvider provider = configuration.createProvider();
        
        assertThat(provider.getAmount(), is(500l));
    }
    
    @Test
    public void testMemoryResourceProvider()
    {
        MemoryResourceProviderConfiguration configuration = new MemoryResourceProviderConfiguration(true);
        MemoryResourceProvider provider = configuration.createProvider();
        
        assertThat(provider.getAmount() > 0, is(true));
        
        configuration = new MemoryResourceProviderConfiguration(false);
        provider = configuration.createProvider();
        
        assertThat(provider.getAmount() > 0, is(true));
    }
    
    @Test
    public void testUniformAllocationPolicy()
    {
        TestResourceConsumer consumer1 = new TestResourceConsumer(100);
        TestResourceConsumer consumer2 = new TestResourceConsumer(200);
        TestResourceConsumer consumer3 = new TestResourceConsumer(300);
        TestResourceConsumer consumer4 = new TestResourceConsumer(400);
        Map<String, IResourceConsumer> map = new HashMap<String, IResourceConsumer>();
        map.put("segment1", consumer1);
        map.put("segment2", consumer2);
        map.put("segment3", consumer3);
        map.put("segment4", consumer4);
        
        UniformAllocationPolicy policy = new UniformAllocationPolicyConfiguration().createPolicy();
        policy.allocate(1000, map);
        
        assertTrue(consumer1.getQuota() == 250);
        assertTrue(consumer2.getQuota() == 250);
        assertTrue(consumer3.getQuota() == 250);
        assertTrue(consumer4.getQuota() == 250);
    }
    
    @Test
    public void testFixedAllocationPolicy()
    {
        FixedAllocationPolicyConfigurationBuilder builder = new FixedAllocationPolicyConfigurationBuilder();
        builder.setOtherPolicy(new UniformAllocationPolicyConfiguration());
        builder.addQuota("segment1", 100);
        builder.addQuota("segment2", 200);
        
        FixedAllocationPolicy policy = builder.toConfiguration().createPolicy();
        
        TestResourceConsumer consumer1 = new TestResourceConsumer(100);
        TestResourceConsumer consumer2 = new TestResourceConsumer(200);
        TestResourceConsumer consumer3 = new TestResourceConsumer(300);
        TestResourceConsumer consumer4 = new TestResourceConsumer(400);
        Map<String, IResourceConsumer> map = new HashMap<String, IResourceConsumer>();
        map.put("segment1", consumer1);
        map.put("segment2", consumer2);
        map.put("segment3", consumer3);
        map.put("segment4", consumer4);
        
        policy.allocate(50, map);
        
        assertTrue(consumer1.getQuota() == 50);
        assertTrue(consumer2.getQuota() == 0);
        assertTrue(consumer3.getQuota() == 0);
        assertTrue(consumer4.getQuota() == 0);
        
        policy.allocate(150, map);
        
        assertTrue(consumer1.getQuota() == 100);
        assertTrue(consumer2.getQuota() == 50);
        assertTrue(consumer3.getQuota() == 0);
        assertTrue(consumer4.getQuota() == 0);
        
        policy.allocate(350, map);
        
        assertTrue(consumer1.getQuota() == 100);
        assertTrue(consumer2.getQuota() == 200);
        assertTrue(consumer3.getQuota() == 25);
        assertTrue(consumer4.getQuota() == 25);
    }
    
    @Test
    public void testPercentAllocationPolicy()
    {
        PercentageAllocationPolicyConfigurationBuilder builder = new PercentageAllocationPolicyConfigurationBuilder();
        builder.setOtherPolicy(new UniformAllocationPolicyConfiguration());
        builder.addQuota("segment1", 10);
        builder.addQuota("segment2", 20);
        
        PercentageAllocationPolicy policy = builder.toConfiguration().createPolicy();
        
        TestResourceConsumer consumer1 = new TestResourceConsumer(100);
        TestResourceConsumer consumer2 = new TestResourceConsumer(200);
        TestResourceConsumer consumer3 = new TestResourceConsumer(300);
        TestResourceConsumer consumer4 = new TestResourceConsumer(400);
        Map<String, IResourceConsumer> map = new HashMap<String, IResourceConsumer>();
        map.put("segment1", consumer1);
        map.put("segment2", consumer2);
        map.put("segment3", consumer3);
        map.put("segment4", consumer4);
        
        policy.allocate(50, map);
        
        assertTrue(consumer1.getQuota() == 5);
        assertTrue(consumer2.getQuota() == 10);
        assertTrue(consumer3.getQuota() == 17);
        assertTrue(consumer4.getQuota() == 17);
    }
    
    @Test
    public void testFloatingAllocationPolicy()
    {
        FloatingAllocationPolicy policy = new FloatingAllocationPolicyConfiguration("float", 10, 0).createPolicy();
        
        TestResourceConsumer consumer1 = new TestResourceConsumer(100);
        TestResourceConsumer consumer2 = new TestResourceConsumer(200);
        TestResourceConsumer consumer3 = new TestResourceConsumer(300);
        TestResourceConsumer consumer4 = new TestResourceConsumer(400);
        Map<String, IResourceConsumer> map = new LinkedHashMap<String, IResourceConsumer>();
        map.put("segment1", consumer1);
        map.put("segment2", consumer2);
        map.put("segment3", consumer3);
        map.put("float", consumer4);
        
        policy.allocate(50, map);
        
        assertTrue(consumer1.getQuota() == 50);
        assertTrue(consumer2.getQuota() == 0);
        assertTrue(consumer3.getQuota() == 0);
        assertTrue(consumer4.getQuota() == 0);
        
        policy.allocate(350, map);
        
        assertTrue(consumer1.getQuota() == 110);
        assertTrue(consumer2.getQuota() == 220);
        assertTrue(consumer3.getQuota() == 20);
        assertTrue(consumer4.getQuota() == 0);
        
        policy.allocate(1000, map);
        
        assertTrue(consumer1.getQuota() == 110);
        assertTrue(consumer2.getQuota() == 220);
        assertTrue(consumer3.getQuota() == 330);
        assertTrue(consumer4.getQuota() == 340);
    }
    
    @Test
    public void testThresholdAllocationPolicy()
    {
        ThresholdAllocationPolicy policy = new ThresholdAllocationPolicyConfigurationBuilder()
            .addThreshold(2000, new PercentageAllocationPolicyConfigurationBuilder().addQuota("segment1", 10).addQuota("segment2", 20)
                .setOtherPolicy(new UniformAllocationPolicyConfiguration()).toConfiguration())
            .addThreshold(1000, new UniformAllocationPolicyConfiguration())
            .addThreshold(0, new FixedAllocationPolicyConfigurationBuilder().addQuota("segment1", 10).addQuota("segment2", 20)
                .setOtherPolicy(new UniformAllocationPolicyConfiguration()).toConfiguration()).toConfiguration().createPolicy();
        
        TestResourceConsumer consumer1 = new TestResourceConsumer(100);
        TestResourceConsumer consumer2 = new TestResourceConsumer(200);
        TestResourceConsumer consumer3 = new TestResourceConsumer(300);
        TestResourceConsumer consumer4 = new TestResourceConsumer(400);
        Map<String, IResourceConsumer> map = new HashMap<String, IResourceConsumer>();
        map.put("segment1", consumer1);
        map.put("segment2", consumer2);
        map.put("segment3", consumer3);
        map.put("segment4", consumer4);
        
        policy.allocate(50, map);
        assertTrue(consumer1.getQuota() == 10);
        assertTrue(consumer2.getQuota() == 20);
        assertTrue(consumer3.getQuota() == 10);
        assertTrue(consumer4.getQuota() == 10);
        
        policy.allocate(1000, map);
        assertTrue(consumer1.getQuota() == 250);
        assertTrue(consumer2.getQuota() == 250);
        assertTrue(consumer3.getQuota() == 250);
        assertTrue(consumer4.getQuota() == 250);
        
        policy.allocate(2000, map);
        assertTrue(consumer1.getQuota() == 200);
        assertTrue(consumer2.getQuota() == 400);
        assertTrue(consumer3.getQuota() == 700);
        assertTrue(consumer4.getQuota() == 700);
    }
    
    @Test
    public void testDynamicUniformAllocationPolicy()
    {
        TestResourceConsumer consumer1 = new TestResourceConsumer(1);
        TestResourceConsumer consumer2 = new TestResourceConsumer(1);
        TestResourceConsumer consumer3 = new TestResourceConsumer(150);
        TestResourceConsumer consumer4 = new TestResourceConsumer(200);
        TestResourceConsumer consumer5 = new TestResourceConsumer(200);
        Map<String, IResourceConsumer> map = new LinkedHashMap<String, IResourceConsumer>();
        map.put("segment1", consumer1);
        map.put("segment2", consumer2);
        map.put("segment3", consumer3);
        map.put("segment4", consumer4);
        map.put("segment5", consumer5);
        
        DynamicUniformAllocationPolicy policy = new DynamicUniformAllocationPolicyConfigurationBuilder().setMinQuota(0).
            setUnderloadedReservePercentage(10). setOverloadedReservePercentage(10).toConfiguration().createPolicy();
        
        List<IResourceConsumer> consumers = Arrays.<IResourceConsumer>asList(consumer1, consumer2, consumer3, consumer4, consumer5); 
                
        for (int i = 0; i < 400; i++)
        {
            if (i <= 200)
                consumer1.amount = (long)(consumer1.amount * 1.1 + 1) <= 200 ? (long)(consumer1.amount * 1.1 + 1) : 200;
            else
                consumer1.amount = 400 - i;
            consumer2.amount = i <= 200 ? i : (400 - i);
            if (consumer4.quota > 200)
            {
                if (consumer4.quota < 330)
                    consumer4.amount = consumer4.quota;
                else
                    consumer4.amount = 330;
            }
            else
                consumer4.amount = 200;
            
            if (consumer5.quota > 200)
                consumer5.amount = consumer5.quota;
            else
                consumer5.amount = 200;

            policy.allocate(1000, map);
            
            System.out.print(i + ": ");
            
            for (IResourceConsumer consumer : map.values())
            {
                TestResourceConsumer test = (TestResourceConsumer)consumer;
                test.limitAmount = true;
                System.out.print(test.amount + "/" + test.quota + ", ");
            }
            
            System.out.println();
            
            if (i == 7)
                checkConsumers(consumers, new long[]{9,9, 7,7, 150,200, 330,363, 388,421});
            else if (i == 35)
                checkConsumers(consumers, new long[]{200,220, 35,38, 150,200, 271,271, 271,271});
            else if (i == 80)
                checkConsumers(consumers, new long[]{200,200, 80,200, 150,200, 200,200, 200,200});
            else if (i == 321)
                checkConsumers(consumers, new long[]{79,86, 79,86, 150,200, 200,220, 200,220});
            else if (i == 399)
                checkConsumers(consumers, new long[]{1,1, 1,1, 150,200, 330,363, 433,435});
        }
    }
    
    @Test
    public void testDynamicPercentAllocationPolicy()
    {
        TestResourceConsumer consumer1 = new TestResourceConsumer(1);
        TestResourceConsumer consumer2 = new TestResourceConsumer(1);
        TestResourceConsumer consumer3 = new TestResourceConsumer(150);
        TestResourceConsumer consumer4 = new TestResourceConsumer(200);
        TestResourceConsumer consumer5 = new TestResourceConsumer(200);
        Map<String, IResourceConsumer> map = new LinkedHashMap<String, IResourceConsumer>();
        map.put("segment1", consumer1);
        map.put("segment2", consumer2);
        map.put("segment3", consumer3);
        map.put("segment4", consumer4);
        map.put("segment5", consumer5);
        
        DynamicPercentageAllocationPolicy policy = new DynamicPercentageAllocationPolicyConfigurationBuilder()
            .addQuota("segment1", 10)
            .addQuota("segment2", 20)
            .addQuota("segment3", 30)
            .addQuota("segment4", 40)
            .setMinQuota(0)
            .setOtherPolicy(new DynamicUniformAllocationPolicyConfigurationBuilder().setMinQuota(0).
                setUnderloadedReservePercentage(10).setOverloadedReservePercentage(10).toConfiguration())
            .toConfiguration().createPolicy();
        
        List<IResourceConsumer> consumers = Arrays.<IResourceConsumer>asList(consumer1, consumer2, consumer3, consumer4, consumer5); 
                
        for (int i = 0; i < 400; i++)
        {
            if (i <= 100)
                consumer1.amount = (long)(consumer1.amount * 1.1 + 1) <= 100 ? (long)(consumer1.amount * 1.1 + 1) : 100;
            else
                consumer1.amount = ((400 - i) > 100) ? 100 : (400 - i);
            consumer2.amount = i <= 200 ? i : (400 - i);
            if (consumer4.quota > 200)
            {
                if (consumer4.quota < 330)
                    consumer4.amount = consumer4.quota;
                else
                    consumer4.amount = 330;
            }
            else
                consumer4.amount = 200;
            
            if (consumer5.quota > 200)
                consumer5.amount = consumer5.quota;
            else
                consumer5.amount = 200;

            policy.allocate(1000, map);
            
            System.out.print(i + ": ");
            
            for (IResourceConsumer consumer : map.values())
            {
                TestResourceConsumer test = (TestResourceConsumer)consumer;
                test.limitAmount = true;
                System.out.print(test.amount + "/" + test.quota + ", ");
            }
            
            System.out.println();
            
            if (i == 0)
                checkConsumers(consumers, new long[]{2,4, 0,0, 150,300, 200,400, 200,296});
            else if (i == 19)
                checkConsumers(consumers, new long[]{40,100, 19,38, 150,300, 330,400, 162,162});
            else if (i == 28)
                checkConsumers(consumers, new long[]{100,100, 28,56, 150,300, 330,400, 144,144});
            else if (i == 80)
                checkConsumers(consumers, new long[]{100,100, 80,200, 150,300, 330,400, 0,0});
            else if (i == 321)
                checkConsumers(consumers, new long[]{79,100, 79,158, 150,300, 330,400, 42,42});
            else if (i == 361)
                checkConsumers(consumers, new long[]{39,78, 39,78, 150,300, 330,400, 144,144});
            else if (i == 399)
                checkConsumers(consumers, new long[]{1,2, 1,2, 150,300, 330,400, 292,296});
        }
    }

    @Test
    public void testDynamicFixedAllocationPolicy()
    {
        TestResourceConsumer consumer1 = new TestResourceConsumer(1);
        TestResourceConsumer consumer2 = new TestResourceConsumer(1);
        TestResourceConsumer consumer3 = new TestResourceConsumer(150);
        TestResourceConsumer consumer4 = new TestResourceConsumer(200);
        TestResourceConsumer consumer5 = new TestResourceConsumer(200);
        Map<String, IResourceConsumer> map = new LinkedHashMap<String, IResourceConsumer>();
        map.put("segment1", consumer1);
        map.put("segment2", consumer2);
        map.put("segment3", consumer3);
        map.put("segment4", consumer4);
        map.put("segment5", consumer5);
        
        DynamicFixedAllocationPolicy policy = new DynamicFixedAllocationPolicyConfigurationBuilder()
            .addQuota("segment1", 100)
            .addQuota("segment2", 200)
            .addQuota("segment3", 300)
            .addQuota("segment4", 400)
            .setMinQuota(0)
            .setOtherPolicy(new DynamicUniformAllocationPolicyConfigurationBuilder().setMinQuota(0).toConfiguration())
            .toConfiguration().createPolicy();
        
        List<IResourceConsumer> consumers = Arrays.<IResourceConsumer>asList(consumer1, consumer2, consumer3, consumer4, consumer5); 
                
        for (int i = 0; i < 400; i++)
        {
            if (i <= 100)
                consumer1.amount = (long)(consumer1.amount * 1.1 + 1) <= 100 ? (long)(consumer1.amount * 1.1 + 1) : 100;
            else
                consumer1.amount = ((400 - i) > 100) ? 100 : (400 - i);
            consumer2.amount = i <= 200 ? i : (400 - i);
            if (consumer4.quota > 200)
            {
                if (consumer4.quota < 330)
                    consumer4.amount = consumer4.quota;
                else
                    consumer4.amount = 330;
            }
            else
                consumer4.amount = 200;
            
            if (consumer5.quota > 200)
                consumer5.amount = consumer5.quota;
            else
                consumer5.amount = 200;

            policy.allocate(1000, map);
            
            System.out.print(i + ": ");
            
            for (IResourceConsumer consumer : map.values())
            {
                TestResourceConsumer test = (TestResourceConsumer)consumer;
                test.limitAmount = true;
                System.out.print(test.amount + "/" + test.quota + ", ");
            }
            
            System.out.println();
            
            if (i == 0)
                checkConsumers(consumers, new long[]{2,4, 0,0, 150,300, 200,400, 200,296});
            else if (i == 19)
                checkConsumers(consumers, new long[]{40,100, 19,38, 150,300, 330,400, 162,162});
            else if (i == 28)
                checkConsumers(consumers, new long[]{100,100, 28,56, 150,300, 330,400, 144,144});
            else if (i == 80)
                checkConsumers(consumers, new long[]{100,100, 80,200, 150,300, 330,400, 0,0});
            else if (i == 321)
                checkConsumers(consumers, new long[]{79,100, 79,158, 150,300, 330,400, 42,42});
            else if (i == 361)
                checkConsumers(consumers, new long[]{39,78, 39,78, 150,300, 330,400, 144,144});
            else if (i == 399)
                checkConsumers(consumers, new long[]{1,2, 1,2, 150,300, 330,400, 292,296});
        }
    }

    @Test
    public void testLimitingAllocationPolicy()
    {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long maxMemoryLimit = (long)(memoryBean.getHeapMemoryUsage().getMax() * 0.75d);
        long maxNativeMemoryLimit = (long)(VM.maxDirectMemory() * 0.75d);
        
        TestResourceConsumer consumer1 = new TestResourceConsumer(100);
        TestResourceConsumer consumer2 = new TestResourceConsumer(200);
        Map<String, IResourceConsumer> map = new HashMap<String, IResourceConsumer>();
        map.put("heap", consumer1);
        map.put("native", consumer2);
        
        LimitingAllocationPolicy policy = new LimitingAllocationPolicyConfiguration(new UniformAllocationPolicyConfiguration(), 75d).createPolicy();
        policy.allocate(1000, map);
        
        assertTrue(consumer1.getQuota() == 500);
        assertTrue(consumer2.getQuota() == 500);
        
        policy.allocate(2 * (maxMemoryLimit + maxNativeMemoryLimit), map);
        
        assertTrue(consumer1.getQuota() == maxMemoryLimit);
        assertTrue(consumer2.getQuota() == maxNativeMemoryLimit);
    }
    
    private void checkConsumers(List<IResourceConsumer> list, long[] values)
    {
        for (int i = 0; i < list.size(); i++)
        {
            IResourceConsumer consumer = list.get(i);
            assertThat(consumer.getAmount(), is(values[2 * i]));
            assertThat(consumer.getQuota(), is(values[2 * i + 1]));
        }
    }
    
    public static class TestResourceConsumer implements IResourceConsumer
    {
        public boolean limitAmount; 
        public long amount;
        public long quota;
        public long time;
        
        public TestResourceConsumer()
        {
        }
        
        public TestResourceConsumer(long amount)
        {
            this.amount = amount;
        }
        
        @Override
        public long getAmount()
        {
            return amount;
        }

        @Override
        public long getQuota()
        {
            return quota;
        }

        @Override
        public void setQuota(long value)
        {
            quota = value;
            time = Times.getCurrentTime();
            
            if (limitAmount && quota < amount)
                amount = quota;
        }
    }
}