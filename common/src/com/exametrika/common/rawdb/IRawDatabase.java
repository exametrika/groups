/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;

import java.util.List;

import com.exametrika.common.rawdb.config.RawDatabaseConfiguration;
import com.exametrika.common.utils.ILifecycle;



/**
 * The {@link IRawDatabase} represents a low level database.
 * 
 * @threadsafety Implementations of this interface and its methods are thread safe.
 * @author AndreyM
 */
public interface IRawDatabase extends ILifecycle
{
    /**
     * Returns database configuration.
     *
     * @return database configuration
     */
    RawDatabaseConfiguration getConfiguration();
    
    /**
     * Sets database configuration.
     *
     * @param configuration database configuration
     */
    void setConfiguration(RawDatabaseConfiguration configuration);

    /**
     * Flushes database.
     */
    void flush();
    
    /**
     * Clears all internal page, file and other caches. All file binding information is also cleared. 
     */
    void clearCaches();
    
    /**
     * Performs database operation in transaction asynchronously.
     *
     * @param operation operation to execute
     */
    void transaction(IRawOperation operation);
    
    /**
     * Performs batch of database operations in transaction asynchronously. Each operation is performed in its own transaction.
     *
     * @param operations list of operations to execute
     */
    void transaction(List<IRawOperation> operations);
    
    /**
     * Performs database operation in transaction synchronously. Can not be called from main transaction thread.
     *
     * @param operation operation to execute
     */
    void transactionSync(IRawOperation operation);
    
    /**
     * Performs batch database operation in transaction asynchronously.
     *
     * @param operation operation to execute
     */
    void transaction(IRawBatchOperation operation);
    
    /**
     * Performs batch database operation in transaction synchronously. Can not be called from main transaction thread.
     *
     * @param operation operation to execute
     */
    void transactionSync(IRawBatchOperation operation);
}
