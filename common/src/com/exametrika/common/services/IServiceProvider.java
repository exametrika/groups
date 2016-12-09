/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.services;




/**
 * The {@link IServiceProvider} represents a provider of service.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IServiceProvider
{
    /**
     * Registers services of this service provider.
     *
     * @param registrar service registrar
     */
    void register(IServiceRegistrar registrar);
}
