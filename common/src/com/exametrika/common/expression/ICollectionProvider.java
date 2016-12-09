/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression;



/**
 * The {@link ICollectionProvider} is a provider of collection values and operations.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface ICollectionProvider
{
    /**
     * Returns true if value is collection.
     *
     * @param value value
     * @param get if true get operation is performed, else set operation is performed
     * @return true if value is collection
     */
    boolean isCollection(Object value, boolean get);
    
    /**
     * Returns collection element by index.
     *
     * @param collection collection
     * @param index index
     * @return collection element
     */
    Object get(Object collection, Object index);
    
    /**
     * Sets collection element by index.
     *
     * @param collection collection
     * @param index index
     * @param value collection element
     */
    void set(Object collection, Object index, Object value);
    
    /**
     * Returns iterable over collection.
     *
     * @param collection collection
     * @return iterable
     */
    Iterable getIterable(Object collection);
    
    /**
     * Checks if collection contains specified value.
     *
     * @param collection collection
     * @param value value
     * @return true if collection contains value
     */
    boolean contains(Object collection, Object value);
}
