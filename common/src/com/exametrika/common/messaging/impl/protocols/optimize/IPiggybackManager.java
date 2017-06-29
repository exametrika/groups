/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.protocols.optimize;

import com.exametrika.common.messaging.IAddress;

/**
 * The {@link IPiggybackManager} represents a piggyback manager.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IPiggybackManager
{
    /**
     * Registers piggyback sender for given message destination address.
     *
     * @param address message destination address
     * @param sender piggyback sender
     */
    void registerSender(IAddress address, IPiggybackSender sender);
   
    /**
     * Registers piggyback receiver for given message source address.
     *
     * @param address message source address
     * @param receiver piggyback receiver
     */
    void registerReceiver(IAddress address, IPiggybackReceiver receiver);
    
    /**
     * Unregisters piggyback sender for given message destination address.
     *
     * @param address message destination address
     * @param sender piggyback sender
     */
    void unregisterSender(IAddress address, IPiggybackSender sender);
    
    /**
     * Unregisters piggyback receiver for given message source address.
     *
     * @param address message source address
     * @param receiver piggyback receiver
     */
    void unregisterReceiver(IAddress address, IPiggybackReceiver receiver);
}
