/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.log;



/**
 * The {@link IMarker} represents a logging marker.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IMarker
{
    /**
     * Returns marker name.
     *
     * @return marker name
     */
    String getName();
    
    @Override
    boolean equals(Object o);
    
    @Override
    int hashCode();
}
