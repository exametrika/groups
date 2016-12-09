/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;

import java.util.Map;



/**
 * The {@link IRuntimeContextRegistrar} is a helper object used to initialize runtime context from service registry.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IRuntimeContextRegistrar
{
    /**
     * Registers neccessary runtime objects to be used in expressions based on this runtime context.
     *
     * @param context runtime context
     */
    void register(Map<String, ? extends Object> context);
}
