/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

import sun.misc.VM;

import com.exametrika.common.resource.IAllocationPolicy;
import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.utils.Assert;

/**
 * The {@link LimitingAllocationPolicy} is an allocation policy which limits allocated quota by JVM settings (Xmx, MaxDirectMemorySize). 
 * Policy must be assigned to "heap"/"native" level of consumers tree.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class LimitingAllocationPolicy implements IAllocationPolicy
{
    private final IAllocationPolicy basePolicy;
    private final double limitPercentage;
    private final long maxMemorySize;
    private final long maxNativeMemorySize;
    
    public LimitingAllocationPolicy(IAllocationPolicy basePolicy, double limitPercentage)
    {
        Assert.notNull(basePolicy);
        Assert.isTrue(limitPercentage >= 0 && limitPercentage <= 100);
        
        this.basePolicy = basePolicy;
        this.limitPercentage = limitPercentage;
        
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        maxMemorySize = memoryBean.getHeapMemoryUsage().getMax();
        maxNativeMemorySize = VM.maxDirectMemory();
    }
    
    @Override
    public void allocate(long amount, Map<String, IResourceConsumer> consumers)
    {
        consumers = new LinkedHashMap<String, IResourceConsumer>(consumers);
        
        for (Map.Entry<String, IResourceConsumer> entry : consumers.entrySet())
        {
            if (entry.getKey().equals("native"))
                entry.setValue(new ResourceConsumerProxy(entry.getValue(), (long)(maxNativeMemorySize * limitPercentage / 100)));
            else if (entry.getKey().equals("heap"))
                entry.setValue(new ResourceConsumerProxy(entry.getValue(), (long)(maxMemorySize * limitPercentage / 100)));
            else
                Assert.error();
        }
        
        basePolicy.allocate(amount, consumers);
    }
    
    private static class ResourceConsumerProxy implements IResourceConsumer
    {
        private final IResourceConsumer consumer;
        private final long maxQuota;

        public ResourceConsumerProxy(IResourceConsumer consumer, long maxQuota)
        {
            Assert.notNull(consumer);
            
            this.consumer = consumer;
            this.maxQuota = maxQuota;
        }
        
        @Override
        public long getAmount()
        {
            return consumer.getAmount();
        }

        @Override
        public long getQuota()
        {
            return consumer.getQuota();
        }

        @Override
        public void setQuota(long value)
        {
            value = Math.min(value, maxQuota);
            
            consumer.setQuota(value);
        }
    }
}
