/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups.cluster.state;

import java.util.UUID;

/**
 * The {@link IStateTransferFactory} is a state transfer factory.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IStateTransferFactory
{
    /**
     * Creates state store.
     *
     * @param groupId identifier of group where state is transferred
     * @return state store
     */
    IStateStore createStore(UUID groupId);
    
    /**
     * Creates server part of state transfer.
     *
     * @param groupId identifier of group where state is transferred
     * @return server part of state transfer
     */
    IStateTransferServer createServer(UUID groupId);
    
    /**
     * Creates client part of state transfer.
     *
     * @param groupId identifier of group where state is transferred
     * @return client part of state transfer
     */
    IStateTransferClient createClient(UUID groupId);
}
