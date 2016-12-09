/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import java.util.UUID;


/**
 * The {@link IRawBatchContext} represents a batch context interface.
 * 
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 * @author AndreyM
 */
public interface IRawBatchContext
{
    /**
     * Returns identifier of serialization extension.
     *
     * @return identifier of serialization extension
     */
    UUID getExtensionId();
    
    /**
     * Returns batch context.
     *
     * @return batch context
     */
    <T> T getContext();
    
    /**
     * Opens batch context.
     */
    void open();
}
