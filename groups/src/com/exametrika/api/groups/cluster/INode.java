/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.groups.cluster;

import java.util.Map;
import java.util.UUID;

import com.exametrika.common.messaging.IAddress;

/**
 * The {@link INode} is a cluster node.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface INode extends Comparable<INode>
{
    /**
     * Returns unique identifier of node.
     * 
     * @return unique identifier of node
     */
    UUID getId();

    /**
     * Returns node name
     * 
     * @return node name
     */
    String getName();

    /**
     * Returns node address.
     *
     * @return node address
     */
    IAddress getAddress();
    
    /**
     * Finds node property with specified name.
     *
     * @param <T> property type
     * @param name property name
     * @return property value or null if property with specified name is not found
     */
    <T> T findProperty(String name);
    
    /**
     * Returns all node properties.
     *
     * @return all node properties
     */
    Map<String, Object> getProperties();
    
    /**
     * Returns domain.
     *
     * @return domain
     */
    String getDomain();
    
    @Override
    public abstract boolean equals(Object o);
    
    @Override
    public abstract int hashCode();
}