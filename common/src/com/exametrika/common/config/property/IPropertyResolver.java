/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.property;


/**
 * The {@link IPropertyResolver} is used to resolve property values that can be referenced in configuration as ${property}.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IPropertyResolver
{
    /**
     * Resolves property value.
     *
     * @param propertyName property name
     * @return property value or null if property is not found
     */
    String resolveProperty(String propertyName);
}
