/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;



/**
 * The {@link IConnectionProvider} represents a provider of connections to remote nodes.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IConnectionProvider
{
    /**
     * Initiates connection with specified node.
     *
     * @param connection node address
     */
    void connect(String connection);
    
    /**
     * Initiates connection with specified node.
     *
     * @param connection node address
     */
    void connect(IAddress connection);
    
    /**
     * Initiates disconnection from specified node.
     *
     * @param connection node address
     */
    void disconnect(String connection);
    
    /**
     * Initiates disconnection from specified node.
     *
     * @param connection node address
     */
    void disconnect(IAddress connection);
    
    /**
     * Canonicalize specified node address in order to be compatible with {@link IAddress#getConnection()}.
     *
     * @param connection node address
     * @return canonicalized node address
     */
    String canonicalize(String connection);
}
