/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import sun.misc.VM;

import com.exametrika.common.resource.IResourceProvider;
import com.sun.management.OperatingSystemMXBean;

/**
 * The {@link MemoryResourceProvider} is a resource provider which uses all available memory of process as a resource.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class MemoryResourceProvider implements IResourceProvider
{
    private final boolean nativeMemory;

    public MemoryResourceProvider(boolean nativeMemory)
    {
        this.nativeMemory = nativeMemory;
    }
    
    @Override
    public long getAmount()
    {
        long processAmount;
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        if (nativeMemory)
            processAmount = VM.maxDirectMemory();
        else
            processAmount = memoryBean.getHeapMemoryUsage().getMax();
        
        OperatingSystemMXBean osBean = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        return Math.min(osBean.getTotalPhysicalMemorySize(), processAmount);
    }
}
