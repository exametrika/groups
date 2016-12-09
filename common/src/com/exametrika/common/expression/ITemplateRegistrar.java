/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;



/**
 * The {@link ITemplateRegistrar} is a helper object used to initialize templates from service registry.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ITemplateRegistrar
{
    /**
     * Registers custom templates in specified registry.
     *
     * @param registry template registry
     */
    void register(ITemplateRegistry registry);
}
