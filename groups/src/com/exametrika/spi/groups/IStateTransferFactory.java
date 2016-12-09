/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups;




/**
 * The {@link IStateTransferFactory} is a state transfer factory.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IStateTransferFactory
{
    /**
     * Creates server part of state transfer.
     *
     * @return server part of state transfer
     */
    IStateTransferServer createServer();
    
    /**
     * Creates client part of state transfer.
     *
     * @return client part of state transfer
     */
    IStateTransferClient createClient();
}
