/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import com.exametrika.common.expression.IConversionProvider;







/**
 * The {@link StandardConversionProvider} is a standard conversion provider.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class StandardConversionProvider implements IConversionProvider
{
    @Override
    public boolean asBoolean(Object value)
    {
        if (value == null)
            return false;
        else if (value instanceof Boolean)
            return (Boolean)value;
        else if (value instanceof Number)
            return ((Number)value).longValue() != 0;
        else if (value instanceof Character)
            return ((Character)value).charValue() != 0;
        else
            return true;
    }

    @Override
    public Object cast(Object value, Class clazz)
    {
        if (value == null)
            return null;
        else if (clazz.isInstance(value))
            return value;
        else if (clazz == String.class)
            return value.toString();
        else if (clazz == Byte.class || clazz == byte.class)
        {
            if (value instanceof Number)
                return ((Number)value).byteValue();
            else if (value instanceof String)
                return Byte.parseByte((String)value);
            else if (value instanceof Boolean)
                return Boolean.TRUE.equals(value) ? (byte)1 : (byte)0;
        }
        else if (clazz == Short.class || clazz == short.class)
        {
            if (value instanceof Number)
                return ((Number)value).shortValue();
            else if (value instanceof String)
                return Short.parseShort((String)value);
            else if (value instanceof Boolean)
                return Boolean.TRUE.equals(value) ? (short)1 : (short)0;
        }
        else if (clazz == Integer.class || clazz == int.class)
        {
            if (value instanceof Number)
                return ((Number)value).intValue();
            else if (value instanceof String)
                return Integer.parseInt((String)value);
            else if (value instanceof Boolean)
                return Boolean.TRUE.equals(value) ? 1 : 0;
        }
        else if (clazz == Long.class || clazz == long.class)
        {
            if (value instanceof Number)
                return ((Number)value).longValue();
            else if (value instanceof String)
                return Long.parseLong((String)value);
            else if (value instanceof Boolean)
                return Boolean.TRUE.equals(value) ? 1l : 0l;
        }
        else if (clazz == Float.class || clazz == float.class)
        {
            if (value instanceof Number)
                return ((Number)value).floatValue();
            else if (value instanceof String)
                return Float.parseFloat((String)value);
            else if (value instanceof Boolean)
                return Boolean.TRUE.equals(value) ? 1.0f : 0.0f;
        }
        else if (clazz == Double.class || clazz == double.class)
        {
            if (value instanceof Number)
                return ((Number)value).doubleValue();
            else if (value instanceof String)
                return Double.parseDouble((String)value);
            else if (value instanceof Boolean)
                return Boolean.TRUE.equals(value) ? 1.0d : 0.0d;
        }
        else if (clazz == Boolean.class || clazz == boolean.class)
        {
            if (value instanceof Number)
                return ((Number)value).longValue() != 0;
            else if (value instanceof String)
                return Boolean.parseBoolean((String)value);
        }

        if (clazz.isAssignableFrom(value.getClass()))
            return clazz.cast(value);
        else
            return value;
    }
}
