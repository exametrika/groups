/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource;





/**
 * The {@link IResourceConsumer} represents a consumer of resource.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IResourceConsumer
{
    /**
     * Returns amount of resource which consumer actually consumes.
     *
     * @return amount of resource consumer actually consumes
     */
    long getAmount();
    
    /**
     * Returns quota of resource consumer - maximum amount of resource consumer allowed to consume.
     *
     * @return quota of resource consumer - maximum amount of resource consumer allowed to consume
     */
    long getQuota();
    
    /**
     * Sets quota - maximum amount of resource consumer allowed to consume.
     *
     * @param value maximum amount of resource consumer allowed to consume
     */
    void setQuota(long value);
}
