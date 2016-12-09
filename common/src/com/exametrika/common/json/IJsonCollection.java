/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;

import com.exametrika.common.utils.InvalidArgumentException;




/**
 * The {@link IJsonCollection} represents a JSON collection - object or array.
 * 
 * @param <T> collection element type
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IJsonCollection<T> extends Iterable<T>
{
    /**
     * Is collection empty?
     *
     * @return true if collection is empty
     */
    boolean isEmpty();
    
    /**
     * Returns collection size.
     *
     * @return collection size
     */
    int size();
    
    /**
     * Selects collection element by specified path.
     *
     * @param path path
     * @return selected element or null if optional element is not found
     * @exception InvalidArgumentException if required element is not found
     */
    <V> V select(String path);
    
    /**
     * Selects collection element by specified path.
     *
     * @param path path
     * @param defaultValue default value if optional element is not found
     * @return selected element
     * @exception InvalidArgumentException if required element is not found
     */
    <V> V select(String path, Object defaultValue);
}
