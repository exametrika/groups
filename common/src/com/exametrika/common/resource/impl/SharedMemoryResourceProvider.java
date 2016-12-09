/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource.impl;

import java.lang.management.ManagementFactory;

import com.exametrika.common.resource.IResourceProvider;
import com.sun.management.OperatingSystemMXBean;

/**
 * The {@link SharedMemoryResourceProvider} is a resource provider which uses all available physical memory of host.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class SharedMemoryResourceProvider implements IResourceProvider
{
    @Override
    public long getAmount()
    {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        return osBean.getTotalPhysicalMemorySize();
    }
}
