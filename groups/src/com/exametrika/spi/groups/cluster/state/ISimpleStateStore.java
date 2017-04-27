/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups.cluster.state;

import java.util.UUID;

import com.exametrika.common.utils.ByteArray;


/**
 * The {@link ISimpleStateStore} defines external state store interface which is used for backing up group state.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface ISimpleStateStore extends IStateStore
{
    /**
     * Loads state with specified identifier from external store. Load is atomic operation.
     *
     * @param id state identifier
     * @return data, containing requested state or null if data is not available
     */
    ByteArray load(UUID id);
    
    /**
     * Saves specified state in external store. Save is atomic operation.
     *
     * @param id state identifier
     * @param state data, containing saved state
     */
    void save(UUID id, ByteArray state);
}
