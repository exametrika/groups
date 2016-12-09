/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource;

import com.exametrika.common.json.JsonObject;






/**
 * The {@link IResourceAllocator} represents a resource allocator.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IResourceAllocator
{
    /**
     * Registers consumer with specified name for allocation.
     *
     * @param name hierarchical name uniquely identifying consumer within resource allocator. Name consists of number of
     * segments separated by period. Each segment allows to map some specific allocation policy to this level of consumer
     * hierarchy. 
     * @param consumer consumer for allocation
     */
    void register(String name, IResourceConsumer consumer);
    
    /**
     * Unregisters consumer with specified name.
     * 
     * @param name consumer name
     */
    void unregister(String name);
    
    /**
     * Returns statistics.
     *
     * @return statistics
     */
    JsonObject getStatistics();
}
