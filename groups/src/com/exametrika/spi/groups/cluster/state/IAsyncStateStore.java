/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.groups.cluster.state;

import java.io.File;
import java.util.UUID;


/**
 * The {@link IAsyncStateStore} defines external state store interface which is used for backing up group state.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author Medvedev-A
 */
public interface IAsyncStateStore extends IStateStore
{
    /**
     * Loads state with specified identifier from external store. Load is atomic operation.
     *
     * @param id state identifier
     * @param state file, containing requested state
     * @return true if state has been loaded, false if requested state is unavailable
     */
    boolean load(UUID id, File state);
    
    /**
     * Saves specified state in external store. Save is atomic operation.
     *
     * @param id state identifier
     * @param state file, containing saved state
     */
    void save(UUID id, File state);
}
