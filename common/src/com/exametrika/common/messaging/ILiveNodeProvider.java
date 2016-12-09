/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;

import java.util.List;
import java.util.UUID;





/**
 * The {@link ILiveNodeProvider} is a provider of nodes that are "alive" for the system.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ILiveNodeProvider
{
    /**
     * Returns identifier of current list of live nodes. This identifier must be incremented (starting from 0) each time when list of live nodes 
     * has been changed.
     *
     * @return identifier of current list of live nodes
     */
    long getId();
    
    /**
     * Returns address of local node.
     *
     * @return address of local node
     */
    IAddress getLocalNode();
    
    /**
     * Returns list of live nodes (local node is included).
     *
     * @return list of live nodes (local node is included)
     */
    List<IAddress> getLiveNodes();
    
    /**
     * Is specified node alive?
     *
     * @param node address
     * @return true if node is alive
     */
    boolean isLive(IAddress node);
    
    /**
     * Finds node by identifier.
     *
     * @param id node identifier
     * @return node address or null if node is not alive
     */
    IAddress findById(UUID id);
    
    /**
     * Finds node by name.
     *
     * @param name node name
     * @return node address or null if node is not alive
     */
    IAddress findByName(String name);
    
    /**
     * Finds node by connection.
     *
     * @param connection node connection, connection must be canonicalized
     * @return node address or null if node is not alive
     */
    IAddress findByConnection(String connection);
}
