/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.text.MessageFormat;

/**
 * The {@link Assert} contains various assert functions.
 * 
 * @author Medvedev-A
 */
public final class Assert
{
    /**
     * Checks that specified value is null.
     *
     * @param value value to check
     * @exception IllegalArgumentException if check failed
     */
    public static void isNull(Object value)
    {
        if (value != null)
            throw new IllegalArgumentException("Value must be null.");
    }
    
    /**
     * Checks that specified value is null.
     *
     * @param value value to check
     * @param message exception message if check fails
     * @param args additional arguments
     * @exception IllegalArgumentException if check fails
     */
    public static void isNull(Object value, String message, Object ... args)
    {
        if (value != null)
            throw new IllegalArgumentException(MessageFormat.format(message, args));
    }
    
    /**
     * Checks that specified value is not null.
     *
     * @param value value to check
     * @exception IllegalArgumentException if check fails
     */
    public static void notNull(Object value)
    {
        if (value == null)
            throw new IllegalArgumentException("Value must not be null.");
    }
    
    /**
     * Checks that specified value is not null.
     *
     * @param value value to check
     * @param message exception message if check fails
     * @param args additional arguments
     * @exception IllegalArgumentException if check fails
     */
    public static void notNull(Object value, String message, Object ... args)
    {
        if (value == null)
            throw new IllegalArgumentException(MessageFormat.format(message, args));
    }

    /**
     * Checks that specified value is instance of specified class.
     *
     * @param clazz class to check
     * @param value value to check
     * @exception IllegalArgumentException if check fails
     */
    public static void isInstanceOf(Class clazz, Object value)
    {
        if (!clazz.isInstance(value))
            throw new IllegalArgumentException(MessageFormat.format("Value ''{0}'' is not an instance of class ''{1}''", value, clazz));
    }
    
    /**
     * Checks that specified value is instance of specified class.
     *
     * @param clazz class to check
     * @param value value to check
     * @param message exception message if check fails
     * @param args additional arguments
     * @exception IllegalArgumentException if check fails
     */
    public static void isInstanceOf(Class clazz, Object value, String message, Object ... args)
    {
        if (!clazz.isInstance(value))
            throw new IllegalArgumentException(MessageFormat.format(message, args));
    }

    /**
     * Checks that specified expression is true.
     *
     * @param expression to check
     * @exception IllegalArgumentException if check fails
     */
    public static void isTrue(boolean expression)
    {
        if (!expression)
            throw new IllegalArgumentException("Expression must be true.");
    }

    /**
     * Checks that specified expression is true.
     *
     * @param expression to check
     * @param message exception message if check fails
     * @param args additional arguments
     * @exception IllegalArgumentException if check fails
     */
    public static void isTrue(boolean expression, String message, Object ... args)
    {
        if (!expression)
            throw new IllegalArgumentException(MessageFormat.format(message, args));
    }

    /**
     * Checks that specified state expression is true.
     *
     * @param expression to check
     * @exception IllegalStateException if check fails
     */
    public static void checkState(boolean expression)
    {
        if (!expression)
            throw new IllegalStateException("Expression must be true.");
    }
    
    /**
     * Checks that specified state expression is true.
     *
     * @param expression to check
     * @param message exception message if check fails
     * @param args additional arguments
     * @exception IllegalStateException if check fails
     */
    public static void checkState(boolean expression, String message, Object ... args)
    {
        if (!expression)
            throw new IllegalStateException(MessageFormat.format(message, args));
    }

    /**
     * Checks that specified string value is not null and has length.
     *
     * @param value to check
     * @exception IllegalArgumentException if check fails
     */
    public static void hasLength(String value)
    {
        if (value == null || value.length() == 0)
            throw new IllegalArgumentException("String must have length.");
    }
    
    /**
     * Checks that specified string value is not null and has length.
     *
     * @param value to check
     * @param message exception message if check fails
     * @param args additional arguments
     * @exception IllegalArgumentException if check fails
     */
    public static void hasLength(String value, String message, Object ... args)
    {
        if (value == null || value.length() == 0)
            throw new IllegalArgumentException(MessageFormat.format(message, args));
    }
    
    /**
     * Generates assertion error indicating a bug.
     * 
     * @param <T> return type
     * @return never retuns (used in return statements)
     * @exception AssertionError
     */
    public static <T> T error()
    {
        throw new AssertionError();
    }
    
    /**
     * Generates assertion error indicating a bug.
     * 
     * @param <T> return type
     * @param message exception message
     * @param args additional arguments
     * @return never retuns (used in return statements)
     * @exception AssertionError
     */
    public static <T> T error(String message, Object ... args)
    {
        throw new AssertionError(MessageFormat.format(message, args));
    }
    
    /**
     * Checks that specified expression is true.
     *
     * @param expression to check
     * @exception UnsupportedOperationException if check fails
     */
    public static void supports(boolean expression)
    {
        if (!expression)
            throw new UnsupportedOperationException("Expression must be true.");
    }
    
    /**
     * Checks that specified expression is true.
     *
     * @param expression to check
     * @param message exception message if check fails
     * @param args additional arguments
     * @exception UnsupportedOperationException if check fails
     */
    public static void supports(boolean expression, String message, Object ... args)
    {
        if (!expression)
            throw new UnsupportedOperationException(MessageFormat.format(message, args));
    }
    
    private Assert()
    {
    }
}
