/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource;

import java.util.Map;





/**
 * The {@link IAllocationPolicy} represents a resource allocation policy. Allocation policy allows to share resource
 * between several consumers accordingly to some allocation policy.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IAllocationPolicy
{
    /**
     * Performs resource allocation between specified consumers.
     *
     * @param amount available amount of resource
     * @param consumers consumers between which resource is allocated. Consumers are set as segment:consumer pairs where
     * segment is a segment of consumer name corresponding to hierarchy level of allocation policy (i.e. first level -
     * first segment, second level - second segment and so on)
     */
    void allocate(long amount, Map<String, IResourceConsumer> consumers);
}
