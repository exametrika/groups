/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups;




/**
 * The {@link ISimpleStateTransferFactory} is a simple state transfer factory.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ISimpleStateTransferFactory
{
    /**
     * Creates server part of state transfer.
     *
     * @return server part of state transfer
     */
    ISimpleStateTransferServer createServer();
    
    /**
     * Creates client part of state transfer.
     *
     * @return client part of state transfer
     */
    ISimpleStateTransferClient createClient();
}
