/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;



/**
 * The {@link IConversionProvider} is a provider of type conversions.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IConversionProvider
{
    /**
     * Converts speicifed value to boolean.
     *
     * @param value value
     * @return boolean value
     */
    boolean asBoolean(Object value);
    
    /**
     * Typecasts specified value to given class performing type conversion if needed.
     *
     * @param value value
     * @param clazz class
     * @return typecasted value
     */
    Object cast(Object value, Class clazz);
}
