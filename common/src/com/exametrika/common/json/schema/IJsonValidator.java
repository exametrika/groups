/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;




/**
 * The {@link IJsonValidator} represents a JSON validator.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IJsonValidator
{
    /**
     * Does validator support instances of specified class?
     *
     * @param clazz class
     * @return true if validator supports instances of specified class
     */
    boolean supports(Class clazz);
    
    /**
     * Validates specified instance.
     *
     * @param type validation type
     * @param instance instance to validate
     * @param context validation context
     */
    void validate(JsonType type, Object instance, IJsonValidationContext context);
}
