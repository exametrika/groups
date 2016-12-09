/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.resource;





/**
 * The {@link IResourceProvider} represents a provider of resource.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IResourceProvider
{
    /**
     * Returns amount of resource which provider has.
     *
     * @return amount of resource provider has
     */
    long getAmount();
}
