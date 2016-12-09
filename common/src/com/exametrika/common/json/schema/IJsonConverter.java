/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json.schema;




/**
 * The {@link IJsonConverter} represents a JSON converter which converts string value to a value of target type.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IJsonConverter
{
    /**
     * Converts string value to value of specified type.
     *
     * @param type target type
     * @param value string value
     * @param context validation context
     * @return converted value
     */
    Object convert(JsonType type, String value, JsonValidationContext context);
}
